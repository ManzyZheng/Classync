package com.classync.dto;

import lombok.Data;

@Data
public class DiscussionRequest {
    private Long classroomId;
    private Long userId;
    private String content;
    private Long replyToId;
    private Boolean isAnonymous;
}

