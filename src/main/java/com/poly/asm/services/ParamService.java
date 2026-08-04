package com.poly.asm.services;

import org.springframework.stereotype.Service;


import jakarta.servlet.http.HttpServletRequest;

@Service
public class ParamService {

    // Lấy tham số từ request
    public String getString(HttpServletRequest request, String name) {
        return request.getParameter(name);
    }

    // Lấy tham số kiểu Integer từ request
    public Integer getInt(HttpServletRequest request, String name) {
        try {
            return Integer.parseInt(request.getParameter(name));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Lấy tham số kiểu Double từ request
    public Double getDouble(HttpServletRequest request, String name) {
        try {
            return Double.parseDouble(request.getParameter(name));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
