package com.securecloud.storage.controller;

import com.securecloud.storage.model.User;
import com.securecloud.storage.service.UserService;
import com.securecloud.storage.service.TokenService;
import com.securecloud.storage.service.AnomalyDetectionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AnomalyDetectionService anomalyDetectionService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody User user) {
        User registeredUser = userService.registerUser(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Registration successful");
        response.put("user", registeredUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody User user, HttpServletRequest request) {

        User loggedUser = userService.loginUser(user.getEmail(), user.getPassword());

        if (loggedUser != null) {
            anomalyDetectionService.trackLogin(loggedUser, request);

            Map<String, Object> response = new HashMap<>();
            response.put("token", tokenService.generateToken(loggedUser.getEmail()));
            response.put("user", loggedUser);
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid credentials"));
    }
}