package com.poly.asm.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import com.poly.asm.daos.UserRepository;
import com.poly.asm.entitys.User;
import com.poly.asm.entitys.CustomOAuth2User;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        logger.info("Starting OAuth2 login processing for provider: {}", userRequest.getClientRegistration().getRegistrationId());
        OAuth2User oAuth2User;
        try {
            if (userRequest instanceof OidcUserRequest) {
                oAuth2User = super.loadUser(userRequest); // Xử lý OIDC cho Google
            } else {
                oAuth2User = super.loadUser(userRequest); // Xử lý OAuth2 cho Facebook
            }
            logger.info("OAuth2 user loaded successfully: attributes={}", oAuth2User.getAttributes());
        } catch (Exception e) {
            logger.error("Failed to load OAuth2 user for provider {}: {}", 
                         userRequest.getClientRegistration().getRegistrationId(), e.getMessage(), e);
            throw new RuntimeException("Failed to load OAuth2 user: " + e.getMessage(), e);
        }
        String provider = userRequest.getClientRegistration().getRegistrationId(); // google or facebook
        String providerId = oAuth2User.getName();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        logger.info("OAuth2 user details: email={}, name={}, provider={}, providerId={}", email, name, provider, providerId);

        if (email == null) {
            logger.error("Email not provided by OAuth2 provider: {}", provider);
            throw new IllegalArgumentException("Email not provided by OAuth2 provider");
        }

        User user = userRepository.findByProviderIdAndProvider(providerId, provider);
        if (user != null) {
            logger.info("Found existing user: providerId={}, provider={}, username={}, email={}", 
                        providerId, provider, user.getUsername(), user.getEmail());
            return new CustomOAuth2User(oAuth2User, user);
        }

        logger.info("No user found for providerId: {}, provider: {}. Checking email: {}", providerId, provider, email);
        // Kiểm tra email trùng lặp
        User existingUserByEmail = userRepository.findByEmail(email);
        if (existingUserByEmail != null) {
            logger.warn("Email {} already exists for user: {}, provider: {}", 
                        email, existingUserByEmail.getUsername(), existingUserByEmail.getProvider());
            // Cập nhật providerId và username nếu email trùng
            existingUserByEmail.setProviderId(providerId);
            existingUserByEmail.setProvider(provider);
            existingUserByEmail.setUsername(email.split("@")[0] + "_" + provider + "_" + System.currentTimeMillis());
            existingUserByEmail.setFullname(name != null ? name : existingUserByEmail.getFullname());
            try {
                user = userRepository.save(existingUserByEmail);
                logger.info("Updated existing user: providerId={}, email={}, username={}", 
                            providerId, email, user.getUsername());
            } catch (Exception e) {
                logger.error("Failed to update user: providerId={}, email={}, error={}", providerId, email, e.getMessage(), e);
                throw new RuntimeException("Failed to update user: " + e.getMessage(), e);
            }
        } else {
            logger.info("Creating new user for providerId: {}, provider: {}", providerId, provider);
            user = new User();
            user.setUsername(email.split("@")[0] + "_" + provider + "_" + System.currentTimeMillis());
            user.setEmail(email);
            user.setFullname(name != null ? name : "Unknown");
            user.setPassword("");
            user.setRole("USER");
            user.setActive(true);
            user.setProvider(provider);
            user.setProviderId(providerId);
            user.setPhone(null); // Không set phone từ OAuth2
            try {
                user = userRepository.save(user);
                logger.info("User saved successfully: providerId={}, email={}, username={}", 
                            providerId, email, user.getUsername());
            } catch (Exception e) {
                logger.error("Failed to save user to database: providerId={}, email={}, username={}, error={}", 
                             providerId, email, user.getUsername(), e.getMessage(), e);
                throw new RuntimeException("Failed to save user: " + e.getMessage(), e);
            }
        }

        return new CustomOAuth2User(oAuth2User, user);
    }

    public User findUserByEmail(String email) {
        logger.info("Finding user by email: {}", email);
        User user = userRepository.findByEmail(email);
        logger.info("Find user result: {}", user != null ? user.getUsername() : "null");
        return user;
    }
}
