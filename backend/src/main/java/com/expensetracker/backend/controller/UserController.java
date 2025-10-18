package com.expensetracker.backend.controller;

import com.expensetracker.backend.model.User;
import com.expensetracker.backend.repository.UserRepository;
import com.expensetracker.backend.security.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*") // allow calls from Android app
public class UserController {

    @Autowired
    private UserRepository userRepository;

    // Get all users
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Signup
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        // Check if email already exists
        if (userRepository.findByEmail(user.getEmail()) != null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Email already registered");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        User savedUser = userRepository.save(user);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("user", savedUser);
        return ResponseEntity.ok(response);
    }

    // Login
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody User loginRequest) {
        Map<String, Object> response = new HashMap<>();
        User user = userRepository.findByEmail(loginRequest.getEmail());

        if (user != null && user.getPassword().equals(loginRequest.getPassword())) {
            String token = JwtUtil.generateToken(user.getEmail());
            response.put("success", true);
            response.put("token", token);
            response.put("user", user);
        } else {
            response.put("success", false);
            response.put("message", "Invalid credentials");
        }
        return response;
    }


    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Missing or invalid Authorization header"));
        }

        String jwt = token.replace("Bearer ", "");
        try {
            if (!JwtUtil.isTokenValid(jwt)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Invalid or expired token"));
            }

            String email = JwtUtil.extractEmail(jwt);
            User user = userRepository.findByEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "User not found for token"));
            }
            return ResponseEntity.ok(user);
        } catch (JwtException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Invalid or expired token"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Server error"));
        }
    }
}
