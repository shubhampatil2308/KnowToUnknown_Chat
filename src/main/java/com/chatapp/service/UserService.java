package com.chatapp.service;

import com.chatapp.dto.UserDTO;
import com.chatapp.entity.*;
import com.chatapp.repository.*;
import com.chatapp.util.Base64Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private GroupMessageRepository groupMessageRepository;

    @Autowired
    private ChatGroupRepository chatGroupRepository;

    public User registerUser(String username, String email, String password, String name, String phone) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setName(name);
        user.setPhone(phone);
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<UserDTO> searchUsers(String query) {
        return userRepository.findAll().stream()
                .filter(user -> user.getUsername().toLowerCase().contains(query.toLowerCase()) ||
                               user.getName().toLowerCase().contains(query.toLowerCase()) ||
                               user.getEmail().toLowerCase().contains(query.toLowerCase()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public User updateUserProfile(Long userId, String name, String status, String theme, String email, String phone) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (name != null && !name.trim().isEmpty()) user.setName(name);
        if (status != null) user.setStatus(status);
        if (theme != null) user.setTheme(theme);
        
        // Update email with uniqueness check
        if (email != null && !email.trim().isEmpty()) {
            if (!email.equals(user.getEmail())) {
                if (userRepository.existsByEmail(email)) {
                    throw new RuntimeException("Email already exists");
                }
                user.setEmail(email);
            }
        }
        
        // Update phone
        if (phone != null) {
            user.setPhone(phone.trim().isEmpty() ? null : phone);
        }
        
        return userRepository.save(user);
    }

    public User updateProfilePicture(Long userId, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setProfilePicture(file.getBytes());
        user.setProfilePictureType(file.getContentType());
        return userRepository.save(user);
    }

    public void setOnlineStatus(Long userId, boolean online) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setOnline(online);
        userRepository.save(user);
    }

    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Delete all messages where user is sender or receiver
        List<Message> sentMessages = messageRepository.findBySender(user);
        messageRepository.deleteAll(sentMessages);

        List<Message> receivedMessages = messageRepository.findByReceiver(user);
        messageRepository.deleteAll(receivedMessages);

        // Delete all friend requests where user is sender or receiver
        List<FriendRequest> sentRequests = friendRequestRepository.findBySender(user);
        friendRequestRepository.deleteAll(sentRequests);

        List<FriendRequest> receivedRequests = friendRequestRepository.findByReceiver(user);
        friendRequestRepository.deleteAll(receivedRequests);

        // Handle groups where user is creator - transfer to first admin or delete if no admins
        List<ChatGroup> createdGroups = chatGroupRepository.findByCreatedBy(user);

        for (ChatGroup group : createdGroups) {
            List<GroupMember> members = groupMemberRepository.findByGroup(group);
            // Find first admin member (excluding the user being deleted)
            Optional<GroupMember> newAdmin = members.stream()
                    .filter(m -> !m.getUser().getId().equals(userId) && 
                            m.getRole() == GroupMember.MemberRole.ADMIN)
                    .findFirst();

            if (newAdmin.isPresent()) {
                // Transfer ownership to first admin
                group.setCreatedBy(newAdmin.get().getUser());
                chatGroupRepository.save(group);
            } else {
                // Find first member to transfer ownership
                Optional<GroupMember> firstMember = members.stream()
                        .filter(m -> !m.getUser().getId().equals(userId))
                        .findFirst();

                if (firstMember.isPresent()) {
                    group.setCreatedBy(firstMember.get().getUser());
                    firstMember.get().setRole(GroupMember.MemberRole.ADMIN);
                    groupMemberRepository.save(firstMember.get());
                    chatGroupRepository.save(group);
                } else {
                    // No other members, delete the group and all its messages
                    List<GroupMessage> groupMessages = groupMessageRepository.findByGroupOrderByTimestampAsc(group);
                    groupMessageRepository.deleteAll(groupMessages);
                    groupMemberRepository.deleteAll(members);
                    chatGroupRepository.delete(group);
                }
            }
        }

        // Delete all group memberships
        List<GroupMember> memberships = groupMemberRepository.findByUser(user);
        groupMemberRepository.deleteAll(memberships);

        // Delete all group messages where user is sender
        List<GroupMessage> groupMessages = groupMessageRepository.findBySender(user);
        groupMessageRepository.deleteAll(groupMessages);

        // Finally, delete the user
        userRepository.delete(user);
    }

    public UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setPhone(user.getPhone());
        dto.setOnline(user.isOnline());
        dto.setStatus(user.getStatus());
        dto.setTheme(user.getTheme());
        if (user.getProfilePicture() != null) {
            dto.setProfilePictureBase64(Base64Util.encode(user.getProfilePicture()));
            dto.setProfilePictureType(user.getProfilePictureType());
        }
        return dto;
    }
}


