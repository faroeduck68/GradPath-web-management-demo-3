package com.service;

import com.domain.AiQuestion;

public interface AIService {
    /**
     * 调用 DeepSeek 生成题目并保存
     * @param keyword 关键词
     * @param userId 操作人
     */
    AiQuestion generateAndSave(String keyword, Long userId);


    String chat(String prompt);

}