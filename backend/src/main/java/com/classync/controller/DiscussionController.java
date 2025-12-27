package com.classync.controller;

import com.classync.dto.DiscussionRequest;
import com.classync.dto.DiscussionWithUserDTO;
import com.classync.entity.Discussion;
import com.classync.service.DiscussionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/discussions")
@RequiredArgsConstructor
public class DiscussionController {
    
    private final DiscussionService discussionService;
    
    @GetMapping("/classroom/{classroomId}")
    public ResponseEntity<List<DiscussionWithUserDTO>> getDiscussionsByClassroomId(@PathVariable Long classroomId) {
        List<DiscussionWithUserDTO> discussions = discussionService.getDiscussionsByClassroomId(classroomId);
        return ResponseEntity.ok(discussions);
    }
    
    @PostMapping
    public ResponseEntity<Discussion> createDiscussion(@RequestBody DiscussionRequest request) {
        Discussion discussion = new Discussion();
        discussion.setClassroomId(request.getClassroomId());
        discussion.setUserId(request.getUserId());
        discussion.setContent(request.getContent());
        discussion.setReplyToId(request.getReplyToId());
        discussion.setIsAnonymous(request.getIsAnonymous());
        
        Discussion created = discussionService.createDiscussion(discussion);
        return ResponseEntity.ok(created);
    }
}

