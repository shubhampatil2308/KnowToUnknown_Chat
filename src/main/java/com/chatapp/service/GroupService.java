package com.chatapp.service;

import com.chatapp.dto.ChatGroupDTO;
import com.chatapp.dto.GroupMessageDTO;
import com.chatapp.entity.ChatGroup;
import com.chatapp.entity.GroupMember;
import com.chatapp.entity.GroupMessage;
import com.chatapp.entity.User;
import com.chatapp.repository.ChatGroupRepository;
import com.chatapp.repository.GroupMemberRepository;
import com.chatapp.repository.GroupMessageRepository;
import com.chatapp.repository.UserRepository;
import com.chatapp.util.Base64Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class GroupService {
    @Autowired
    private ChatGroupRepository chatGroupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private GroupMessageRepository groupMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    public ChatGroup createGroup(String name, String description, Long createdById, MultipartFile image) throws IOException {
        User creator = userRepository.findById(createdById)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatGroup group = new ChatGroup();
        group.setName(name);
        group.setDescription(description);
        group.setCreatedBy(creator);
        if (image != null && !image.isEmpty()) {
            group.setGroupImage(image.getBytes());
            group.setGroupImageType(image.getContentType());
        }
        group = chatGroupRepository.save(group);

        // Add creator as admin
        GroupMember admin = new GroupMember();
        admin.setGroup(group);
        admin.setUser(creator);
        admin.setRole(GroupMember.MemberRole.ADMIN);
        groupMemberRepository.save(admin);

        return group;
    }

    public void addMemberToGroup(Long groupId, Long userId) {
        ChatGroup group = chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (groupMemberRepository.existsByGroupAndUser(group, user)) {
            throw new RuntimeException("User is already a member");
        }

        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        member.setRole(GroupMember.MemberRole.MEMBER);
        groupMemberRepository.save(member);
    }

    public void removeMemberFromGroup(Long groupId, Long userId) {
        ChatGroup group = chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        GroupMember member = groupMemberRepository.findByGroupAndUser(group, user)
                .orElseThrow(() -> new RuntimeException("User is not a member"));
        groupMemberRepository.delete(member);
    }

    public List<ChatGroupDTO> getUserGroups(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return groupMemberRepository.findByUser(user)
                .stream()
                .map(member -> convertToDTO(member.getGroup()))
                .collect(Collectors.toList());
    }

    public GroupMessage sendGroupMessage(Long groupId, Long senderId, String content, GroupMessage.MessageType type) {
        ChatGroup group = chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!groupMemberRepository.existsByGroupAndUser(group, sender)) {
            throw new RuntimeException("User is not a member of this group");
        }

        GroupMessage message = new GroupMessage();
        message.setGroup(group);
        message.setSender(sender);
        message.setContent(content);
        message.setType(type);
        return groupMessageRepository.save(message);
    }

    public GroupMessage sendGroupMediaMessage(Long groupId, Long senderId, MultipartFile file, GroupMessage.MessageType type) throws IOException {
        ChatGroup group = chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!groupMemberRepository.existsByGroupAndUser(group, sender)) {
            throw new RuntimeException("User is not a member of this group");
        }

        GroupMessage message = new GroupMessage();
        message.setGroup(group);
        message.setSender(sender);
        message.setContent(file.getOriginalFilename());
        message.setType(type);
        message.setMediaData(file.getBytes());
        message.setMediaType(file.getContentType());
        message.setFileName(file.getOriginalFilename());
        return groupMessageRepository.save(message);
    }

    public List<GroupMessageDTO> getGroupMessages(Long groupId) {
        ChatGroup group = chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        return groupMessageRepository.findByGroupOrderByTimestampAsc(group)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ChatGroupDTO getGroupDetails(Long groupId) {
        ChatGroup group = chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        return convertToDTO(group);
    }

    public ChatGroupDTO convertToDTO(ChatGroup group) {
        ChatGroupDTO dto = new ChatGroupDTO();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        dto.setCreatedById(group.getCreatedBy().getId());
        dto.setCreatedByName(group.getCreatedBy().getName());
        if (group.getGroupImage() != null) {
            dto.setGroupImageBase64(Base64Util.encode(group.getGroupImage()));
            dto.setGroupImageType(group.getGroupImageType());
        }
        dto.setCreatedAt(group.getCreatedAt());
        dto.setMembers(groupMemberRepository.findByGroup(group)
                .stream()
                .map(member -> userService.convertToDTO(member.getUser()))
                .collect(Collectors.toList()));
        return dto;
    }

    public GroupMessageDTO convertToDTO(GroupMessage message) {
        GroupMessageDTO dto = new GroupMessageDTO();
        dto.setId(message.getId());
        dto.setGroupId(message.getGroup().getId());
        dto.setGroupName(message.getGroup().getName());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderName(message.getSender().getName());
        dto.setSenderUsername(message.getSender().getUsername());
        dto.setContent(message.getContent());
        dto.setType(message.getType().name());
        if (message.getMediaData() != null) {
            dto.setMediaDataBase64(Base64Util.encode(message.getMediaData()));
            dto.setMediaType(message.getMediaType());
            dto.setFileName(message.getFileName());
        }
        dto.setTimestamp(message.getTimestamp());
        return dto;
    }
}

