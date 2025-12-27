package com.classync.service;

import com.classync.dto.QuestionWithOptionsDTO;
import com.classync.entity.Question;
import com.classync.entity.QuestionOption;
import com.classync.repository.QuestionOptionRepository;
import com.classync.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QuestionService {
    
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    
    public List<Question> getQuestionsByClassroomId(Long classroomId) {
        return questionRepository.findByClassroomIdOrderByCreatedAtAsc(classroomId);
    }
    
    public Optional<Question> getQuestionById(Long id) {
        return questionRepository.findById(id);
    }
    
    public QuestionWithOptionsDTO getQuestionWithOptions(Long id) {
        Optional<Question> questionOpt = questionRepository.findById(id);
        if (questionOpt.isEmpty()) {
            return null;
        }
        
        Question question = questionOpt.get();
        List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByOptionOrderAsc(id);
        
        QuestionWithOptionsDTO dto = new QuestionWithOptionsDTO();
        dto.setId(question.getId());
        dto.setClassroomId(question.getClassroomId());
        dto.setType(question.getType());
        dto.setContent(question.getContent());
        dto.setIsOpen(question.getIsOpen());
        dto.setIsFinished(question.getIsFinished());
        dto.setOptions(options);
        
        return dto;
    }
    
    @Transactional
    public Question createQuestion(Question question, List<QuestionOption> options) {
        Question savedQuestion = questionRepository.save(question);
        
        if (options != null && !options.isEmpty()) {
            for (int i = 0; i < options.size(); i++) {
                QuestionOption option = options.get(i);
                option.setQuestionId(savedQuestion.getId());
                option.setOptionOrder(i);
                questionOptionRepository.save(option);
            }
        }
        
        return savedQuestion;
    }
    
    @Transactional
    public Question updateQuestion(Long id, Question question, List<QuestionOption> options) {
        Optional<Question> existingOpt = questionRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return null;
        }
        
        Question existing = existingOpt.get();
        existing.setContent(question.getContent());
        existing.setType(question.getType());
        // 保留isOpen和isFinished状态，如果传入了新值则更新
        if (question.getIsOpen() != null) {
            existing.setIsOpen(question.getIsOpen());
        }
        if (question.getIsFinished() != null) {
            existing.setIsFinished(question.getIsFinished());
        }
        Question updated = questionRepository.save(existing);
        
        // 删除旧选项，添加新选项
        questionOptionRepository.deleteByQuestionId(id);
        
        if (options != null && !options.isEmpty()) {
            for (int i = 0; i < options.size(); i++) {
                QuestionOption option = options.get(i);
                option.setQuestionId(id);
                option.setOptionOrder(i);
                questionOptionRepository.save(option);
            }
        }
        
        return updated;
    }
    
    public void deleteQuestion(Long id) {
        questionOptionRepository.deleteByQuestionId(id);
        questionRepository.deleteById(id);
    }
    
    public Question toggleQuestionOpen(Long id) {
        Optional<Question> questionOpt = questionRepository.findById(id);
        if (questionOpt.isPresent()) {
            Question question = questionOpt.get();
            question.setIsOpen(!question.getIsOpen());
            Question saved = questionRepository.save(question);
            System.out.println("Toggle question " + id + " to isOpen=" + saved.getIsOpen());
            return saved;
        }
        return null;
    }
    
    public void finishQuestion(Long id) {
        Optional<Question> questionOpt = questionRepository.findById(id);
        if (questionOpt.isPresent()) {
            Question question = questionOpt.get();
            question.setIsFinished(true);
            questionRepository.save(question);
        }
    }
}

