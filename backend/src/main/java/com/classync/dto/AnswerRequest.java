package com.classync.dto;

import lombok.Data;

@Data
public class AnswerRequest {
    private Long questionId;
    private Long userId;
    private String content;
}

