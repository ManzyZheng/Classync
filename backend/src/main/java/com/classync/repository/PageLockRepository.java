package com.classync.repository;

import com.classync.entity.PageLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageLockRepository extends JpaRepository<PageLock, Long> {
    
    List<PageLock> findByClassroomId(Long classroomId);
    
    Optional<PageLock> findByClassroomIdAndPageNumber(Long classroomId, Integer pageNumber);
    
    void deleteByClassroomId(Long classroomId);
    
    /**
     * 批量解锁从指定页面开始的所有页面
     */
    @Modifying
    @Query("UPDATE PageLock p SET p.isLocked = false WHERE p.classroomId = :classroomId AND p.pageNumber >= :fromPage")
    int unlockFromPage(@Param("classroomId") Long classroomId, @Param("fromPage") Integer fromPage);
    
    /**
     * 批量解锁所有页面
     */
    @Modifying
    @Query("UPDATE PageLock p SET p.isLocked = false WHERE p.classroomId = :classroomId")
    int unlockAllPages(@Param("classroomId") Long classroomId);
}

