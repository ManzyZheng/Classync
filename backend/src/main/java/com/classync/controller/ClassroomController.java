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
}

