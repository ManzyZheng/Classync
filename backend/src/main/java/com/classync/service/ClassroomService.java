package com.classync.service;

import com.classync.entity.Classroom;
import com.classync.repository.ClassroomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class ClassroomService {
    
    private final ClassroomRepository classroomRepository;
    
    public Classroom createClassroom(Classroom classroom) {
        classroom.setClassCode(generateClassCode());
        return classroomRepository.save(classroom);
    }
    
    public Optional<Classroom> getClassroomById(Long id) {
        return classroomRepository.findById(id);
    }
    
    public Optional<Classroom> getClassroomByCode(String classCode) {
        return classroomRepository.findByClassCode(classCode);
    }
    
    public List<Classroom> getClassroomsByHostUserId(Long hostUserId) {
        return classroomRepository.findByHostUserId(hostUserId);
    }
    
    public Classroom updateClassroom(Classroom classroom) {
        return classroomRepository.save(classroom);
    }
    
    public void updateCurrentPage(Long classroomId, Integer pageNumber) {
        Optional<Classroom> classroomOpt = classroomRepository.findById(classroomId);
        if (classroomOpt.isPresent()) {
            Classroom classroom = classroomOpt.get();
            classroom.setCurrentPage(pageNumber);
            classroomRepository.save(classroom);
        }
    }
    
    private String generateClassCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        // 检查是否已存在
        if (classroomRepository.findByClassCode(code.toString()).isPresent()) {
            return generateClassCode();
        }
        return code.toString();
    }
}

