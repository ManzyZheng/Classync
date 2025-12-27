package com.classync.dto;

import com.classync.entity.User;
import lombok.Data;

@Data
public class DiscussionWithUserDTO {
    private Long id;
    private Long classroomId;
    private Long userId;
    private String userName;
    private String content;
    private Long replyToId;
    private String replyToName;
    private Boolean isAnonymous;
    private String createdAt;
}

