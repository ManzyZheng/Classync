package com.classync.service;

import com.classync.entity.Classroom;
import com.classync.entity.ClassroomParticipant;
import com.classync.entity.Question;
import com.classync.entity.QuestionOption;
import com.classync.repository.ClassroomRepository;
import com.classync.repository.ClassroomParticipantRepository;
import com.classync.repository.QuestionRepository;
import com.classync.repository.QuestionOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassroomService {
    
    private final ClassroomRepository classroomRepository;
    private final ClassroomParticipantRepository participantRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    
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
    
    /**
     * 复制课堂文件（PDF）
     */
    @Transactional
    public String copyClassroomFile(Long sourceClassroomId) {
        Optional<Classroom> sourceOpt = classroomRepository.findById(sourceClassroomId);
        if (sourceOpt.isEmpty() || sourceOpt.get().getPdfPath() == null) {
            return null;
        }
        // 返回原始文件路径，实际复制在前端上传时处理
        return sourceOpt.get().getPdfPath();
    }
    
    /**
     * 复制课堂的所有问题（不含提交结果）
     */
    @Transactional
    public void copyQuestionsToClassroom(Long sourceClassroomId, Long targetClassroomId) {
        List<Question> sourceQuestions = questionRepository.findByClassroomIdOrderByCreatedAtAsc(sourceClassroomId);
        
        for (Question sourceQuestion : sourceQuestions) {
            // 创建新问题
            Question newQuestion = new Question();
            newQuestion.setClassroomId(targetClassroomId);
            newQuestion.setType(sourceQuestion.getType());
            newQuestion.setContent(sourceQuestion.getContent());
            newQuestion.setQuestions(sourceQuestion.getQuestions()); // 复制测验子问题
            newQuestion.setIsOpen(false); // 新问题默认关闭
            newQuestion.setIsFinished(false); // 新问题默认未完成
            
            Question savedQuestion = questionRepository.save(newQuestion);
            
            // 复制问题选项
            List<QuestionOption> sourceOptions = questionOptionRepository.findByQuestionIdOrderByOptionOrderAsc(sourceQuestion.getId());
            for (int i = 0; i < sourceOptions.size(); i++) {
                QuestionOption sourceOption = sourceOptions.get(i);
                QuestionOption newOption = new QuestionOption();
                newOption.setQuestionId(savedQuestion.getId());
                newOption.setContent(sourceOption.getContent());
                newOption.setIsCorrect(sourceOption.getIsCorrect());
                newOption.setOptionOrder(i);
                questionOptionRepository.save(newOption);
            }
        }
    }
    
    /**
     * 复制单个问题（包括选项）
     */
    @Transactional
    public Question copyQuestion(Long sourceQuestionId, Long targetClassroomId) {
        Optional<Question> sourceOpt = questionRepository.findById(sourceQuestionId);
        if (sourceOpt.isEmpty()) {
            return null;
        }
        
        Question sourceQuestion = sourceOpt.get();
        
        // 创建新问题
        Question newQuestion = new Question();
        newQuestion.setClassroomId(targetClassroomId);
        newQuestion.setType(sourceQuestion.getType());
        newQuestion.setContent(sourceQuestion.getContent());
        newQuestion.setQuestions(sourceQuestion.getQuestions());
        newQuestion.setIsOpen(false);
        newQuestion.setIsFinished(false);
        
        Question savedQuestion = questionRepository.save(newQuestion);
        
        // 复制问题选项
        List<QuestionOption> sourceOptions = questionOptionRepository.findByQuestionIdOrderByOptionOrderAsc(sourceQuestionId);
        for (int i = 0; i < sourceOptions.size(); i++) {
            QuestionOption sourceOption = sourceOptions.get(i);
            QuestionOption newOption = new QuestionOption();
            newOption.setQuestionId(savedQuestion.getId());
            newOption.setContent(sourceOption.getContent());
            newOption.setIsCorrect(sourceOption.getIsCorrect());
            newOption.setOptionOrder(i);
            questionOptionRepository.save(newOption);
        }
        
        return savedQuestion;
    }
    
    /**
     * 复制整个课堂（除了时间和提交结果）
     */
    @Transactional
    public Classroom copyEntireClassroom(Long sourceClassroomId, String newName, LocalDateTime newStartTime, LocalDateTime newEndTime, Long hostUserId) {
        Optional<Classroom> sourceOpt = classroomRepository.findById(sourceClassroomId);
        if (sourceOpt.isEmpty()) {
            return null;
        }
        
        Classroom source = sourceOpt.get();
        
        // 创建新课堂
        Classroom newClassroom = new Classroom();
        newClassroom.setName(newName);
        newClassroom.setStartTime(newStartTime);
        newClassroom.setEndTime(newEndTime);
        newClassroom.setHostUserId(hostUserId);
        newClassroom.setPdfPath(source.getPdfPath()); // 复制文件路径（实际文件需要单独复制）
        newClassroom.setClassCode(generateClassCode());
        newClassroom.setCurrentPage(1);
        
        Classroom savedClassroom = classroomRepository.save(newClassroom);
        
        // 复制所有问题
        copyQuestionsToClassroom(sourceClassroomId, savedClassroom.getId());
        
        return savedClassroom;
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

