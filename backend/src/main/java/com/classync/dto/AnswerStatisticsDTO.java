package com.classync.dto;

import lombok.Data;

@Data
public class AnswerStatisticsDTO {
    private String optionContent;
    private Long count;
    private Double percentage;
    private Boolean isCorrect;
}

