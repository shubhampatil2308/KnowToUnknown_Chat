package com.chatapp.repository;

import com.chatapp.entity.FriendRequest;
import com.chatapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
    List<FriendRequest> findByReceiverAndStatus(User receiver, FriendRequest.FriendRequestStatus status);
    List<FriendRequest> findBySenderAndStatus(User sender, FriendRequest.FriendRequestStatus status);
    Optional<FriendRequest> findBySenderAndReceiver(User sender, User receiver);
    boolean existsBySenderAndReceiver(User sender, User receiver);
    List<FriendRequest> findBySender(User sender);
    List<FriendRequest> findByReceiver(User receiver);
}


