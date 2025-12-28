package com.classync.service;

import com.classync.entity.PageLock;
import com.classync.repository.PageLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PageLockService {

    private final PageLockRepository pageLockRepository;

    /**
     * 获取课堂的所有页面锁定状态
     */
    public Map<Integer, Boolean> getPageLocksByClassroom(Long classroomId) {
        List<PageLock> locks = pageLockRepository.findByClassroomId(classroomId);
        return locks.stream()
                .collect(Collectors.toMap(
                        PageLock::getPageNumber,
                        PageLock::getIsLocked));
    }

    /**
     * 切换单个页面的锁定状态
     */
    @Transactional
    public PageLock togglePageLock(Long classroomId, Integer pageNumber) {
        System.out.println("[PageLockService] Toggling lock for classroom=" + classroomId + ", page=" + pageNumber);

        PageLock lock = pageLockRepository
                .findByClassroomIdAndPageNumber(classroomId, pageNumber)
                .orElseGet(() -> {
                    System.out.println("[PageLockService] Creating new PageLock record");
                    PageLock newLock = new PageLock();
                    newLock.setClassroomId(classroomId);
                    newLock.setPageNumber(pageNumber);
                    newLock.setIsLocked(false);
                    System.out.println("[PageLockService] New lock created: " + newLock);
                    return newLock;
                });

        System.out.println("[PageLockService] Current lock state: " + lock);
        System.out.println("[PageLockService] Current isLocked value: " + lock.getIsLocked());

        Boolean newState = !lock.getIsLocked();
        System.out.println("[PageLockService] Setting new state to: " + newState);
        lock.setIsLocked(newState);

        System.out.println("[PageLockService] Saving lock: " + lock);
        PageLock saved = pageLockRepository.save(lock);
        System.out.println("[PageLockService] Lock saved successfully: " + saved);

        return saved;
    }

    /**
     * 设置单个页面的锁定状态
     */
    @Transactional
    public PageLock setPageLock(Long classroomId, Integer pageNumber, Boolean isLocked) {
        PageLock lock = pageLockRepository
                .findByClassroomIdAndPageNumber(classroomId, pageNumber)
                .orElseGet(() -> {
                    PageLock newLock = new PageLock();
                    newLock.setClassroomId(classroomId);
                    newLock.setPageNumber(pageNumber);
                    return newLock;
                });

        lock.setIsLocked(isLocked);
        return pageLockRepository.save(lock);
    }

    /**
     * 批量锁定从指定页面开始的所有后续页面
     */
    @Transactional
    public void lockPagesFrom(Long classroomId, Integer fromPage, Integer totalPages) {
        for (int page = fromPage; page <= totalPages; page++) {
            setPageLock(classroomId, page, true);
        }
    }

    /**
     * 检查页面是否被锁定
     */
    public boolean isPageLocked(Long classroomId, Integer pageNumber) {
        return pageLockRepository
                .findByClassroomIdAndPageNumber(classroomId, pageNumber)
                .map(PageLock::getIsLocked)
                .orElse(false);
    }

    /**
     * 批量解锁从指定页面开始的所有页面（一次 SQL）
     */
    @Transactional
    public int unlockFromPage(Long classroomId, Integer fromPage) {
        System.out.println("[PageLockService] Batch unlocking from page " + fromPage + " for classroom " + classroomId);
        int count = pageLockRepository.unlockFromPage(classroomId, fromPage);
        System.out.println("[PageLockService] Unlocked " + count + " pages");
        return count;
    }

    /**
     * 批量解锁所有页面（一次 SQL）
     */
    @Transactional
    public int unlockAllPages(Long classroomId) {
        System.out.println("[PageLockService] Batch unlocking all pages for classroom " + classroomId);
        int count = pageLockRepository.unlockAllPages(classroomId);
        System.out.println("[PageLockService] Unlocked " + count + " pages");
        return count;
    }
}
