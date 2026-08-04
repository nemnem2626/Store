package com.poly.asm.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import com.poly.asm.daos.UserRepository;
import com.poly.asm.entitys.User;
import com.poly.asm.entitys.CustomOAuth2User;

@Service
public class CustomOidcUserService extends OidcUserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOidcUserService.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        logger.info("Starting OIDC login processing for provider: {}", userRequest.getClientRegistration().getRegistrationId());
        OidcUser oidcUser;
        try {
            oidcUser = super.loadUser(userRequest); // Load user từ Google OIDC
            logger.info("OIDC user loaded successfully: attributes={}", oidcUser.getAttributes());
        } catch (Exception e) {
            logger.error("Failed to load OIDC user for provider {}: {}", 
                         userRequest.getClientRegistration().getRegistrationId(), e.getMessage(), e);
            throw new RuntimeException("Failed to load OIDC user: " + e.getMessage(), e);
        }

        String provider = userRequest.getClientRegistration().getRegistrationId(); // "google"
        String providerId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();
        logger.info("OIDC user details: email={}, name={}, provider={}, providerId={}", email, name, provider, providerId);

        if (email == null) {
            logger.error("Email not provided by OIDC provider: {}", provider);
            throw new IllegalArgumentException("Email not provided by OIDC provider");
        }

        User user = userRepository.findByProviderIdAndProvider(providerId, provider);
        if (user != null) {
            logger.info("Found existing user: providerId={}, provider={}, username={}, email={}, role={}", 
                        providerId, provider, user.getUsername(), user.getEmail(), user.getRole());
            return new CustomOAuth2User(oidcUser, user);
        }

        logger.info("No user found for providerId: {}, provider: {}. Checking email: {}", providerId, provider, email);
        User existingUserByEmail = userRepository.findByEmail(email);
        if (existingUserByEmail != null) {
            logger.warn("Email {} already exists for user: {}, provider: {}", 
                        email, existingUserByEmail.getUsername(), existingUserByEmail.getProvider());
            existingUserByEmail.setProviderId(providerId);
            existingUserByEmail.setProvider(provider);
            existingUserByEmail.setUsername(email.split("@")[0] + "_" + provider + "_" + System.currentTimeMillis());
            existingUserByEmail.setFullname(name != null ? name : existingUserByEmail.getFullname());
            try {
                user = userRepository.save(existingUserByEmail);
                logger.info("Updated existing user: providerId={}, email={}, username={}, role={}", 
                            providerId, email, user.getUsername(), user.getRole());
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
            user.setPhone(null);
            try {
                user = userRepository.save(user);
                logger.info("User saved successfully: providerId={}, email={}, username={}, role={}", 
                            providerId, email, user.getUsername(), user.getRole());
            } catch (Exception e) {
                logger.error("Failed to save user: providerId={}, email={}, error={}", providerId, email, e.getMessage(), e);
                throw new RuntimeException("Failed to save user: " + e.getMessage(), e);
            }
        }

        return new CustomOAuth2User(oidcUser, user);
    }
}