package com.chatapp.controller;

import com.chatapp.entity.User;
import com.chatapp.service.EmailService;
import com.chatapp.service.UserService;
import com.chatapp.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:8080")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String username,
                                     @RequestParam String email,
                                     @RequestParam String password,
                                     @RequestParam String name,
                                     @RequestParam(required = false) String phone,
                                     @RequestParam(required = false) MultipartFile profilePicture) {
        try {
            User user = userService.registerUser(username, email, password, name, phone);
            if (profilePicture != null && !profilePicture.isEmpty()) {
                userService.updateProfilePicture(user.getId(), profilePicture);
            }
            userService.setOnlineStatus(user.getId(), true);

            // Send registration success email (non-blocking best-effort)
            try {
                emailService.sendRegistrationEmail(user);
                System.out.println("Registration email service called for user: " + user.getEmail());
            } catch (Exception e) {
                System.err.println("Error calling registration email service: " + e.getMessage());
                // Don't fail registration if email fails
            }

            String token = jwtUtil.generateToken(user.getUsername());
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", userService.convertToDTO(user));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username,
                                  @RequestParam String password) {
        try {
            Optional<User> userOpt = userService.findByUsername(username);
            if (userOpt.isEmpty() || !passwordEncoder.matches(password, userOpt.get().getPassword())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid credentials"));
            }
            User user = userOpt.get();
            userService.setOnlineStatus(user.getId(), true);

            // Send login success email (non-blocking best-effort)
            try {
                emailService.sendLoginEmail(user);
                System.out.println("Login email service called for user: " + user.getEmail());
            } catch (Exception e) {
                System.err.println("Error calling login email service: " + e.getMessage());
                // Don't fail login if email fails
            }

            String token = jwtUtil.generateToken(user.getUsername());
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", userService.convertToDTO(user));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                String username = jwtUtil.extractUsername(token);
                Optional<User> user = userService.findByUsername(username);
                if (user.isPresent() && jwtUtil.validateToken(token, username)) {
                    return ResponseEntity.ok(userService.convertToDTO(user.get()));
                }
            }
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid token"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

