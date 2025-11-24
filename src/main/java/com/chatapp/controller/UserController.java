package com.chatapp.controller;

import com.chatapp.dto.UserDTO;
import com.chatapp.entity.User;
import com.chatapp.service.UserService;
import com.chatapp.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:8080")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    private Long getUserIdFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            String username = jwtUtil.extractUsername(token);
            return userService.findByUsername(username)
                    .map(u -> u.getId())
                    .orElse(null);
        }
        return null;
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserDTO>> getAllUsers(@RequestHeader(value = "Authorization", required = false) String token) {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserDTO>> searchUsers(@RequestParam String query) {
        return ResponseEntity.ok(userService.searchUsers(query));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(userService.convertToDTO(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestHeader("Authorization") String token,
                                          @RequestBody Map<String, String> payload) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid token"));
        }
        try {
            String name = payload.get("name");
            String status = payload.get("status");
            String theme = payload.get("theme");
            String email = payload.get("email");
            String phone = payload.get("phone");
            User updated = userService.updateUserProfile(userId, name, status, theme, email, phone);
            return ResponseEntity.ok(userService.convertToDTO(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/profile/picture")
    public ResponseEntity<?> updateProfilePicture(@RequestHeader("Authorization") String token,
                                                  @RequestParam MultipartFile file) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid token"));
        }
        try {
            User updated = userService.updateProfilePicture(userId, file);
            return ResponseEntity.ok(userService.convertToDTO(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/online")
    public ResponseEntity<?> setOnlineStatus(@RequestHeader("Authorization") String token,
                                            @RequestParam boolean online) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid token"));
        }
        userService.setOnlineStatus(userId, online);
        return ResponseEntity.ok(Map.of("message", "Status updated"));
    }

    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount(@RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid token"));
        }
        try {
            userService.deleteUser(userId);
            return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}


