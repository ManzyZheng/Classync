package com.classync.service;

import com.classync.dto.DiscussionWithUserDTO;
import com.classync.entity.Discussion;
import com.classync.entity.User;
import com.classync.repository.DiscussionRepository;
import com.classync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DiscussionService {
    
    private final DiscussionRepository discussionRepository;
    private final UserRepository userRepository;
    
    public Discussion createDiscussion(Discussion discussion) {
        return discussionRepository.save(discussion);
    }
    
    public List<DiscussionWithUserDTO> getDiscussionsByClassroomId(Long classroomId) {
        List<Discussion> discussions = discussionRepository.findByClassroomIdOrderByCreatedAtAsc(classroomId);
        List<DiscussionWithUserDTO> result = new ArrayList<>();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (Discussion discussion : discussions) {
            DiscussionWithUserDTO dto = new DiscussionWithUserDTO();
            dto.setId(discussion.getId());
            dto.setClassroomId(discussion.getClassroomId());
            dto.setUserId(discussion.getUserId());
            dto.setContent(discussion.getContent());
            dto.setReplyToId(discussion.getReplyToId());
            dto.setIsAnonymous(discussion.getIsAnonymous());
            dto.setCreatedAt(discussion.getCreatedAt().format(formatter));
            
            // 获取用户名
            if (discussion.getIsAnonymous()) {
                dto.setUserName("匿名用户");
            } else {
                Optional<User> userOpt = userRepository.findById(discussion.getUserId());
                dto.setUserName(userOpt.map(User::getName).orElse("未知用户"));
            }
            
            // 获取被回复者名称
            if (discussion.getReplyToId() != null) {
                Optional<Discussion> replyToOpt = discussionRepository.findById(discussion.getReplyToId());
                if (replyToOpt.isPresent()) {
                    Discussion replyTo = replyToOpt.get();
                    if (replyTo.getIsAnonymous()) {
                        dto.setReplyToName("匿名用户");
                    } else {
                        Optional<User> replyToUserOpt = userRepository.findById(replyTo.getUserId());
                        dto.setReplyToName(replyToUserOpt.map(User::getName).orElse("未知用户"));
                    }
                }
            }
            
            result.add(dto);
        }
        
        return result;
    }
}

