package com.experimentalassistant.backend.controller;

import com.experimentalassistant.backend.common.Result;
import com.experimentalassistant.backend.entity.Conversation;
import com.experimentalassistant.backend.entity.Message;
import com.experimentalassistant.backend.service.ChatService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/projects/{projectId}/conversations")
    public Result<List<Conversation>> listConversations(@PathVariable Long projectId) {
        return Result.success(chatService.listConversations(projectId));
    }

    @PostMapping("/projects/{projectId}/conversations")
    public Result<Conversation> createConversation(@PathVariable Long projectId, @RequestBody Map<String, String> body) {
        return Result.success(chatService.createConversation(projectId, body.get("title")));
    }

    @DeleteMapping("/conversations/{id}")
    public Result<Void> deleteConversation(@PathVariable Long id) {
        chatService.deleteConversation(id);
        return Result.success();
    }

    @GetMapping("/conversations/{id}/messages")
    public Result<List<Message>> listMessages(@PathVariable Long id) {
        return Result.success(chatService.listMessages(id));
    }

    @PostMapping("/conversations/{id}/messages")
    public Result<Message> sendMessage(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.success(chatService.sendMessage(id, body.get("content"), body.get("metaJson")));
    }
    
    @PostMapping("/messages/{id}/regenerate")
    public Result<Message> regenerate(@PathVariable Long id) {
        return Result.success(chatService.regenerateMessage(id));
    }
}
