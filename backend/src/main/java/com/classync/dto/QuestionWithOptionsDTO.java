package com.classync.dto;

import com.classync.entity.QuestionOption;
import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Data;

import java.util.List;

@Data
public class QuestionWithOptionsDTO {
    private Long id;
    private Long classroomId;
    private String type;
    private String content;
    private String questions; // JSON字符串，存储测验的子问题
    private Boolean isOpen;
    private Boolean isFinished;
    private List<QuestionOption> options;
}

