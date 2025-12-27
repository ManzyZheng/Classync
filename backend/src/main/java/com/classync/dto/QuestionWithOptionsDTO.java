package com.classync.dto;

import com.classync.entity.QuestionOption;
import lombok.Data;

import java.util.List;

@Data
public class QuestionWithOptionsDTO {
    private Long id;
    private Long classroomId;
    private String type;
    private String content;
    private Boolean isOpen;
    private Boolean isFinished;
    private List<QuestionOption> options;
}

