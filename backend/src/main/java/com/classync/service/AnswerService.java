package com.classync.service;

import com.classync.dto.AnswerStatisticsDTO;
import com.classync.entity.Answer;
import com.classync.entity.QuestionOption;
import com.classync.repository.AnswerRepository;
import com.classync.repository.QuestionOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AnswerService {
    
    private final AnswerRepository answerRepository;
    private final QuestionOptionRepository questionOptionRepository;
    
    public Answer submitAnswer(Answer answer) {
        // 检查用户是否已经回答过：所有题型统一只允许提交一次
        Optional<Answer> existingAnswer = answerRepository.findByQuestionIdAndUserId(
            answer.getQuestionId(), answer.getUserId());
        
        if (existingAnswer.isPresent()) {
            // 已经存在答案，直接返回第一次的答案，不再新增记录
            return existingAnswer.get();
        }
        
        return answerRepository.save(answer);
    }
    
    public List<Answer> getAnswersByQuestionId(Long questionId) {
        return answerRepository.findByQuestionId(questionId);
    }
    
    public List<AnswerStatisticsDTO> getAnswerStatistics(Long questionId) {
        List<Answer> answers = answerRepository.findByQuestionId(questionId);
        List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByOptionOrderAsc(questionId);
        
        Map<String, Long> countMap = new HashMap<>();
        for (Answer answer : answers) {
            countMap.put(answer.getContent(), countMap.getOrDefault(answer.getContent(), 0L) + 1);
        }
        
        List<AnswerStatisticsDTO> statistics = new ArrayList<>();
        long totalAnswers = answers.size();
        
        for (QuestionOption option : options) {
            AnswerStatisticsDTO stat = new AnswerStatisticsDTO();
            stat.setOptionContent(option.getContent());
            long count = countMap.getOrDefault(option.getContent(), 0L);
            stat.setCount(count);
            stat.setPercentage(totalAnswers > 0 ? (count * 100.0 / totalAnswers) : 0.0);
            stat.setIsCorrect(option.getIsCorrect());
            statistics.add(stat);
        }
        
        return statistics;
    }
    
    public List<String> getEssayAnswers(Long questionId) {
        List<Answer> answers = answerRepository.findByQuestionId(questionId);
        List<String> essayAnswers = new ArrayList<>();
        for (Answer answer : answers) {
            essayAnswers.add(answer.getContent());
        }
        return essayAnswers;
    }
    
    // ✅ 返回完整的Answer对象（包含时间戳等信息）
    public List<Answer> getEssayAnswersFull(Long questionId) {
        return answerRepository.findByQuestionId(questionId);
    }
}

