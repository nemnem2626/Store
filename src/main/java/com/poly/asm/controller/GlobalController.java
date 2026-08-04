package com.poly.asm.controller;

import com.poly.asm.entitys.Cart;
import com.poly.asm.entitys.CartItem;
import com.poly.asm.services.CartService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;

@ControllerAdvice
public class GlobalController {

    @Autowired
    private CartService cartService;

    @ModelAttribute
    public void addAttributesToAllModels(HttpServletRequest request, Model model) {
        Cart cart = cartService.getCart(request); // Đúng
        int cartItemCount = cart != null ? cart.getCartItems().size() : 0;
        model.addAttribute("cartItemCount", cartItemCount);
    }
}