package com.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestDTO {
    private Long id;
    private Long senderId;
    private String senderName;
    private String senderUsername;
    private String senderEmail;
    private Long receiverId;
    private String receiverName;
    private String receiverUsername;
    private String status;
    private LocalDateTime createdAt;
}


