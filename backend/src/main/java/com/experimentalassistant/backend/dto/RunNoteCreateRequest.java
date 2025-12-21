package com.experimentalassistant.backend.dto;

import lombok.Data;

@Data
public class RunNoteCreateRequest {
    private String type; // NOTE | AI_DRAFT (Conclusion handled separately or via type)
    private String title;
    private String contentMd;
}
