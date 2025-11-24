package com.chatapp.controller;

import com.chatapp.dto.MessageDTO;
import com.chatapp.service.ChatService;
import com.chatapp.service.UserService;
import com.chatapp.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:8080")
public class ChatController {

    @Autowired
    private ChatService chatService;

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

    @GetMapping("/conversation/{userId2}")
    public ResponseEntity<List<MessageDTO>> getConversation(@RequestHeader("Authorization") String token,
                                                           @PathVariable Long userId2) {
        Long userId1 = getUserIdFromToken(token);
        if (userId1 == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(chatService.getConversation(userId1, userId2));
    }

    @PostMapping("/read/{userId2}")
    public ResponseEntity<?> markConversationAsRead(@RequestHeader("Authorization") String token,
                                                   @PathVariable Long userId2) {
        Long userId1 = getUserIdFromToken(token);
        if (userId1 == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid token"));
        }
        try {
            chatService.markConversationAsRead(userId1, userId2);
            return ResponseEntity.ok(Map.of("message", "Messages marked as read"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

