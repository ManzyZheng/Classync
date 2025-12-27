package com.classync.repository;

import com.classync.entity.Discussion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscussionRepository extends JpaRepository<Discussion, Long> {
    List<Discussion> findByClassroomIdOrderByCreatedAtAsc(Long classroomId);
}

