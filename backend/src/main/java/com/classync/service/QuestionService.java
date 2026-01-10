package com.classync.service;

import com.classync.dto.QuestionWithOptionsDTO;
import com.classync.entity.Question;
import com.classync.entity.QuestionOption;
import com.classync.repository.AnswerRepository;
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
    private final AnswerRepository answerRepository;
    
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
        dto.setQuestions(question.getQuestions()); // 设置测验的子问题JSON字符串
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
        // 更新questions字段（测验的子问题）
        existing.setQuestions(question.getQuestions());
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
    
    @Transactional
    public void deleteQuestion(Long id) {
        // 先删除关联的答案
        answerRepository.deleteByQuestionId(id);
        // 再删除问题选项
        questionOptionRepository.deleteByQuestionId(id);
        // 最后删除问题本身
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
    
    /**
     * 复制单个问题（包括选项）到指定课堂
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
}

