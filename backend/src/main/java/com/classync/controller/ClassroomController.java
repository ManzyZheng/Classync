package com.classync.controller;

import com.classync.dto.ClassroomRequest;
import com.classync.entity.Classroom;
import com.classync.service.ClassroomService;
import com.classync.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/classrooms")
@RequiredArgsConstructor
public class ClassroomController {
    
    private final ClassroomService classroomService;
    private final FileService fileService;
    
    @PostMapping
    public ResponseEntity<Classroom> createClassroom(@RequestBody ClassroomRequest request) {
        Classroom classroom = new Classroom();
        classroom.setName(request.getName());
        classroom.setStartTime(request.getStartTime());
        classroom.setEndTime(request.getEndTime());
        classroom.setHostUserId(request.getHostUserId());
        
        Classroom created = classroomService.createClassroom(classroom);
        return ResponseEntity.ok(created);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Classroom> getClassroomById(@PathVariable Long id) {
        return classroomService.getClassroomById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/code/{classCode}")
    public ResponseEntity<Classroom> getClassroomByCode(@PathVariable String classCode) {
        return classroomService.getClassroomByCode(classCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/host/{userId}")
    public ResponseEntity<List<Classroom>> getClassroomsByHostUserId(@PathVariable Long userId) {
        List<Classroom> classrooms = classroomService.getClassroomsByHostUserId(userId);
        return ResponseEntity.ok(classrooms);
    }
    
    @GetMapping("/participant/{userId}")
    public ResponseEntity<List<Classroom>> getClassroomsByParticipantUserId(@PathVariable Long userId) {
        List<Classroom> classrooms = classroomService.getClassroomsByParticipantUserId(userId);
        return ResponseEntity.ok(classrooms);
    }
    
    @PostMapping("/{classroomId}/participant/{userId}")
    public ResponseEntity<Void> recordParticipant(@PathVariable Long classroomId, @PathVariable Long userId) {
        classroomService.recordParticipant(classroomId, userId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{classroomId}/participant/{userId}")
    public ResponseEntity<Void> removeParticipant(@PathVariable Long classroomId, @PathVariable Long userId) {
        classroomService.removeParticipant(classroomId, userId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClassroom(@PathVariable Long id) {
        classroomService.deleteClassroom(id);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{id}/time")
    public ResponseEntity<Classroom> updateClassroomTime(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            Classroom classroom = classroomService.getClassroomById(id).orElse(null);
            if (classroom == null) {
                return ResponseEntity.notFound().build();
            }
            
            String startTime = request.get("startTime");
            String endTime = request.get("endTime");
            
            if (startTime != null) {
                classroom.setStartTime(java.time.LocalDateTime.parse(startTime, 
                    java.time.format.DateTimeFormatter.ISO_DATE_TIME));
            }
            if (endTime != null) {
                classroom.setEndTime(java.time.LocalDateTime.parse(endTime, 
                    java.time.format.DateTimeFormatter.ISO_DATE_TIME));
            }
            
            Classroom updated = classroomService.updateClassroom(classroom);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{id}/upload")
    public ResponseEntity<Classroom> uploadPdf(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            String filename = fileService.uploadFile(file);
            Classroom classroom = classroomService.getClassroomById(id).orElse(null);
            if (classroom == null) {
                return ResponseEntity.notFound().build();
            }
            classroom.setPdfPath(filename);
            Classroom updated = classroomService.updateClassroom(classroom);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 设置放映页展示问题
     */
    @PostMapping("/{id}/display-question")
    public ResponseEntity<Classroom> setDisplayQuestion(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            Classroom classroom = classroomService.getClassroomById(id).orElse(null);
            if (classroom == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 从请求中获取 questionId 和 mode
            Object questionIdObj = request.get("questionId");
            String mode = (String) request.get("mode");
            
            if (questionIdObj == null) {
                // 清除展示状态
                classroom.setDisplayQuestionId(null);
                classroom.setDisplayQuestionMode(null);
            } else {
                // 设置展示状态
                Long questionId = questionIdObj instanceof Integer 
                    ? ((Integer) questionIdObj).longValue() 
                    : (Long) questionIdObj;
                classroom.setDisplayQuestionId(questionId);
                classroom.setDisplayQuestionMode(mode);
            }
            
            Classroom updated = classroomService.updateClassroom(classroom);
            System.out.println("[ClassroomController] Display question updated: questionId=" 
                + updated.getDisplayQuestionId() + ", mode=" + updated.getDisplayQuestionMode());
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 复制课堂文件路径信息
     */
    @GetMapping("/{id}/copy-file")
    public ResponseEntity<Map<String, String>> getFileToCopy(@PathVariable Long id) {
        String pdfPath = classroomService.copyClassroomFile(id);
        if (pdfPath == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("pdfPath", pdfPath));
    }
    
    /**
     * 复制课堂的所有问题到新课堂
     */
    @PostMapping("/{sourceId}/copy-questions-to/{targetId}")
    public ResponseEntity<Void> copyQuestionsToClassroom(
            @PathVariable Long sourceId,
            @PathVariable Long targetId) {
        try {
            classroomService.copyQuestionsToClassroom(sourceId, targetId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 复制整个课堂
     */
    @PostMapping("/{sourceId}/copy")
    public ResponseEntity<Classroom> copyEntireClassroom(
            @PathVariable Long sourceId,
            @RequestBody ClassroomRequest request) {
        try {
            Classroom copied = classroomService.copyEntireClassroom(
                sourceId,
                request.getName(),
                request.getStartTime(),
                request.getEndTime(),
                request.getHostUserId()
            );
            if (copied == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(copied);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
}

