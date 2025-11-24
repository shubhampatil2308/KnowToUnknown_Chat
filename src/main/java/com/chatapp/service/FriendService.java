package com.chatapp.service;

import com.chatapp.dto.FriendRequestDTO;
import com.chatapp.entity.FriendRequest;
import com.chatapp.entity.User;
import com.chatapp.repository.FriendRequestRepository;
import com.chatapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class FriendService {
    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private UserRepository userRepository;

    public FriendRequest sendFriendRequest(Long senderId, Long receiverId) {
        if (senderId.equals(receiverId)) {
            throw new RuntimeException("Cannot send friend request to yourself");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        if (friendRequestRepository.existsBySenderAndReceiver(sender, receiver) ||
            friendRequestRepository.existsBySenderAndReceiver(receiver, sender)) {
            throw new RuntimeException("Friend request already exists");
        }

        FriendRequest request = new FriendRequest();
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setStatus(FriendRequest.FriendRequestStatus.PENDING);
        return friendRequestRepository.save(request);
    }

    public FriendRequest acceptFriendRequest(Long requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));
        request.setStatus(FriendRequest.FriendRequestStatus.ACCEPTED);
        return friendRequestRepository.save(request);
    }

    public FriendRequest rejectFriendRequest(Long requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));
        request.setStatus(FriendRequest.FriendRequestStatus.REJECTED);
        return friendRequestRepository.save(request);
    }

    public List<FriendRequestDTO> getPendingRequests(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return friendRequestRepository.findByReceiverAndStatus(user, FriendRequest.FriendRequestStatus.PENDING)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<FriendRequestDTO> getSentRequests(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return friendRequestRepository.findBySenderAndStatus(user, FriendRequest.FriendRequestStatus.PENDING)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<FriendRequestDTO> getAcceptedFriends(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<FriendRequest> sent = friendRequestRepository.findBySenderAndStatus(user, FriendRequest.FriendRequestStatus.ACCEPTED);
        List<FriendRequest> received = friendRequestRepository.findByReceiverAndStatus(user, FriendRequest.FriendRequestStatus.ACCEPTED);
        
        // Combine sent and received accepted requests
        List<FriendRequest> allAccepted = new java.util.ArrayList<>(sent);
        allAccepted.addAll(received);
        
        return allAccepted.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public boolean areFriends(Long userId1, Long userId2) {
        User user1 = userRepository.findById(userId1)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User user2 = userRepository.findById(userId2)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return friendRequestRepository.findBySenderAndReceiver(user1, user2)
                .map(req -> req.getStatus() == FriendRequest.FriendRequestStatus.ACCEPTED)
                .orElse(false) ||
               friendRequestRepository.findBySenderAndReceiver(user2, user1)
                .map(req -> req.getStatus() == FriendRequest.FriendRequestStatus.ACCEPTED)
                .orElse(false);
    }

    public void removeFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new RuntimeException("Invalid friend selection");
        }
        User user1 = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User user2 = userRepository.findById(friendId)
                .orElseThrow(() -> new RuntimeException("Friend not found"));

        boolean removed = false;
        var direct = friendRequestRepository.findBySenderAndReceiver(user1, user2);
        if (direct.isPresent() && direct.get().getStatus() == FriendRequest.FriendRequestStatus.ACCEPTED) {
            friendRequestRepository.delete(direct.get());
            removed = true;
        }
        var reverse = friendRequestRepository.findBySenderAndReceiver(user2, user1);
        if (reverse.isPresent() && reverse.get().getStatus() == FriendRequest.FriendRequestStatus.ACCEPTED) {
            friendRequestRepository.delete(reverse.get());
            removed = true;
        }

        if (!removed) {
            throw new RuntimeException("You are not friends with this user");
        }
    }

    private FriendRequestDTO convertToDTO(FriendRequest request) {
        FriendRequestDTO dto = new FriendRequestDTO();
        dto.setId(request.getId());
        dto.setSenderId(request.getSender().getId());
        dto.setSenderName(request.getSender().getName());
        dto.setSenderUsername(request.getSender().getUsername());
        dto.setSenderEmail(request.getSender().getEmail());
        dto.setReceiverId(request.getReceiver().getId());
        dto.setReceiverName(request.getReceiver().getName());
        dto.setReceiverUsername(request.getReceiver().getUsername());
        dto.setStatus(request.getStatus().name());
        dto.setCreatedAt(request.getCreatedAt());
        return dto;
    }
}

