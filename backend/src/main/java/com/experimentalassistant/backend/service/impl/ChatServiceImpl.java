package com.experimentalassistant.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.experimentalassistant.backend.dto.ai.AiChatRequest;
import com.experimentalassistant.backend.dto.ai.AiChatResponse;
import com.experimentalassistant.backend.entity.Conversation;
import com.experimentalassistant.backend.entity.Message;
import com.experimentalassistant.backend.mapper.ConversationMapper;
import com.experimentalassistant.backend.mapper.MessageMapper;
import com.experimentalassistant.backend.service.AiService;
import com.experimentalassistant.backend.service.ChatService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatServiceImpl extends ServiceImpl<ConversationMapper, Conversation> implements ChatService {

    private final MessageMapper messageMapper;
    private final AiService aiService;

    public ChatServiceImpl(MessageMapper messageMapper, AiService aiService) {
        this.messageMapper = messageMapper;
        this.aiService = aiService;
    }

    @Override
    public List<Conversation> listConversations(Long projectId) {
        return this.list(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getProjectId, projectId)
                .orderByDesc(Conversation::getUpdatedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Conversation createConversation(Long projectId, String title) {
        Conversation c = new Conversation();
        c.setProjectId(projectId);
        c.setTitle(title != null ? title : "New Chat");
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        this.save(c);
        return c;
    }

    @Override
    public List<Message> listMessages(Long conversationId) {
        return messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .orderByAsc(Message::getCreatedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Message sendMessage(Long conversationId, String content, String metaJson) {
        Conversation c = this.getById(conversationId);
        if (c == null) throw new RuntimeException("Conversation not found");

        // Save User Message
        Message userMsg = new Message();
        userMsg.setConversationId(conversationId);
        userMsg.setRole("user");
        userMsg.setContent(content);
        userMsg.setMetaJson(metaJson);
        userMsg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(userMsg);

        // Update conversation time
        c.setUpdatedAt(LocalDateTime.now());
        this.updateById(c);

        // Call AI
        // For simplicity in this patch, we wait for response. 
        // In Part D, we will improve timeout handling.
        AiChatRequest aiRequest = new AiChatRequest();
        AiChatRequest.Message reqMsg = new AiChatRequest.Message();
        reqMsg.setRole("user");
        reqMsg.setContent(content);
        aiRequest.setMessages(List.of(reqMsg));
        AiChatResponse aiResponse = aiService.chat(aiRequest);
        String aiReply = aiResponse != null ? aiResponse.getReply() : null;

        // Save AI Message
        Message aiMsg = new Message();
        aiMsg.setConversationId(conversationId);
        aiMsg.setRole("ai");
        aiMsg.setContent(aiReply);
        aiMsg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(aiMsg);

        return aiMsg;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Message regenerateMessage(Long messageId) {
        // messageId is the USER message to regenerate response for? 
        // Or the AI message to replace?
        // Prompt says: "regenerate rule: reuse same user input ... generate new AI response, keep old or replace"
        // Let's assume messageId is the AI message we want to regenerate, or the User message we want to re-trigger.
        // Better UX: User clicks "Regenerate" on an AI message. We find the preceding User message.
        
        Message targetMsg = messageMapper.selectById(messageId);
        if (targetMsg == null) throw new RuntimeException("Message not found");
        
        String userContent;
        String metaJson;
        
        if ("ai".equals(targetMsg.getRole())) {
             // Find previous user message
             // Simple logic: find strict previous ID? No, IDs might not be sequential if concurrent.
             // Find latest user message before this one in same conversation?
             // MVP: Assume caller passes the USER message ID to regenerate FROM.
             // Wait, UI usually has "Regenerate" on AI message. 
             // Let's assume we are replacing the AI message content? 
             // Or appending a new AI message? 
             // Let's implement: Pass USER message ID.
             throw new RuntimeException("Please pass the User Message ID to regenerate response for.");
        } else {
            userContent = targetMsg.getContent();
            metaJson = targetMsg.getMetaJson();
        }

        // Call AI again
        AiChatRequest aiRequest = new AiChatRequest();
        AiChatRequest.Message aiMsgReq = new AiChatRequest.Message();
        aiMsgReq.setRole("user");
        aiMsgReq.setContent(userContent);
        aiRequest.setMessages(List.of(aiMsgReq));
        AiChatResponse aiResponse = aiService.chat(aiRequest);
        String aiReply = aiResponse != null ? aiResponse.getReply() : null;

        // Save NEW AI Message (don't overwrite old one, so we have history? Or overwrite?
        // Requirement: "keep old or replace, choose one".
        // Let's append new one for history, UI can show latest.
        Message aiMsg = new Message();
        aiMsg.setConversationId(targetMsg.getConversationId());
        aiMsg.setRole("ai");
        aiMsg.setContent(aiReply);
        aiMsg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(aiMsg);
        
        // Update conversation time
        Conversation c = this.getById(targetMsg.getConversationId());
        c.setUpdatedAt(LocalDateTime.now());
        this.updateById(c);

        return aiMsg;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(Long id) {
        messageMapper.delete(new LambdaQueryWrapper<Message>().eq(Message::getConversationId, id));
        this.removeById(id);
    }
}
