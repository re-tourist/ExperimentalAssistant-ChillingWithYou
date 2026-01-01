package com.experimentalassistant.backend.service.impl;

import com.experimentalassistant.backend.config.AiProperties;
import com.experimentalassistant.backend.dto.RunDetailResponse;
import com.experimentalassistant.backend.dto.ai.AiChatRequest;
import com.experimentalassistant.backend.dto.ai.AiChatResponse;
import com.experimentalassistant.backend.service.AiService;
import com.experimentalassistant.backend.service.RunService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private final AiProperties aiProperties;
    private final RunService runService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public AiServiceImpl(AiProperties aiProperties, RunService runService, RestTemplateBuilder restTemplateBuilder) {
        this.aiProperties = aiProperties;
        this.runService = runService;
        long timeoutMs = Math.max(1, aiProperties.getHttp().getTimeoutMs());
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
        this.restTemplate.getMessageConverters().stream()
                .filter(c -> c instanceof StringHttpMessageConverter)
                .map(c -> (StringHttpMessageConverter) c)
                .forEach(c -> c.setDefaultCharset(StandardCharsets.UTF_8));
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        // 1. Check Enabled
        if (!aiProperties.isEnable()) {
            throw new RuntimeException("AI_DISABLED");
        }
        
        // 2. Mock Mode
        if ("mock".equalsIgnoreCase(aiProperties.getProvider())) {
            AiChatResponse res = new AiChatResponse();
            res.setReply("This is a mock response from ExperimentalAssistant AI. \n\nI received your message and attachments. Real AI is currently disabled or in mock mode.");
            res.setProvider("mock");
            res.setModel("mock-model");
            return res;
        }

        if (!StringUtils.hasText(aiProperties.getHttp().getApiKey())) {
            throw new RuntimeException("AI_DISABLED");
        }

        // 3. Process Attachments
        String attachmentsContext = buildAttachmentsContext(request.getAttachments());
        AiChatRequest.Options options = request.getOptions();
        
        // 4. Construct Messages
        List<Map<String, Object>> messages = new ArrayList<>();
        if (request.getMessages() != null) {
            for (AiChatRequest.Message msg : request.getMessages()) {
                Map<String, Object> m = new HashMap<>();
                m.put("role", msg.getRole());
                m.put("content", msg.getContent());
                messages.add(m);
            }
        }

        // Append attachments to the last user message or as a system message
        if (StringUtils.hasText(attachmentsContext)) {
            Map<String, Object> contextMsg = new HashMap<>();
            contextMsg.put("role", "system");
            contextMsg.put("content", "Reference Context:\n" + attachmentsContext);
            int insertIndex = 0;
            while (insertIndex < messages.size() && "system".equalsIgnoreCase(String.valueOf(messages.get(insertIndex).get("role")))) {
                insertIndex++;
            }
            messages.add(insertIndex, contextMsg);
        }

        messages = trimMessages(messages, 12, 12000);

        try {
            return retryCallOpenAiCompatible(messages, options, 0);
        } catch (Exception e) {
            handleAiError(e);
            return null; // Unreachable
        }
    }

    private List<Map<String, Object>> trimMessages(List<Map<String, Object>> messages, int maxMessages, int maxChars) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        int effectiveMaxMessages = Math.max(1, maxMessages);
        int effectiveMaxChars = Math.max(256, maxChars);

        List<Map<String, Object>> system = new ArrayList<>();
        List<Map<String, Object>> rest = new ArrayList<>();
        for (Map<String, Object> m : messages) {
            if (m == null) {
                continue;
            }
            String role = String.valueOf(m.get("role"));
            if ("system".equalsIgnoreCase(role) && system.size() < 2) {
                system.add(m);
            } else {
                rest.add(m);
            }
        }

        int remainingSlots = Math.max(0, effectiveMaxMessages - system.size());
        int start = Math.max(0, rest.size() - remainingSlots);
        List<Map<String, Object>> tail = rest.subList(start, rest.size());

        List<Map<String, Object>> out = new ArrayList<>(system.size() + tail.size());
        out.addAll(system);

        int totalChars = out.stream()
                .map(m -> String.valueOf(m.get("content")))
                .mapToInt(s -> s == null ? 0 : s.length())
                .sum();

        for (int i = tail.size() - 1; i >= 0; i--) {
            Map<String, Object> m = tail.get(i);
            String content = String.valueOf(m.get("content"));
            int len = content == null ? 0 : content.length();
            if (!out.isEmpty() && totalChars + len > effectiveMaxChars) {
                continue;
            }
            out.add(system.size(), m);
            totalChars += len;
        }

        if (out.isEmpty()) {
            return List.of(tail.get(tail.size() - 1));
        }
        return out;
    }

    private String buildAttachmentsContext(List<AiChatRequest.Attachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (AiChatRequest.Attachment att : attachments) {
            if (att == null) continue;
            if ("run".equalsIgnoreCase(att.getType()) && att.getId() != null) {
                RunDetailResponse runDetail = runService.getRunDetail(att.getId());
                if (runDetail != null) {
                    try {
                        parts.add("run#" + att.getId() + "\n" + objectMapper.writeValueAsString(runDetail));
                    } catch (Exception e) {
                        parts.add("run#" + att.getId() + "\n" + String.valueOf(runDetail));
                    }
                }
            } else if ("note".equalsIgnoreCase(att.getType())) {
                if (StringUtils.hasText(att.getContent())) {
                    parts.add("note\n" + att.getContent());
                }
            }
        }
        return String.join("\n\n", parts);
    }

    private AiChatResponse retryCallOpenAiCompatible(List<Map<String, Object>> messages, AiChatRequest.Options options, int retryCount) {
        try {
            return doCallOpenAiCompatible(messages, options);
        } catch (Exception e) {
            // Check if retryable (timeout, 502, 503)
            boolean isRetryable = isRetryableError(e);
            if (isRetryable && retryCount < 1) { // Retry once (total 2 attempts)
                log.warn("AI API call failed, retrying... (attempt {})", retryCount + 1);
                try {
                    Thread.sleep(1000 * (retryCount + 1)); // Linear backoff 1s
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                return retryCallOpenAiCompatible(messages, options, retryCount + 1);
            }
            throw e;
        }
    }

    private boolean isRetryableError(Exception e) {
        if (e instanceof java.net.SocketTimeoutException || e instanceof java.net.ConnectException) {
            return true;
        }
        if (e instanceof HttpStatusCodeException ex) {
            int code = ex.getStatusCode().value();
            return code == 502 || code == 503 || code == 504 || code == 429;
        }
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
            if (root instanceof java.net.SocketTimeoutException || root instanceof java.net.ConnectException) {
                return true;
            }
        }
        return false;
    }

    private AiChatResponse doCallOpenAiCompatible(List<Map<String, Object>> messages, AiChatRequest.Options options) {
        String url = aiProperties.getHttp().getBaseUrl();
        if (url.endsWith("/")) {
            url = url + "chat/completions";
        } else {
            url = url + "/chat/completions";
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", aiProperties.getHttp().getModel());
        payload.put("messages", messages);
        boolean stream = options != null && Boolean.TRUE.equals(options.getStream());
        payload.put("stream", stream);
        
        if (options != null) {
            if (options.getTemperature() != null) payload.put("temperature", options.getTemperature());
            if (options.getMaxTokens() != null) {
                payload.put("max_tokens", options.getMaxTokens());
            }
        }
        if (!payload.containsKey("max_tokens")) {
            payload.put("max_tokens", 1024);
        }

        String model = aiProperties.getHttp().getModel();
        if (!stream && model != null && model.toLowerCase().startsWith("deepseek-")) {
            payload.put("enable_thinking", false);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(aiProperties.getHttp().getApiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<byte[]> response = restTemplate.postForEntity(url, entity, byte[].class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            try {
                String bodyText = new String(response.getBody(), StandardCharsets.UTF_8);
                JsonNode root = objectMapper.readTree(bodyText);
                AiChatResponse res = new AiChatResponse();
                
                if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
                    JsonNode choice = root.get("choices").get(0);
                    if (choice.has("message") && choice.get("message").has("content")) {
                        res.setReply(choice.get("message").get("content").asText());
                    }
                }
                
                if (root.has("usage")) {
                    JsonNode usageNode = root.get("usage");
                    AiChatResponse.Usage usage = new AiChatResponse.Usage();
                    usage.setPromptTokens(usageNode.path("prompt_tokens").asInt(0));
                    usage.setCompletionTokens(usageNode.path("completion_tokens").asInt(0));
                    usage.setTotalTokens(usageNode.path("total_tokens").asInt(0));
                    res.setUsage(usage);
                }
                
                res.setProvider("dashscope"); // or generic
                res.setModel(aiProperties.getHttp().getModel());
                return res;
            } catch (Exception e) {
                 throw new RuntimeException("Failed to parse AI response: " + e.getMessage(), e);
            }
        } else {
            throw new RuntimeException("AI API failed: " + response.getStatusCode());
        }
    }

    private void handleAiError(Exception e) {
        String detail = e.getMessage();
        if (e instanceof HttpStatusCodeException ex) {
            String body = ex.getResponseBodyAsString();
            if (StringUtils.hasText(body)) {
                detail = ex.getStatusCode() + ": " + body;
            } else {
                detail = ex.getStatusCode().toString();
            }
        } else {
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            if (root != e) {
                String rootMsg = root.getMessage();
                if (StringUtils.hasText(rootMsg)) {
                    detail = root.getClass().getSimpleName() + ": " + rootMsg;
                } else {
                    detail = root.getClass().getSimpleName();
                }
            } else if (!StringUtils.hasText(detail)) {
                detail = e.getClass().getSimpleName();
            }
        }
        
        // Convert to business error if timeout
        if (detail.contains("SocketTimeoutException") || detail.contains("ConnectException") || detail.contains("504") || detail.contains("HttpTimeoutException")) {
            log.error("AI Chat Timeout: {}", detail);
            throw new RuntimeException("AI_TIMEOUT: The AI model took too long to respond (timeout). Please try again later or simplify your request.");
        }
        
        log.error("AI Chat Error: {}", detail);
        throw new RuntimeException("AI Service Error: " + detail);
    }

    // Deprecated direct call, kept for interface if needed, but we use the one above
    private AiChatResponse callOpenAiCompatible(List<Map<String, Object>> messages, AiChatRequest.Options options) {
         return retryCallOpenAiCompatible(messages, options, 0);
    }
}
