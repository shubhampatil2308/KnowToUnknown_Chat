package com.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private Long id;
    private Long senderId;
    private String senderName;
    private String senderUsername;
    private Long receiverId;
    private String receiverName;
    private String content;
    private String type;
    private String mediaDataBase64;
    private String mediaType;
    private String fileName;
    private boolean isRead;
    private LocalDateTime timestamp;
}


