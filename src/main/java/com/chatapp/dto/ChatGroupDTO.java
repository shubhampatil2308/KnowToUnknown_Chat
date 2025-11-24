package com.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatGroupDTO {
    private Long id;
    private String name;
    private String description;
    private Long createdById;
    private String createdByName;
    private String groupImageBase64;
    private String groupImageType;
    private LocalDateTime createdAt;
    private List<UserDTO> members;
}


