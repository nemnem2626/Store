package com.poly.asm.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.poly.asm.services.CustomOAuth2UserService;
import com.poly.asm.services.CustomOidcUserService;
import com.poly.asm.services.CustomUserDetailsService;
import com.poly.asm.entitys.User;
import com.poly.asm.entitys.CustomOAuth2User;
import com.poly.asm.daos.UserRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService; // Cho Facebook

    @Autowired
    private CustomOidcUserService customOidcUserService;    // Cho Google

    @Autowired
    private UserRepository userRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return customUserDetailsService;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        logger.info("Initializing AuthenticationManager");
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring SecurityFilterChain");
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/oauth2/**", "/assets/**", "/home", "/product", "/debug-auth", "/error", "/vnpay-return").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/staff/**").hasRole("STAFF")
                .requestMatchers("/account/**", "/orders", "/user/edit").authenticated()
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/perform_login")
                .successHandler((request, response, authentication) -> {
                    logger.info("Form login successful for user: {}", authentication.getName());
                    User user = userRepository.findByUsername(authentication.getName());
                    if (user != null) {
                        request.getSession().setAttribute("user", user);
                        logger.info("User stored in session: username={}, role={}", user.getUsername(), user.getRole());
                        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                            response.sendRedirect("/admin/index");
                        } else if ("STAFF".equalsIgnoreCase(user.getRole())) {
                            response.sendRedirect("/staff/index");
                        } else {
                            response.sendRedirect("/home");
                        }
                    } else {
                        logger.error("User not found for username: {}", authentication.getName());
                        response.sendRedirect("/login?error=" + encodeErrorMessage("Đăng nhập thất bại"));
                    }
                })
                .failureUrl("/login?error=" + encodeErrorMessage("Sai tài khoản hoặc mật khẩu"))
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> {
                    logger.info("Configuring OAuth2 userInfoEndpoint");
                    userInfo.oidcUserService(customOidcUserService);    // Cho Google
                    userInfo.userService(customOAuth2UserService);     // Cho Facebook
                })
                .successHandler((request, response, authentication) -> {
                    logger.info("OAuth2 login successful for user: {}", authentication.getName());
                    logger.info("Principal type: {}", authentication.getPrincipal().getClass().getSimpleName());
                    User user = null;
                    if (authentication.getPrincipal() instanceof CustomOAuth2User) {
                        user = ((CustomOAuth2User) authentication.getPrincipal()).getUser();
                        logger.info("Principal is CustomOAuth2User: username={}, role={}", user.getUsername(), user.getRole());
                    } else {
                        logger.warn("Unexpected principal type: {}, falling back to database lookup", authentication.getPrincipal().getClass().getSimpleName());
                        String provider = authentication.getPrincipal() instanceof OidcUser ? "google" : "facebook";
                        user = userRepository.findByProviderIdAndProvider(authentication.getName(), provider);
                        logger.info("Fallback: Found user in database: username={}", user != null ? user.getUsername() : "null");
                        if (user == null) {
                            logger.warn("No user found in database, creating new user for providerId: {}, provider: {}", authentication.getName(), provider);
                            String email = authentication.getPrincipal() instanceof OidcUser 
                                ? ((OidcUser) authentication.getPrincipal()).getEmail()
                                : ((OAuth2User) authentication.getPrincipal()).getAttribute("email");
                            String name = authentication.getPrincipal() instanceof OidcUser 
                                ? ((OidcUser) authentication.getPrincipal()).getFullName()
                                : ((OAuth2User) authentication.getPrincipal()).getAttribute("name");
                            if (email == null) {
                                logger.error("Email not provided by OAuth2 provider: {}", provider);
                                response.sendRedirect("/login?error=" + encodeErrorMessage("Email không được cung cấp bởi provider"));
                                return;
                            }
                            user = new User();
                            user.setUsername(email.split("@")[0] + "_" + provider + "_" + System.currentTimeMillis());
                            user.setEmail(email);
                            user.setFullname(name != null ? name : "Unknown");
                            user.setPassword("");
                            user.setRole("USER");
                            user.setActive(true);
                            user.setProvider(provider);
                            user.setProviderId(authentication.getName());
                            user.setPhone(null);
                            try {
                                user = userRepository.save(user);
                                logger.info("Created new user: providerId={}, email={}, username={}", 
                                            authentication.getName(), email, user.getUsername());
                            } catch (Exception e) {
                                logger.error("Failed to save new user: providerId={}, email={}, error={}", 
                                             authentication.getName(), email, e.getMessage(), e);
                                response.sendRedirect("/login?error=" + encodeErrorMessage("Lỗi khi lưu user: " + e.getMessage()));
                                return;
                            }
                        }
                        // Cập nhật Authentication với GrantedAuthority
                        authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            new CustomOAuth2User(authentication.getPrincipal(), user),
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                        );
                        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                    if (user != null) {
                        request.getSession().setAttribute("user", user);
                        logger.info("User stored in session: username={}, role={}", user.getUsername(), user.getRole());
                        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                            response.sendRedirect("/admin/index");
                        } else if ("STAFF".equalsIgnoreCase(user.getRole())) {
                            response.sendRedirect("/staff/index");
                        } else {
                            response.sendRedirect("/home");
                        }
                    } else {
                        logger.error("User not found for OAuth2 principal: {}, provider={}", authentication.getName(), 
                            authentication.getPrincipal() instanceof OidcUser ? "google" : "facebook");
                        response.sendRedirect("/login?error=" + encodeErrorMessage("Đăng nhập OAuth2 thất bại"));
                    }
                })
                .failureHandler((request, response, exception) -> {
                    logger.error("OAuth2 login failed: {}", exception.getMessage(), exception);
                    response.sendRedirect("/login?error=" + encodeErrorMessage("Đăng nhập OAuth2 thất bại: " + exception.getMessage()));
                })
            )
            .exceptionHandling(exception -> exception
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    logger.error("Access denied: {}", accessDeniedException.getMessage());
                    response.sendRedirect("/login?error=" + encodeErrorMessage("Bạn không có quyền truy cập"));
                })
                .authenticationEntryPoint((request, response, authException) -> {
                    logger.error("Authentication error: {}", authException.getMessage());
                    response.sendRedirect("/login?error=" + encodeErrorMessage("Vui lòng đăng nhập"));
                })
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/home")
                .deleteCookies("userId", "rememberedUsername")
                .invalidateHttpSession(true)
            );

        return http.build();
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