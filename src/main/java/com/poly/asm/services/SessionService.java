package com.poly.asm.services;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    // Lưu thông tin vào session
    public void setAttribute(HttpSession session, String name, Object value) {
        session.setAttribute(name, value);
    }

    // Lấy thông tin từ session
    public Object getAttribute(HttpSession session, String name) {
        return session.getAttribute(name);
    }

    // Xóa thông tin khỏi session
    public void removeAttribute(HttpSession session, String name) {
        session.removeAttribute(name);
    }

    // Xóa tất cả session
    public void invalidateSession(HttpSession session) {
        session.invalidate();
    }
}
