package com.chatapp.controller;

import com.chatapp.dto.ChatGroupDTO;
import com.chatapp.dto.GroupMessageDTO;
import com.chatapp.service.GroupService;
import com.chatapp.service.UserService;
import com.chatapp.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "http://localhost:8080")
public class GroupController {

    @Autowired
    private GroupService groupService;

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

    @PostMapping("/create")
    public ResponseEntity<?> createGroup(@RequestHeader("Authorization") String token,
                                        @RequestParam String name,
                                        @RequestParam(required = false) String description,
                                        @RequestParam(required = false) MultipartFile image) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid token"));
        }
        try {
            return ResponseEntity.ok(groupService.convertToDTO(groupService.createGroup(name, description, userId, image)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-groups")
    public ResponseEntity<List<ChatGroupDTO>> getMyGroups(@RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(groupService.getUserGroups(userId));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<?> addMember(@RequestHeader("Authorization") String token,
                                      @PathVariable Long groupId,
                                      @RequestParam Long userId) {
        try {
            groupService.addMemberToGroup(groupId, userId);
            return ResponseEntity.ok(Map.of("message", "Member added"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<?> removeMember(@RequestHeader("Authorization") String token,
                                         @PathVariable Long groupId,
                                         @PathVariable Long userId) {
        try {
            groupService.removeMemberFromGroup(groupId, userId);
            return ResponseEntity.ok(Map.of("message", "Member removed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{groupId}/messages")
    public ResponseEntity<List<GroupMessageDTO>> getGroupMessages(@RequestHeader("Authorization") String token,
                                                                  @PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getGroupMessages(groupId));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroupDetails(@RequestHeader("Authorization") String token,
                                             @PathVariable Long groupId) {
        try {
            return ResponseEntity.ok(groupService.getGroupDetails(groupId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}


