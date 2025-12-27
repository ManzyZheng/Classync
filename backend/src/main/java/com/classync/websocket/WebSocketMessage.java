package com.classync.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    private String event;  // 细粒度事件类型
    private Long classroomId;
    private Object payload;  // 轻量级数据
    
    // 便捷构造器
    public static WebSocketMessage of(String event, Long classroomId, Object payload) {
        return new WebSocketMessage(event, classroomId, payload);
    }
}

