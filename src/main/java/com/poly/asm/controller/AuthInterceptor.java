package com.poly.asm.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.poly.asm.daos.UserRepository;
import com.poly.asm.entitys.User;
import com.poly.asm.entitys.CustomOAuth2User;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);

    private static final List<String> PUBLIC_URLS = Arrays.asList(
            "/login", "/register", "/oauth2/", "/assets/", "/home", "/product", "/debug-auth", "/error"
    );
    private static final List<String> USER_REQUIRED_URLS = Arrays.asList(
            "/account/edit", "/account/chgpwd", "/order/"
    );
    private static final List<String> ADMIN_REQUIRED_URLS = Arrays.asList(
            "/admin/"
    );
    private static final List<String> STAFF_REQUIRED_URLS = Arrays.asList(
            "/staff/"
    );

    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        logger.info("Intercepting URI: {}", uri);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) request.getSession().getAttribute("user");

        // Cập nhật user từ Authentication nếu cần
        if (user == null && auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            if (auth.getPrincipal() instanceof CustomOAuth2User) {
                user = ((CustomOAuth2User) auth.getPrincipal()).getUser();
                logger.info("Set user from CustomOAuth2User: {}", user.getUsername());
            } else if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
                String username = ((org.springframework.security.core.userdetails.User) auth.getPrincipal()).getUsername();
                user = userRepository.findByUsername(username);
                logger.info("Set user from form login: {}", username);
            } else if (auth.getPrincipal() instanceof OidcUser) {
                OidcUser oidcUser = (OidcUser) auth.getPrincipal();
                String providerId = oidcUser.getSubject();
                String provider = "google";
                user = userRepository.findByProviderIdAndProvider(providerId, provider);
                logger.info("Set user from OidcUser: providerId={}, provider={}", providerId, provider);
            }
            if (user != null) {
                request.getSession().setAttribute("user", user);
                logger.info("User set in session: username={}, role={}", user.getUsername(), user.getRole());
            }
        }

        // Kiểm tra quyền dựa trên Authentication
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean isStaff = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"));

            if (ADMIN_REQUIRED_URLS.stream().anyMatch(uri::startsWith) && !isAdmin) {
                logger.warn("Admin access denied for URI: {}", uri);
                response.sendRedirect("/login?error=" + URLEncoder.encode("Bạn không có quyền truy cập", StandardCharsets.UTF_8));
                return false;
            }
            if (STAFF_REQUIRED_URLS.stream().anyMatch(uri::startsWith) && !isStaff) {
                logger.warn("Staff access denied for URI: {}", uri);
                response.sendRedirect("/login?error=" + URLEncoder.encode("Bạn không có quyền truy cập", StandardCharsets.UTF_8));
                return false;
            }
            if (USER_REQUIRED_URLS.stream().anyMatch(uri::startsWith) && !isAdmin && !isStaff && user == null) {
                logger.warn("User access denied for URI: {}", uri);
                response.sendRedirect("/login?error=" + URLEncoder.encode("Vui lòng đăng nhập", StandardCharsets.UTF_8));
                return false;
            }
        } else if (!PUBLIC_URLS.stream().anyMatch(uri::startsWith) && !uri.equals("/login")) {
            logger.warn("Unauthenticated access denied for URI: {}", uri);
            response.sendRedirect("/login?error=" + URLEncoder.encode("Vui lòng đăng nhập", StandardCharsets.UTF_8));
            return false;
        }

        logger.info("Allowing access to URI: {}", uri);
        return true;
    }
}