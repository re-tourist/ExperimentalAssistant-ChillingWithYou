package com.experimentalassistant.backend;

import com.experimentalassistant.backend.dto.AiDraftRequest;
import com.experimentalassistant.backend.dto.RunNoteCreateRequest;
import com.experimentalassistant.backend.dto.RunNoteUpdateRequest;
import com.experimentalassistant.backend.entity.Project;
import com.experimentalassistant.backend.entity.Run;
import com.experimentalassistant.backend.service.ProjectService;
import com.experimentalassistant.backend.service.RunService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class RunNoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RunService runService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testRunNotesFlow() throws Exception {
        // Setup: Create Project and Run
        Project project = new Project();
        project.setName("Test Project");
        projectService.save(project);

        Run run = new Run();
        run.setProjectId(project.getId());
        run.setName("Test Run");
        runService.save(run);
        Long runId = run.getId();

        // 1. Create Note
        RunNoteCreateRequest createRequest = new RunNoteCreateRequest();
        createRequest.setType("NOTE");
        createRequest.setTitle("My First Note");
        createRequest.setContentMd("# Note Content\nThis is a test note.");

        String createResp = mockMvc.perform(post("/api/runs/" + runId + "/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("My First Note"))
                .andReturn().getResponse().getContentAsString();
        
        // Extract noteId (parsing JSON simply)
        Long noteId = objectMapper.readTree(createResp).get("data").get("id").asLong();

        // 2. List Notes
        mockMvc.perform(get("/api/runs/" + runId + "/notes"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(noteId));

        // 3. Create/Update Conclusion
        RunNoteUpdateRequest conclusionReq = new RunNoteUpdateRequest();
        conclusionReq.setTitle("Final Conclusion");
        conclusionReq.setContentMd("## Conclusion\nThe run was successful.");

        mockMvc.perform(put("/api/runs/" + runId + "/conclusion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(conclusionReq)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("CONCLUSION"));

        // 4. Get Conclusion
        mockMvc.perform(get("/api/runs/" + runId + "/conclusion"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conclusion.contentMd").value("## Conclusion\nThe run was successful."));

        // 5. AI Draft
        AiDraftRequest aiReq = new AiDraftRequest();
        aiReq.setRunId(runId);
        aiReq.setSaveDraft(true); // Test saving

        mockMvc.perform(post("/api/ai/run-draft")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aiReq)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.draftMd").exists())
                .andExpect(jsonPath("$.data.extracted.keyFindings").isArray());
        
        // Verify draft saved
        mockMvc.perform(get("/api/runs/" + runId + "/notes?type=AI_DRAFT"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("AI_DRAFT"));
                
        // 6. Markdown Refs
        String mdText = "Check this run [[run:" + runId + "]] and [[run:999]]";
        mockMvc.perform(post("/api/markdown/refs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"textMd\": \"" + mdText + "\"}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value(runId));
    }
}
