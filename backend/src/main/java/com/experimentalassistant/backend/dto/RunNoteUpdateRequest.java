package com.experimentalassistant.backend.dto;

import lombok.Data;

@Data
public class RunNoteUpdateRequest {
    private String title;
    private String contentMd;
}
