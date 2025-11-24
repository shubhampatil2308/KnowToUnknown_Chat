package com.chatapp.controller;

import com.chatapp.dto.MessageDTO;
import com.chatapp.entity.Message;
import com.chatapp.service.ChatService;
import com.chatapp.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class WebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatService chatService;

    @Autowired
    private GroupService groupService;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload Map<String, Object> payload) {
        try {
            Long senderId = Long.valueOf(payload.get("senderId").toString());
            Long receiverId = Long.valueOf(payload.get("receiverId").toString());
            String content = payload.get("content").toString();
            String type = payload.getOrDefault("type", "TEXT").toString();

            Message message = chatService.sendMessage(senderId, receiverId, content, 
                Message.MessageType.valueOf(type));
            MessageDTO messageDTO = chatService.convertToDTO(message);

            // Send to receiver
            messagingTemplate.convertAndSendToUser(
                receiverId.toString(), 
                "/queue/messages", 
                messageDTO
            );

            // Send confirmation to sender
            messagingTemplate.convertAndSendToUser(
                senderId.toString(), 
                "/queue/messages", 
                messageDTO
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @MessageMapping("/chat.sendGroupMessage")
    public void sendGroupMessage(@Payload Map<String, Object> payload) {
        try {
            Long groupId = Long.valueOf(payload.get("groupId").toString());
            Long senderId = Long.valueOf(payload.get("senderId").toString());
            String content = payload.get("content").toString();
            String type = payload.getOrDefault("type", "TEXT").toString();

            groupService.sendGroupMessage(groupId, senderId, content, 
                com.chatapp.entity.GroupMessage.MessageType.valueOf(type));
            
            // Reload messages and broadcast to all group members
            var messages = groupService.getGroupMessages(groupId);
            messagingTemplate.convertAndSend("/topic/group/" + groupId, messages);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload Map<String, Object> payload) {
        Long senderId = Long.valueOf(payload.get("senderId").toString());
        Long receiverId = Long.valueOf(payload.get("receiverId").toString());
        boolean isTyping = Boolean.parseBoolean(payload.get("isTyping").toString());

        Map<String, Object> typingInfo = Map.of(
            "senderId", senderId,
            "isTyping", isTyping
        );

        messagingTemplate.convertAndSendToUser(
            receiverId.toString(), 
            "/queue/typing", 
            typingInfo
        );
    }
}

