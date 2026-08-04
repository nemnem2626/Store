package com.poly.asm.controller;

import com.poly.asm.daos.UserRepository;
import com.poly.asm.entitys.Cart;
import com.poly.asm.entitys.User;
import com.poly.asm.services.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserController {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @ModelAttribute
    public void addAttributesToAllModels(HttpServletRequest request, Model model) {
        Cart cart = cartService.getCart(request);
        int cartItemCount = cart != null ? cart.getCartItems().size() : 0;
        model.addAttribute("cartItemCount", cartItemCount);
    }

    @GetMapping("/user/edit")
    public String editProfilePage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        return "web/edit-profile";
    }

    @PostMapping("/user/update")
    public String updateProfile(
            @RequestParam String fullname,
            @RequestParam String phone,
            @RequestParam(required = false) String password,
            HttpSession session,
            Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        // Kiểm tra fullname (chỉ chứa chữ cái Unicode và khoảng trắng, không chứa số hoặc ký tự đặc biệt)
        if (fullname == null || fullname.trim().isEmpty()) {
            model.addAttribute("message", "Họ và tên không được để trống!");
            model.addAttribute("user", user);
            return "web/edit-profile";
        }
        if (!fullname.matches("^[\\p{L}\\s]+$")) {
            model.addAttribute("message", "Họ và tên chỉ được chứa chữ cái và khoảng trắng!");
            model.addAttribute("user", user);
            return "web/edit-profile";
        }

        // Kiểm tra phone (chỉ chứa số)
        if (phone != null && !phone.isEmpty()) {
            if (!phone.matches("^\\d+$")) {
                model.addAttribute("message", "Số điện thoại chỉ được chứa số!");
                model.addAttribute("user", user);
                return "web/edit-profile";
            }
            if (phone.length() > 10) {
                model.addAttribute("message", "Số điện thoại không được vượt quá 10 chữ số!");
                model.addAttribute("user", user);
                return "web/edit-profile";
            }
        }

        // Kiểm tra password (tối thiểu 8 ký tự, chứa chữ và số) nếu có thay đổi
        if (password != null && !password.isEmpty()) {
            if (password.length() < 8 || !password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$")) {
                model.addAttribute("message", "Mật khẩu phải có ít nhất 8 ký tự và chứa cả chữ lẫn số!");
                model.addAttribute("user", user);
                return "web/edit-profile";
            }
            user.setPassword(passwordEncoder.encode(password));
        }

        user.setFullname(fullname);
        user.setPhone(phone);

        userRepository.save(user);
        session.setAttribute("user", user);
        model.addAttribute("message", "Cập nhật thông tin thành công!");
        model.addAttribute("user", user);
        return "web/edit-profile";
    }
}