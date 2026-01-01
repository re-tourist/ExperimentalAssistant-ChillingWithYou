package com.experimentalassistant.backend.dto.ai;

import lombok.Data;
import java.util.List;

@Data
public class AiChatRequest {
    private List<Message> messages;
    private List<Attachment> attachments;
    private Options options;

    @Data
    public static class Message {
        private String role; // system, user, assistant
        private String content;
    }

    @Data
    public static class Attachment {
        private String type; // run, note
        private Long id; // for run
        private Long projectId; // for note
        private String mode; // compact, full (optional)
        // For note, if content is passed from frontend (MVP)
        private String content; 
    }

    @Data
    public static class Options {
        private Double temperature;
        private Integer maxTokens;
        private Boolean stream;
    }
}
