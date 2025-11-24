package com.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String name;
    private String phone;
    private boolean online;
    private String status;
    private String theme;
    private String profilePictureBase64;
    private String profilePictureType;
}


