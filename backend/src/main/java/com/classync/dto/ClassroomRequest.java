package com.classync.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClassroomRequest {
    private String name;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long hostUserId;
}

