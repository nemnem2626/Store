package com.poly.asm.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.poly.asm.daos.UserRepository;
import com.poly.asm.entitys.ChatMessage;
import com.poly.asm.entitys.User;
import com.poly.asm.services.ChatService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Hộp thư hỗ trợ dành cho nhân viên và quản trị viên: xem hội thoại và trả lời khách.
 */
@Controller
@RequestMapping({ "/staff/chat", "/admin/chat" })
public class ChatInboxController {

    private static final int MAX_LENGTH = 1000;

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public String inbox(HttpServletRequest request, Model model) {
        model.addAttribute("conversations", chatService.getConversations());
        model.addAttribute("basePath", basePath(request));
        model.addAttribute("isAdmin", request.getRequestURI().startsWith("/admin"));
        return "chat/inbox";
    }

    @GetMapping("/{customerId}/messages")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> messages(@PathVariable Integer customerId,
            @RequestParam(name = "afterId", required = false) Long afterId) {
        User customer = userRepository.findById(customerId).orElse(null);
        if (customer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Không tìm thấy khách hàng"));
        }
        List<ChatMessage> messages = chatService.getConversation(customerId, afterId);
        chatService.markRead(customerId, ChatService.ROLE_CUSTOMER);
        return ResponseEntity.ok(Map.of(
                "customerName", customer.getFullname() != null ? customer.getFullname() : customer.getUsername(),
                "messages", ChatController.toJson(messages)));
    }

    @PostMapping("/{customerId}/send")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> send(@PathVariable Integer customerId,
            @RequestParam("content") String content, HttpSession session) {
        User customer = userRepository.findById(customerId).orElse(null);
        if (customer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Không tìm thấy khách hàng"));
        }
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nội dung tin nhắn không được để trống"));
        }
        if (content.trim().length() > MAX_LENGTH) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tin nhắn tối đa " + MAX_LENGTH + " ký tự"));
        }
        User staff = (User) session.getAttribute("user");
        ChatMessage saved = chatService.send(customer, staff, ChatService.ROLE_STAFF, content);
        return ResponseEntity.ok(Map.of("message", ChatController.toJson(saved)));
    }

    private String basePath(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/admin") ? "/admin/chat" : "/staff/chat";
    }
}
