package com.classync.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuestionRequest {
    private Long classroomId;
    private String type;
    private String content;
    private List<OptionRequest> options;
    private List<Object> questions; // 测验的子问题列表
    private Boolean isOpen;
    private Boolean isFinished;
}

