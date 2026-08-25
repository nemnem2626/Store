package com.poly.asm.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.poly.asm.entitys.ChatMessage;
import com.poly.asm.entitys.User;
import com.poly.asm.services.ChatService;

import jakarta.servlet.http.HttpSession;

/**
 * API hộp chat của khách hàng: khách gửi tin và nhận trả lời của nhân viên.
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    private static final int MAX_LENGTH = 1000;

    @Autowired
    private ChatService chatService;

    @GetMapping("/messages")
    public ResponseEntity<Map<String, Object>> messages(@RequestParam(name = "afterId", required = false) Long afterId,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Vui lòng đăng nhập"));
        }
        List<ChatMessage> messages = chatService.getConversation(user.getId(), afterId);
        chatService.markRead(user.getId(), ChatService.ROLE_STAFF);
        return ResponseEntity.ok(Map.of("messages", toJson(messages)));
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> send(@RequestParam("content") String content, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Vui lòng đăng nhập"));
        }
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nội dung tin nhắn không được để trống"));
        }
        if (content.trim().length() > MAX_LENGTH) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tin nhắn tối đa " + MAX_LENGTH + " ký tự"));
        }
        ChatMessage saved = chatService.send(user, user, ChatService.ROLE_CUSTOMER, content);
        return ResponseEntity.ok(Map.of("message", toJson(saved)));
    }

    static List<Map<String, Object>> toJson(List<ChatMessage> messages) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatMessage message : messages) {
            result.add(toJson(message));
        }
        return result;
    }

    static Map<String, Object> toJson(ChatMessage message) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", message.getId());
        item.put("content", message.getContent());
        item.put("senderRole", message.getSenderRole());
        item.put("createdAt", message.getCreatedAt() != null ? message.getCreatedAt().getTime() : null);
        return item;
    }
}
