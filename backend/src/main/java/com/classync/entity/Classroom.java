package com.classync.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "classrooms")
public class Classroom {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;
    
    @Column(name = "class_code", nullable = false, unique = true, length = 10)
    private String classCode;
    
    @Column(name = "pdf_path")
    private String pdfPath;
    
    @Column(name = "host_user_id", nullable = false)
    private Long hostUserId;
    
    @Column(name = "current_page")
    private Integer currentPage = 1;
    
    // 问题展示状态
    @Column(name = "display_question_id")
    private Long displayQuestionId;  // 当前展示的问题ID，null表示不展示问题
    
    @Column(name = "display_question_mode", length = 30)
    private String displayQuestionMode;  // QUESTION_ONLY 或 QUESTION_WITH_RESULT
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (currentPage == null) {
            currentPage = 1;
        }
    }
}

