package com.chatapp.repository;

import com.chatapp.entity.Message;
import com.chatapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query("SELECT m FROM Message m WHERE (m.sender = ?1 AND m.receiver = ?2) OR (m.sender = ?2 AND m.receiver = ?1) ORDER BY m.timestamp ASC")
    List<Message> findConversation(User user1, User user2);
    
    List<Message> findByReceiverAndIsReadFalse(User receiver);
    
    List<Message> findBySender(User sender);
    
    List<Message> findByReceiver(User receiver);
}


