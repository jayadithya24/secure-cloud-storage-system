package com.securecloud.storage.controller;

import com.securecloud.storage.model.User;
import com.securecloud.storage.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public User register(@RequestBody User user) {
        return userService.registerUser(user);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {

        User loggedUser = userService.loginUser(user.getEmail(), user.getPassword());

        if (loggedUser != null) {
            return ResponseEntity.ok(loggedUser);
        }

        return ResponseEntity.status(401).body("Invalid credentials");
    }
}