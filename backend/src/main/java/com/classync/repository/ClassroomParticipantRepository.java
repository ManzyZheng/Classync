package com.classync.repository;

import com.classync.entity.ClassroomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassroomParticipantRepository extends JpaRepository<ClassroomParticipant, Long> {
    
    Optional<ClassroomParticipant> findByClassroomIdAndUserId(Long classroomId, Long userId);
    
    // 查找用户参与的所有课堂，按最后访问时间倒序排列
    List<ClassroomParticipant> findByUserIdOrderByLastAccessedAtDesc(Long userId);
}

