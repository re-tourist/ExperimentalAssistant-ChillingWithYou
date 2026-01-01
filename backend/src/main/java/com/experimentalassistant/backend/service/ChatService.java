package com.experimentalassistant.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.experimentalassistant.backend.entity.Conversation;
import com.experimentalassistant.backend.entity.Message;

import java.util.List;

public interface ChatService extends IService<Conversation> {
    List<Conversation> listConversations(Long projectId);
    Conversation createConversation(Long projectId, String title);
    List<Message> listMessages(Long conversationId);
    Message sendMessage(Long conversationId, String content, String metaJson);
    Message regenerateMessage(Long messageId);
    void deleteConversation(Long id);
}
