package com.poly.asm.daos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.poly.asm.entitys.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByUserIdOrderByCreatedAtAsc(Integer userId);

    List<ChatMessage> findByUserIdAndIdGreaterThanOrderByCreatedAtAsc(Integer userId, Long id);

    @Query("SELECT m.user.id FROM ChatMessage m GROUP BY m.user.id ORDER BY MAX(m.id) DESC")
    List<Integer> findConversationUserIds();

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.user.id = :userId AND m.senderRole = :senderRole AND m.isRead = false")
    long countUnread(@Param("userId") Integer userId, @Param("senderRole") String senderRole);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.user.id = :userId AND m.senderRole = :senderRole")
    void markRead(@Param("userId") Integer userId, @Param("senderRole") String senderRole);
}
