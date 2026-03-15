package com.service;

import com.alibaba.fastjson2.JSON;
import com.dto.InterviewStartDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 面试会话管理器 - 使用Redis缓存会话上下文
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewSessionManager {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "interview:session:";
    private static final Duration SESSION_TTL = Duration.ofHours(2);

    /**
     * 创建面试会话
     */
    public void createSession(String sessionId, InterviewStartDTO dto) {
        Map<String, Object> context = new HashMap<>();
        context.put("position", dto.getPosition());
        context.put("difficulty", dto.getDifficulty());
        context.put("language", dto.getLanguage());
        context.put("turnCount", 0);
        context.put("history", new ArrayList<>());

        redisTemplate.opsForValue().set(
                KEY_PREFIX + sessionId,
                JSON.toJSONString(context),
                SESSION_TTL
        );
        log.info("面试会话已创建: {}", sessionId);
    }

    /**
     * 获取会话上下文
     */
    public Map<String, Object> getContext(String sessionId) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + sessionId);
        if (json == null) return null;
        return JSON.parseObject(json, Map.class);
    }

    /**
     * 追加对话历史
     */
    public void appendHistory(String sessionId, String role, String content) {
        Map<String, Object> context = getContext(sessionId);
        if (context == null) return;

        List<Map<String, String>> history = (List<Map<String, String>>) context.get("history");
        history.add(Map.of("role", role, "content", content));
        context.put("turnCount", (int) context.getOrDefault("turnCount", 0) + (role.equals("assistant") ? 1 : 0));

        redisTemplate.opsForValue().set(
                KEY_PREFIX + sessionId,
                JSON.toJSONString(context),
                SESSION_TTL
        );
    }

    /**
     * 获取对话历史（用于构建LLM prompt）
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getHistory(String sessionId) {
        Map<String, Object> context = getContext(sessionId);
        if (context == null) return List.of();
        return (List<Map<String, String>>) context.getOrDefault("history", List.of());
    }

    /**
     * 删除会话
     */
    public void removeSession(String sessionId) {
        redisTemplate.delete(KEY_PREFIX + sessionId);
        log.info("面试会话已删除: {}", sessionId);
    }
}
