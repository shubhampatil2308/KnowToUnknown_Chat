package com.chatapp.service;

import com.chatapp.dto.MessageDTO;
import com.chatapp.entity.Message;
import com.chatapp.entity.User;
import com.chatapp.repository.MessageRepository;
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
public class ChatService {
    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendService friendService;

    public Message sendMessage(Long senderId, Long receiverId, String content, Message.MessageType type) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        if (!friendService.areFriends(senderId, receiverId)) {
            throw new RuntimeException("Users are not friends");
        }

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        message.setType(type);
        return messageRepository.save(message);
    }

    public Message sendMediaMessage(Long senderId, Long receiverId, MultipartFile file, Message.MessageType type) throws IOException {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        if (!friendService.areFriends(senderId, receiverId)) {
            throw new RuntimeException("Users are not friends");
        }

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(file.getOriginalFilename());
        message.setType(type);
        message.setMediaData(file.getBytes());
        message.setMediaType(file.getContentType());
        message.setFileName(file.getOriginalFilename());
        return messageRepository.save(message);
    }

    public List<MessageDTO> getConversation(Long userId1, Long userId2) {
        User user1 = userRepository.findById(userId1)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User user2 = userRepository.findById(userId2)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return messageRepository.findConversation(user1, user2)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public void markAsRead(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        message.setRead(true);
        messageRepository.save(message);
    }

    public void markConversationAsRead(Long userId1, Long userId2) {
        User user1 = userRepository.findById(userId1)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User user2 = userRepository.findById(userId2)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Message> messages = messageRepository.findConversation(user1, user2);
        messages.stream()
                .filter(m -> m.getReceiver().getId().equals(userId1) && !m.isRead())
                .forEach(m -> {
                    m.setRead(true);
                    messageRepository.save(m);
                });
    }

    public MessageDTO convertToDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderName(message.getSender().getName());
        dto.setSenderUsername(message.getSender().getUsername());
        dto.setReceiverId(message.getReceiver().getId());
        dto.setReceiverName(message.getReceiver().getName());
        dto.setContent(message.getContent());
        dto.setType(message.getType().name());
        if (message.getMediaData() != null) {
            dto.setMediaDataBase64(Base64Util.encode(message.getMediaData()));
            dto.setMediaType(message.getMediaType());
            dto.setFileName(message.getFileName());
        }
        dto.setRead(message.isRead());
        dto.setTimestamp(message.getTimestamp());
        return dto;
    }
}


