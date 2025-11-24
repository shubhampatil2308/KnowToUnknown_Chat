package com.chatapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    private String phone;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] profilePicture;

    private String profilePictureType;

    @Column(nullable = false)
    private boolean online = false;

    private LocalDateTime lastSeen;

    private String theme = "light"; // "light" or "dark"

    private String status = "";

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastSeen = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastSeen = LocalDateTime.now();
    }
}

