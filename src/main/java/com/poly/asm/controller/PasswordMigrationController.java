package com.poly.asm.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.poly.asm.daos.UserRepository;
import com.poly.asm.entitys.User;

@RestController
public class PasswordMigrationController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/migrate-passwords")
    public String migratePasswords() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (user.getProvider() == null && !user.getPassword().startsWith("$2a$")) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
                userRepository.save(user);
            }
        }
        return "Password migration completed!";
    }
}
