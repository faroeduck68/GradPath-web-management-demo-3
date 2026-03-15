package com.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.dto.AudioDataDTO;
import com.dto.InterviewStartDTO;
import com.dto.WsMessage;
import com.service.InterviewPipelineService;
import com.service.InterviewSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewWebSocketHandler extends TextWebSocketHandler {

    private final InterviewPipelineService pipelineService;
    private final InterviewSessionManager sessionManager;

    // sessionId -> WebSocketSession
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("WebSocket connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JSONObject json = JSON.parseObject(message.getPayload());
            String type = json.getString("type");
            JSONObject data = json.getJSONObject("data");

            switch (type) {
                case "interview.start" -> handleStart(session, data);
                case "audio.data" -> handleAudio(session, data);
                case "interrupt" -> handleInterrupt(session, data);
                case "interview.end" -> handleEnd(session);
                default -> sendError(session, "UNKNOWN_TYPE", "未知消息类型: " + type);
            }
        } catch (Exception e) {
            log.error("处理消息异常: {}", e.getMessage(), e);
            sendError(session, "INTERNAL_ERROR", "服务器内部错误");
        }
    }

    private void handleStart(WebSocketSession session, JSONObject data) {
        InterviewStartDTO dto = data.toJavaObject(InterviewStartDTO.class);
        String sessionId = session.getId();

        // 初始化会话上下文到Redis
        sessionManager.createSession(sessionId, dto);

        // 发送就绪消息 + AI开场白
        pipelineService.generateGreeting(sessionId, dto, (greeting) -> {
            sendMessage(session, WsMessage.of("interview.ready", Map.of(
                    "sessionId", sessionId,
                    "greeting", greeting
            )));
        });
    }

    private void handleAudio(WebSocketSession session, JSONObject data) {
        AudioDataDTO dto = data.toJavaObject(AudioDataDTO.class);
        String sessionId = session.getId();

        // 启动 STT -> LLM -> TTS 流水线
        pipelineService.process(sessionId, dto, new PipelineCallback(session, dto.getTurnId()));
    }

    private void handleInterrupt(WebSocketSession session, JSONObject data) {
        String turnId = data.getString("turnId");
        String sessionId = session.getId();

        // 取消当前turnId对应的所有异步任务
        pipelineService.cancelTurn(sessionId, turnId);

        sendMessage(session, WsMessage.of("interrupt.ack", Map.of(
                "cancelledTurnId", turnId
        )));
    }

    private void handleEnd(WebSocketSession session) {
        String sessionId = session.getId();
        pipelineService.generateSummary(sessionId, (summary) -> {
            sendMessage(session, WsMessage.of("interview.summary", summary));
            sessionManager.removeSession(sessionId);
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        pipelineService.cancelAll(sessionId);
        sessionManager.removeSession(sessionId);
        log.info("WebSocket disconnected: {} status: {}", sessionId, status);
    }

    // --- 工具方法 ---

    public void sendMessage(WebSocketSession session, WsMessage msg) {
        if (session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(JSON.toJSONString(msg)));
            } catch (IOException e) {
                log.error("发送消息失败: {}", e.getMessage());
            }
        }
    }

    private void sendError(WebSocketSession session, String code, String message) {
        sendMessage(session, WsMessage.of("error", Map.of("code", code, "message", message)));
    }

    /**
     * Pipeline回调：将STT/LLM/TTS结果实时推送给前端
     */
    @RequiredArgsConstructor
    public class PipelineCallback implements InterviewPipelineService.Callback {
        private final WebSocketSession session;
        private final String turnId;

        @Override
        public void onSttResult(String text) {
            sendMessage(session, WsMessage.of("stt.result", Map.of(
                    "turnId", turnId, "text", text
            )));
        }

        @Override
        public void onAiText(int seq, String delta, boolean finished) {
            sendMessage(session, WsMessage.of("ai.text", Map.of(
                    "turnId", turnId, "seq", seq, "delta", delta, "finished", finished
            )));
        }

        @Override
        public void onAiAudio(int seq, String audioBase64, String format, int duration) {
            sendMessage(session, WsMessage.of("ai.audio", Map.of(
                    "turnId", turnId, "seq", seq, "audio", audioBase64,
                    "format", format, "duration", duration
            )));
        }

        @Override
        public void onTurnComplete() {
            sendMessage(session, WsMessage.of("turn.complete", Map.of("turnId", turnId)));
        }

        @Override
        public void onError(String code, String message) {
            sendError(session, code, message);
        }
    }
}
