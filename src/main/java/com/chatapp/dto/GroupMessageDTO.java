package com.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupMessageDTO {
    private Long id;
    private Long groupId;
    private String groupName;
    private Long senderId;
    private String senderName;
    private String senderUsername;
    private String content;
    private String type;
    private String mediaDataBase64;
    private String mediaType;
    private String fileName;
    private LocalDateTime timestamp;
}


