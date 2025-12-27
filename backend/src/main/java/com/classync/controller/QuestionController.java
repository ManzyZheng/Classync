package com.classync.controller;

import com.classync.dto.OptionRequest;
import com.classync.dto.QuestionRequest;
import com.classync.dto.QuestionWithOptionsDTO;
import com.classync.entity.Question;
import com.classync.entity.QuestionOption;
import com.classync.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping("/classroom/{classroomId}")
    public ResponseEntity<List<Question>> getQuestionsByClassroomId(@PathVariable Long classroomId) {
        List<Question> questions = questionService.getQuestionsByClassroomId(classroomId);
        return ResponseEntity.ok(questions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionWithOptionsDTO> getQuestionById(@PathVariable Long id) {
        QuestionWithOptionsDTO question = questionService.getQuestionWithOptions(id);
        if (question == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(question);
    }

    @PostMapping
    public ResponseEntity<Question> createQuestion(@RequestBody QuestionRequest request) {
        Question question = new Question();
        question.setClassroomId(request.getClassroomId());
        question.setType(request.getType());
        question.setContent(request.getContent());
        // 设置默认状态为关闭
        question.setIsOpen(request.getIsOpen() != null ? request.getIsOpen() : false);
        question.setIsFinished(request.getIsFinished() != null ? request.getIsFinished() : false);

        List<QuestionOption> options = new ArrayList<>();
        if (request.getOptions() != null) {
            for (OptionRequest optReq : request.getOptions()) {
                QuestionOption option = new QuestionOption();
                option.setContent(optReq.getContent());
                option.setIsCorrect(optReq.getIsCorrect());
                options.add(option);
            }
        }

        Question created = questionService.createQuestion(question, options);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Question> updateQuestion(@PathVariable Long id, @RequestBody QuestionRequest request) {
        Question question = new Question();
        question.setType(request.getType());
        question.setContent(request.getContent());
        // 保留isOpen和isFinished状态
        if (request.getIsOpen() != null) {
            question.setIsOpen(request.getIsOpen());
        }
        if (request.getIsFinished() != null) {
            question.setIsFinished(request.getIsFinished());
        }

        List<QuestionOption> options = new ArrayList<>();
        if (request.getOptions() != null) {
            for (OptionRequest optReq : request.getOptions()) {
                QuestionOption option = new QuestionOption();
                option.setContent(optReq.getContent());
                option.setIsCorrect(optReq.getIsCorrect());
                options.add(option);
            }
        }

        Question updated = questionService.updateQuestion(id, question, options);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<Void> toggleQuestionOpen(@PathVariable Long id) {
        questionService.toggleQuestionOpen(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/finish")
    public ResponseEntity<Void> finishQuestion(@PathVariable Long id) {
        questionService.finishQuestion(id);
        return ResponseEntity.ok().build();
    }
}
