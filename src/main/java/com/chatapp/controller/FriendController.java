package com.chatapp.controller;

import com.chatapp.dto.FriendRequestDTO;
import com.chatapp.service.FriendService;
import com.chatapp.service.UserService;
import com.chatapp.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
@CrossOrigin(origins = "http://localhost:8080")
public class FriendController {

    @Autowired
    private FriendService friendService;

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

    @PostMapping("/request")
    public ResponseEntity<?> sendFriendRequest(@RequestHeader("Authorization") String token,
                                              @RequestParam Long receiverId) {
        Long senderId = getUserIdFromToken(token);
        if (senderId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid token"));
        }
        try {
            friendService.sendFriendRequest(senderId, receiverId);
            return ResponseEntity.ok(Map.of("message", "Friend request sent"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/accept/{requestId}")
    public ResponseEntity<?> acceptFriendRequest(@RequestHeader("Authorization") String token,
                                                 @PathVariable Long requestId) {
        try {
            friendService.acceptFriendRequest(requestId);
            return ResponseEntity.ok(Map.of("message", "Friend request accepted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reject/{requestId}")
    public ResponseEntity<?> rejectFriendRequest(@RequestHeader("Authorization") String token,
                                                 @PathVariable Long requestId) {
        try {
            friendService.rejectFriendRequest(requestId);
            return ResponseEntity.ok(Map.of("message", "Friend request rejected"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<List<FriendRequestDTO>> getPendingRequests(@RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(friendService.getPendingRequests(userId));
    }

    @GetMapping("/sent")
    public ResponseEntity<List<FriendRequestDTO>> getSentRequests(@RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(friendService.getSentRequests(userId));
    }

    @GetMapping("/accepted")
    public ResponseEntity<List<FriendRequestDTO>> getAcceptedFriends(@RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(friendService.getAcceptedFriends(userId));
    }

    @GetMapping("/check/{userId2}")
    public ResponseEntity<?> areFriends(@RequestHeader("Authorization") String token,
                                       @PathVariable Long userId2) {
        Long userId1 = getUserIdFromToken(token);
        if (userId1 == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid token"));
        }
        boolean areFriends = friendService.areFriends(userId1, userId2);
        return ResponseEntity.ok(Map.of("areFriends", areFriends));
    }

    @DeleteMapping("/remove/{friendId}")
    public ResponseEntity<?> removeFriend(@RequestHeader("Authorization") String token,
                                          @PathVariable Long friendId) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid token"));
        }
        try {
            friendService.removeFriend(userId, friendId);
            return ResponseEntity.ok(Map.of("message", "Friend removed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}


