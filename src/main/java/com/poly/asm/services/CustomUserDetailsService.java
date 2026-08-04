package com.poly.asm.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.poly.asm.daos.UserRepository;
import com.poly.asm.entitys.User;
import java.util.Collections;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("Loading user by username: {}", username);
        User user = userRepository.findByUsername(username);
        if (user == null) {
            logger.error("User not found: {}", username);
            throw new UsernameNotFoundException("User not found: " + username);
        }
        if (!user.getActive()) {
            logger.error("User is inactive: {}", username);
            throw new UsernameNotFoundException("User is inactive: " + username);
        }
        logger.info("User loaded: username={}, role={}, active={}", 
                    user.getUsername(), user.getRole(), user.getActive());
        logger.info("Password in DB: {}", user.getPassword());
        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            user.getActive(),
            true,
            true,
            true,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
}