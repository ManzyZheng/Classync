package com.classync.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "questions")
public class Question {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;
    
    @Column(nullable = false)
    private String type; // "CHOICE" 或 "ESSAY"
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "questions", columnDefinition = "TEXT")
    private String questions; // JSON字符串，存储测验的子问题
    
    @Column(name = "is_open")
    private Boolean isOpen = false;
    
    @Column(name = "is_finished")
    private Boolean isFinished = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isOpen == null) {
            isOpen = false;
        }
        if (isFinished == null) {
            isFinished = false;
        }
    }
}

