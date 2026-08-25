package com.poly.asm.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.poly.asm.daos.ChatMessageRepository;
import com.poly.asm.daos.UserRepository;
import com.poly.asm.entitys.ChatMessage;
import com.poly.asm.entitys.User;

@Service
public class ChatService {

    public static final String ROLE_CUSTOMER = "USER";
    public static final String ROLE_STAFF = "STAFF";

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    public List<ChatMessage> getConversation(Integer customerId, Long afterId) {
        if (afterId == null || afterId <= 0) {
            return chatMessageRepository.findByUserIdOrderByCreatedAtAsc(customerId);
        }
        return chatMessageRepository.findByUserIdAndIdGreaterThanOrderByCreatedAtAsc(customerId, afterId);
    }

    @Transactional
    public ChatMessage send(User customer, User sender, String senderRole, String content) {
        ChatMessage message = new ChatMessage();
        message.setUser(customer);
        message.setSender(sender);
        message.setSenderRole(senderRole);
        message.setContent(content.trim());
        message.setIsRead(false);
        return chatMessageRepository.save(message);
    }

    @Transactional
    public void markRead(Integer customerId, String senderRole) {
        chatMessageRepository.markRead(customerId, senderRole);
    }

    public long countUnreadForCustomer(Integer customerId) {
        return chatMessageRepository.countUnread(customerId, ROLE_STAFF);
    }

    public List<ChatConversation> getConversations() {
        List<ChatConversation> conversations = new ArrayList<>();
        for (Integer customerId : chatMessageRepository.findConversationUserIds()) {
            User customer = userRepository.findById(customerId).orElse(null);
            if (customer == null) {
                continue;
            }
            List<ChatMessage> messages = chatMessageRepository.findByUserIdOrderByCreatedAtAsc(customerId);
            if (messages.isEmpty()) {
                continue;
            }
            ChatMessage last = messages.get(messages.size() - 1);
            conversations.add(new ChatConversation(customer, last.getContent(), last.getSenderRole(),
                    last.getCreatedAt(), chatMessageRepository.countUnread(customerId, ROLE_CUSTOMER)));
        }
        return conversations;
    }
}
