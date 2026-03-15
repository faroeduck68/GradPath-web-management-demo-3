package com.service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * LLM对话服务接口 - 支持流式输出
 */
public interface LlmService {
    /**
     * 流式对话
     * @param systemPrompt 系统提示词
     * @param history 对话历史
     * @param onDelta 每收到一个token的回调
     * @param onComplete 生成完毕回调(完整文本)
     */
    void chatStream(String systemPrompt,
                    List<Map<String, String>> history,
                    Consumer<String> onDelta,
                    Consumer<String> onComplete);
}
