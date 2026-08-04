package com.poly.asm.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    private static final Logger logger = LoggerFactory.getLogger(ErrorController.class);

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
    	String uri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if ("/favicon.ico".equals(uri)) {
            return null; // Bỏ qua lỗi favicon.ico
        }
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        logger.error("Error page accessed. URI: {}, Status: {}, Message: {}, Exception: {}, QueryString: {}",
                uri, status, message, exception != null ? exception.toString() : "None", request.getQueryString());
        model.addAttribute("error", "Đã xảy ra lỗi, vui lòng thử lại");
        return "redirect:/cart/checkout?error=" + encodeErrorMessage("Đã xảy ra lỗi, vui lòng thử lại");
    }

    private String encodeErrorMessage(String message) {
        try {
            return URLEncoder.encode(message, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            logger.error("Failed to encode error message: {}", e.getMessage());
            return "Error_encoding_message";
        }
    }
}