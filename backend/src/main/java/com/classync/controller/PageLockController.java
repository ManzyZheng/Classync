package com.classync.controller;

import com.classync.entity.PageLock;
import com.classync.service.PageLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/page-locks")
@RequiredArgsConstructor
public class PageLockController {

    private final PageLockService pageLockService;

    /**
     * 获取课堂的所有页面锁定状态
     */
    @GetMapping("/classroom/{classroomId}")
    public ResponseEntity<Map<Integer, Boolean>> getPageLocks(@PathVariable Long classroomId) {
        Map<Integer, Boolean> locks = pageLockService.getPageLocksByClassroom(classroomId);
        return ResponseEntity.ok(locks);
    }

    /**
     * 切换单个页面的锁定状态
     */
    @PostMapping("/toggle")
    public ResponseEntity<PageLock> togglePageLock(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("[PageLockController] Toggle request: " + request);

            Long classroomId = Long.valueOf(request.get("classroomId").toString());
            Integer pageNumber = Integer.valueOf(request.get("pageNumber").toString());

            System.out.println(
                    "[PageLockController] Toggling lock for classroom " + classroomId + ", page " + pageNumber);

            PageLock lock = pageLockService.togglePageLock(classroomId, pageNumber);

            System.out.println("[PageLockController] Lock toggled successfully: " + lock);

            return ResponseEntity.ok(lock);
        } catch (Exception e) {
            System.err.println("[PageLockController] Error toggling page lock: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 批量锁定从指定页面开始的所有后续页面
     */
    @PostMapping("/lock-from")
    public ResponseEntity<Void> lockPagesFrom(@RequestBody Map<String, Object> request) {
        Long classroomId = Long.valueOf(request.get("classroomId").toString());
        Integer fromPage = Integer.valueOf(request.get("fromPage").toString());
        Integer totalPages = Integer.valueOf(request.get("totalPages").toString());

        pageLockService.lockPagesFrom(classroomId, fromPage, totalPages);
        return ResponseEntity.ok().build();
    }

    /**
     * 批量解锁从指定页面开始的所有页面（一次 SQL）
     */
    @PostMapping("/unlock-from")
    public ResponseEntity<Map<String, Object>> unlockPagesFrom(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("[PageLockController] Unlock from request: " + request);

            Long classroomId = Long.valueOf(request.get("classroomId").toString());
            Integer fromPage = Integer.valueOf(request.get("fromPage").toString());

            int count = pageLockService.unlockFromPage(classroomId, fromPage);

            System.out.println("[PageLockController] Successfully unlocked " + count + " pages");

            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            System.err.println("[PageLockController] Error unlocking pages: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 批量解锁所有页面（一次 SQL）
     */
    @PostMapping("/unlock-all")
    public ResponseEntity<Map<String, Object>> unlockAllPages(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("[PageLockController] Unlock all request: " + request);

            Long classroomId = Long.valueOf(request.get("classroomId").toString());

            int count = pageLockService.unlockAllPages(classroomId);

            System.out.println("[PageLockController] Successfully unlocked " + count + " pages");

            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            System.err.println("[PageLockController] Error unlocking all pages: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
