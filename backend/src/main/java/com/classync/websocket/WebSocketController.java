package com.classync.websocket;

import com.classync.entity.Classroom;
import com.classync.entity.Question;
import com.classync.service.ClassroomService;
import com.classync.service.PageLockService;
import com.classync.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ClassroomService classroomService;
    private final QuestionService questionService;
    private final PageLockService pageLockService;

    // 事件常量
    private static final String EVENT_CLASSROOM_STATE = "CLASSROOM_STATE";
    private static final String EVENT_PAGE_UPDATE = "PAGE_UPDATE";
    private static final String EVENT_QUESTION_OPENED = "QUESTION_OPENED";
    private static final String EVENT_QUESTION_CLOSED = "QUESTION_CLOSED";
    private static final String EVENT_QUESTION_FINISHED = "QUESTION_FINISHED";
    private static final String EVENT_ANSWER_SUBMITTED = "ANSWER_SUBMITTED";
    private static final String EVENT_DISCUSSION_NEW = "DISCUSSION_NEW";
    private static final String EVENT_DISPLAY_CODE_TOGGLE = "DISPLAY_CODE_TOGGLE";
    private static final String EVENT_PAGE_LOCK_UPDATE = "PAGE_LOCK_UPDATE";
    private static final String EVENT_PAGE_LOCK_BATCH = "PAGE_LOCK_BATCH";
    private static final String EVENT_PAGE_LOCK_BATCH_UNLOCK = "PAGE_LOCK_BATCH_UNLOCK";
    private static final String EVENT_DISPLAY_QUESTION = "DISPLAY_QUESTION";

    @MessageMapping("/join_classroom")
    public void joinClassroom(@Payload Map<String, Object> request) {
        Long classroomId = Long.valueOf(request.get("classroomId").toString());

        // 获取课堂当前状态
        Classroom classroom = classroomService.getClassroomById(classroomId).orElse(null);
        if (classroom == null) {
            return;
        }

        List<Question> openQuestions = questionService.getQuestionsByClassroomId(classroomId)
                .stream()
                .filter(Question::getIsOpen)
                .toList();

        // 获取页面锁定状态
        Map<Integer, Boolean> pageLocks = pageLockService.getPageLocksByClassroom(classroomId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("currentPage", classroom.getCurrentPage());
        payload.put("openQuestions", openQuestions);
        payload.put("pageLocks", pageLocks);

        // 添加问题展示状态
        if (classroom.getDisplayQuestionId() != null) {
            payload.put("displayQuestionId", classroom.getDisplayQuestionId());
            payload.put("displayQuestionMode", classroom.getDisplayQuestionMode());
        }

        WebSocketMessage response = WebSocketMessage.of(
                EVENT_CLASSROOM_STATE,
                classroomId,
                payload);

        messagingTemplate.convertAndSend("/topic/classroom/" + classroomId, response);
    }

    @MessageMapping("/page_change")
    public void pageChange(@Payload Map<String, Object> request) {
        Long classroomId = Long.valueOf(request.get("classroomId").toString());
        Map<String, Object> data = (Map<String, Object>) request.get("data");
        Integer pageNumber = (Integer) data.get("pageNumber");

        // 更新课堂当前页码
        classroomService.updateCurrentPage(classroomId, pageNumber);

        // 广播页码更新（轻量级）
        WebSocketMessage response = WebSocketMessage.of(
                EVENT_PAGE_UPDATE,
                classroomId,
                Map.of("pageNumber", pageNumber));

        messagingTemplate.convertAndSend("/topic/classroom/" + classroomId, response);
    }

    @MessageMapping("/question_toggle")
    public void questionToggle(@Payload Map<String, Object> request) {
        Long classroomId = Long.valueOf(request.get("classroomId").toString());
        Map<String, Object> data = (Map<String, Object>) request.get("data");
        Long questionId = Long.valueOf(data.get("questionId").toString());

        // ✅ 只读取当前状态，不做toggle（HTTP API已经toggle了）
        Question question = questionService.getQuestionById(questionId).orElse(null);

        if (question == null) {
            System.err.println("Question not found: " + questionId);
            return;
        }

        // 根据当前状态发送对应事件
        String event = question.getIsOpen() ? EVENT_QUESTION_OPENED : EVENT_QUESTION_CLOSED;

        // 轻量级payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("questionId", questionId);
        payload.put("isOpen", question.getIsOpen());

        System.out.println(
                "Broadcasting " + event + " for question " + questionId + " with isOpen=" + question.getIsOpen());

        WebSocketMessage response = WebSocketMessage.of(event, classroomId, payload);

        messagingTemplate.convertAndSend("/topic/classroom/" + classroomId, response);
    }

    @MessageMapping("/question_finish")
    public void questionFinish(@Payload Map<String, Object> request) {
        Long classroomId = Long.valueOf(request.get("classroomId").toString());
        Map<String, Object> data = (Map<String, Object>) request.get("data");
        Long questionId = Long.valueOf(data.get("questionId").toString());

        questionService.finishQuestion(questionId);

        // 轻量级payload
        WebSocketMessage response = WebSocketMessage.of(
                EVENT_QUESTION_FINISHED,
                classroomId,
                Map.of("questionId", questionId));

        messagingTemplate.convertAndSend("/topic/classroom/" + classroomId, response);
    }

    @MessageMapping("/answer_submit")
    public void answerSubmit(@Payload Map<String, Object> request) {
        Long classroomId = Long.valueOf(request.get("classroomId").toString());
        Map<String, Object> data = (Map<String, Object>) request.get("data");

        // 广播答案更新（轻量级）
        WebSocketMessage response = WebSocketMessage.of(
                EVENT_ANSWER_SUBMITTED,
                classroomId,
                Map.of("questionId", data.get("questionId")));

        messagingTemplate.convertAndSend("/topic/classroom/" + classroomId, response);
    }

    @MessageMapping("/discussion_new")
    public void discussionNew(@Payload Map<String, Object> request) {
        Long classroomId = Long.valueOf(request.get("classroomId").toString());
        Map<String, Object> data = (Map<String, Object>) request.get("data");

        // 广播新讨论
        WebSocketMessage response = WebSocketMessage.of(
                EVENT_DISCUSSION_NEW,
                classroomId,
                data);

        messagingTemplate.convertAndSend("/topic/classroom/" + classroomId, response);
    }

    @MessageMapping("/display_code_toggle")
    public void displayCodeToggle(@Payload Map<String, Object> request) {
        Long classroomId = Long.valueOf(request.get("classroomId").toString());
        Map<String, Object> data = (Map<String, Object>) request.get("data");

        // 广播展示课堂码切换状态
        WebSocketMessage response = WebSocketMessage.of(
                EVENT_DISPLAY_CODE_TOGGLE,
                classroomId,
                data);

        messagingTemplate.convertAndSend("/topic/classroom/" + classroomId, response);
    }

    @MessageMapping("/page_lock_toggle")
    public void pageLockToggle(@Payload Map<String, Object> request) {
        Long classroomId = Long.valueOf(request.get("classroomId").toString());
        Map<String, Object> data = (Map<String, Object>) request.get("data");
        Integer pageNumber = Integer.valueOf(data.get("pageNumber").toString());
        Boolean isLocked = (Boolean) data.get("isLocked");

        // 广播页面锁定状态更新
        WebSocketMessage response = WebSocketMessage.of(
                EVENT_PAGE_LOCK_UPDATE,
                classroomId,
                Map.of("pageNumber", pageNumber, "isLocked", isLocked));

        messagingTemplate.convertAndSend("/topic/classroom/" + classroomId, response);
    }

    @MessageMapping("/page_lock_batch")
    public void pageLockBatch(@Payload Map<String, Object> request) {
        Long classroomId = Long.valueOf(request.get("classroomId").toString());
        Map<String, Object> data = (Map<String, Object>) request.get("data");

        // 广播批量锁定
        WebSocketMessage response = WebSocketMessage.of(
                EVENT_PAGE_LOCK_BATCH,
                classroomId,
                data);

        messagingTemplate.convertAndSend("/topic/classroom/" + classroomId, response);
    }

    @MessageMapping("/page_lock_batch_unlock")
    public void pageLockBatchUnlock(@Payload Map<String, Object> request) {
        Long classroomId = Long.valueOf(request.get("classroomId").toString());
        Map<String, Object> data = (Map<String, Object>) request.get("data");

        System.out.println("[WebSocket] Broadcasting batch unlock: " + data);

        // 广播批量解锁
        WebSocketMessage response = WebSocketMessage.of(
                EVENT_PAGE_LOCK_BATCH_UNLOCK,
                classroomId,
                data);

        messagingTemplate.convertAndSend("/topic/classroom/" + classroomId, response);
    }

    /**
     * 广播问题展示状态
     */
    @MessageMapping("/display_question")
    public void displayQuestion(@Payload Map<String, Object> request) {
        Long classroomId = Long.valueOf(request.get("classroomId").toString());
        Map<String, Object> data = (Map<String, Object>) request.get("data");

        System.out.println("[WebSocket] Broadcasting display question: " + data);

        // 广播到放映页
        WebSocketMessage response = WebSocketMessage.of(
                EVENT_DISPLAY_QUESTION,
                classroomId,
                data);

        messagingTemplate.convertAndSend("/topic/classroom/" + classroomId, response);
    }
}
