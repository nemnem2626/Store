package com.poly.asm.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.poly.asm.daos.UserRepository;
import com.poly.asm.entitys.User;
import jakarta.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Controller
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String login(Model model, @RequestParam(value = "error", required = false) String error,
                       @RequestParam(value = "success", required = false) String success,
                       @RequestParam(value = "username", required = false) String username) {
        logger.info("Accessing login page, error: {}, success: {}", error, success);
        try {
            if (error != null) {
                model.addAttribute("message", URLDecoder.decode(error, StandardCharsets.UTF_8.toString()));
            }
            if (success != null) {
                model.addAttribute("message", URLDecoder.decode(success, StandardCharsets.UTF_8.toString()));
            }
        } catch (Exception e) {
            logger.error("Failed to decode URL parameter: {}", e.getMessage());
            model.addAttribute("message", "Lỗi xử lý thông báo");
        }
        model.addAttribute("username", username);
        return "web/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        logger.info("Accessing register page");
        return "web/register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam("username") String username,
                              @RequestParam("password") String password,
                              @RequestParam("email") String email,
                              @RequestParam(value = "fullName", required = false) String fullName,
                              @RequestParam(value = "phone", required = false) String phone,
                              @RequestParam(value = "address", required = false) String address,
                              Model model) {
        logger.info("Processing registration for username: {}", username);
        try {
            // Kiểm tra username (chỉ chứa chữ cái, không chứa số hoặc ký tự đặc biệt)
            if (!username.matches("^[a-zA-Z]+$")) {
                model.addAttribute("message", "Tên đăng nhập chỉ được chứa chữ cái!");
                return "web/register";
            }

            // Kiểm tra email
            if (userRepository.findByEmail(email) != null) {
                model.addAttribute("message", "Email đã được sử dụng");
                return "web/register";
            }

            // Kiểm tra password (tối thiểu 8 ký tự, chứa chữ và số)
            if (password.length() < 8 || !password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$")) {
                model.addAttribute("message", "Mật khẩu phải có ít nhất 8 ký tự và chứa cả chữ lẫn số!");
                return "web/register";
            }

            // Kiểm tra phone (chỉ chứa số, không chứa chữ)
            if (phone != null && !phone.isEmpty()) {
                if (!phone.matches("^\\d+$")) {
                    model.addAttribute("message", "Số điện thoại chỉ được chứa số!");
                    return "web/register";
                }
                if (phone.length() > 10) {
                    model.addAttribute("message", "Số điện thoại không được vượt quá 10 chữ số!");
                    return "web/register";
                }
            }

            if (userRepository.findByUsername(username) != null) {
                model.addAttribute("message", "Tên đăng nhập đã tồn tại");
                return "web/register";
            }

            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setEmail(email);
            user.setFullname(fullName != null && !fullName.isEmpty() ? fullName : "Unknown");
            user.setPhone(phone != null && !phone.isEmpty() && phone.length() <= 10 ? phone : null);
            user.setAddress(address);
            user.setRole("USER");
            user.setProvider("form");
            user.setProviderId(null); // Không set providerId cho form
            user.setActive(true);
            userRepository.save(user);
            logger.info("User registered successfully: {}", username);
            return "redirect:/login?success=" + URLEncoder.encode("Đăng ký thành công", StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            logger.error("Registration failed: {}", e.getMessage(), e);
            model.addAttribute("message", "Đăng ký thất bại: " + e.getMessage());
            return "web/register";
        }
    }

    @GetMapping("/perform_login")
    public String handleLogin(Authentication authentication, HttpSession session) {
        logger.info("Processing login for username: {}", authentication.getName());
        User user = userRepository.findByUsername(authentication.getName());
        if (user != null) {
            logger.info("Authentication successful for username: {}", user.getUsername());
            session.setAttribute("user", user);
            logger.info("Redirecting user {} with role: {}", user.getUsername(), user.getRole());
            if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                return "redirect:/admin/index";
            } else if ("STAFF".equalsIgnoreCase(user.getRole())) {
                return "redirect:/staff/index";
            } else {
                return "redirect:/home";
            }
        } else {
            logger.error("User not found for username: {}", authentication.getName());
            try {
                return "redirect:/login?error=" + URLEncoder.encode("Đăng nhập thất bại", StandardCharsets.UTF_8.toString());
            } catch (Exception e) {
                logger.error("Failed to encode error message: {}", e.getMessage());
                return "redirect:/login?error=Failed_to_encode_error_message";
            }
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        logger.info("Logging out user");
        session.invalidate();
        return "redirect:/home";
    }

    @GetMapping("/debug-auth")
    public String debugAuth(Model model, Authentication authentication, HttpSession session) {
        String authInfo = authentication != null ? authentication.getName() : "null";
        String principalInfo = authentication != null ? authentication.getPrincipal().getClass().getSimpleName() : "null";
        User sessionUser = (User) session.getAttribute("user");
        String sessionInfo = sessionUser != null ? sessionUser.getUsername() + " (" + sessionUser.getRole() + ")" : "null";
        model.addAttribute("authInfo", "Auth: " + authInfo + ", Principal: " + principalInfo + ", Session user: " + sessionInfo);
        return "debug-auth";
    }

    @GetMapping("/")
    public String root(Authentication authentication, HttpSession session) {
        logger.info("Accessing root URL");
        User user = (User) session.getAttribute("user");
        if (user != null) {
            logger.info("Root redirect for user: {}, role: {}", user.getUsername(), user.getRole());
            if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                return "redirect:/admin/index";
            } else if ("STAFF".equalsIgnoreCase(user.getRole())) {
                return "redirect:/staff/index";
            } else {
                return "redirect:/home";
            }
        }
        return "redirect:/home";
    }
}