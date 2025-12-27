package com.classync.controller;

import com.classync.dto.AnswerRequest;
import com.classync.dto.AnswerStatisticsDTO;
import com.classync.entity.Answer;
import com.classync.service.AnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/answers")
@RequiredArgsConstructor
public class AnswerController {
    
    private final AnswerService answerService;
    
    @PostMapping
    public ResponseEntity<Answer> submitAnswer(@RequestBody AnswerRequest request) {
        Answer answer = new Answer();
        answer.setQuestionId(request.getQuestionId());
        answer.setUserId(request.getUserId());
        answer.setContent(request.getContent());
        
        Answer created = answerService.submitAnswer(answer);
        return ResponseEntity.ok(created);
    }
    
    @GetMapping("/question/{questionId}")
    public ResponseEntity<List<Answer>> getAnswersByQuestionId(@PathVariable Long questionId) {
        List<Answer> answers = answerService.getAnswersByQuestionId(questionId);
        return ResponseEntity.ok(answers);
    }
    
    @GetMapping("/statistics/{questionId}")
    public ResponseEntity<List<AnswerStatisticsDTO>> getAnswerStatistics(@PathVariable Long questionId) {
        List<AnswerStatisticsDTO> statistics = answerService.getAnswerStatistics(questionId);
        return ResponseEntity.ok(statistics);
    }
    
    @GetMapping("/essay/{questionId}")
    public ResponseEntity<List<Answer>> getEssayAnswers(@PathVariable Long questionId) {
        List<Answer> answers = answerService.getEssayAnswersFull(questionId);
        return ResponseEntity.ok(answers);
    }
}

