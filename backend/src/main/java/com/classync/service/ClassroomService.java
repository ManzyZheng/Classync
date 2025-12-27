package com.classync.service;

import com.classync.entity.Classroom;
import com.classync.entity.ClassroomParticipant;
import com.classync.repository.ClassroomRepository;
import com.classync.repository.ClassroomParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassroomService {
    
    private final ClassroomRepository classroomRepository;
    private final ClassroomParticipantRepository participantRepository;
    
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
    
    public List<Classroom> getClassroomsByParticipantUserId(Long userId) {
        List<ClassroomParticipant> participants = participantRepository.findByUserIdOrderByLastAccessedAtDesc(userId);
        return participants.stream()
                .map(participant -> classroomRepository.findById(participant.getClassroomId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
    
    public void recordParticipant(Long classroomId, Long userId) {
        Optional<ClassroomParticipant> existing = participantRepository.findByClassroomIdAndUserId(classroomId, userId);
        if (existing.isPresent()) {
            // 更新最后访问时间
            ClassroomParticipant participant = existing.get();
            participant.setLastAccessedAt(java.time.LocalDateTime.now());
            participantRepository.save(participant);
        } else {
            // 创建新记录
            ClassroomParticipant participant = new ClassroomParticipant();
            participant.setClassroomId(classroomId);
            participant.setUserId(userId);
            participantRepository.save(participant);
        }
    }
    
    public void removeParticipant(Long classroomId, Long userId) {
        Optional<ClassroomParticipant> participant = participantRepository.findByClassroomIdAndUserId(classroomId, userId);
        participant.ifPresent(participantRepository::delete);
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
    
    public void deleteClassroom(Long classroomId) {
        classroomRepository.deleteById(classroomId);
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

