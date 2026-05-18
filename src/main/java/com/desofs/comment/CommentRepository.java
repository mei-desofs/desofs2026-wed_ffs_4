package com.desofs.comment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c WHERE c.taskId = ?1 AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
    List<Comment> findByTaskId(UUID taskId);

    @Query("SELECT c FROM Comment c WHERE c.id = ?1 AND c.deletedAt IS NULL")
    Optional<Comment> findActiveById(Long id);
}