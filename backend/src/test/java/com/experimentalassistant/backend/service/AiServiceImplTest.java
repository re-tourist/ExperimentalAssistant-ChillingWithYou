package com.experimentalassistant.backend.service;

import com.experimentalassistant.backend.config.AiProperties;
import com.experimentalassistant.backend.dto.ai.AiChatRequest;
import com.experimentalassistant.backend.dto.ai.AiChatResponse;
import com.experimentalassistant.backend.service.impl.AiServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class AiServiceImplTest {

    @Test
    void addsEnableThinkingFalseForDeepSeekWhenNonStream() {
        AiProperties props = buildHttpProps("deepseek-v3.2");
        RunService runService = Mockito.mock(RunService.class);
        AiServiceImpl aiService = new AiServiceImpl(props, runService, new RestTemplateBuilder());

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(aiService, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        byte[] body = "{\"choices\":[{\"message\":{\"content\":\"你好\"}}]}".getBytes(StandardCharsets.UTF_8);
        server.expect(requestTo("http://example.test/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("deepseek-v3.2"))
                .andExpect(jsonPath("$.stream").value(false))
                .andExpect(jsonPath("$.enable_thinking").value(false))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        AiChatRequest req = new AiChatRequest();
        AiChatRequest.Message msg = new AiChatRequest.Message();
        msg.setRole("user");
        msg.setContent("hi");
        req.setMessages(List.of(msg));

        AiChatResponse res = aiService.chat(req);
        assertThat(res.getReply()).isEqualTo("你好");
        assertThat(res.getModel()).isEqualTo("deepseek-v3.2");

        server.verify();
    }

    @Test
    void doesNotAddEnableThinkingWhenStreamTrue() {
        AiProperties props = buildHttpProps("deepseek-v3.2");
        RunService runService = Mockito.mock(RunService.class);
        AiServiceImpl aiService = new AiServiceImpl(props, runService, new RestTemplateBuilder());

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(aiService, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        byte[] body = "{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}".getBytes(StandardCharsets.UTF_8);
        server.expect(requestTo("http://example.test/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.stream").value(true))
                .andExpect(jsonPath("$.enable_thinking").doesNotExist())
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        AiChatRequest req = new AiChatRequest();
        AiChatRequest.Message msg = new AiChatRequest.Message();
        msg.setRole("user");
        msg.setContent("hi");
        req.setMessages(List.of(msg));
        AiChatRequest.Options options = new AiChatRequest.Options();
        options.setStream(true);
        req.setOptions(options);

        AiChatResponse res = aiService.chat(req);
        assertThat(res.getReply()).isEqualTo("ok");

        server.verify();
    }

    private static AiProperties buildHttpProps(String model) {
        AiProperties props = new AiProperties();
        props.setEnable(true);
        props.setProvider("http");
        AiProperties.Http http = new AiProperties.Http();
        http.setBaseUrl("http://example.test/v1");
        http.setApiKey("test-key");
        http.setTimeoutMs(5000);
        http.setModel(model);
        props.setHttp(http);
        return props;
    }
}

