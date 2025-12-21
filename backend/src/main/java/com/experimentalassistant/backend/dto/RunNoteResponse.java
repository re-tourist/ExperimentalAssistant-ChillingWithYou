package com.experimentalassistant.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RunNoteResponse {
    private Long id;
    private Long runId;
    private String type;
    private String title;
    private String contentMd;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
