package com.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.domain.AiQuestion;
import com.mapper.AiQuestionMapper;
import com.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AIServiceImpl implements AIService {

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.api-url}")
    private String apiUrl;

    @Autowired
    private AiQuestionMapper aiQuestionMapper;

    /**
     * 实现通用的 AI 对话功能
     * 用于错误分析、代码解释等
     */
    @Override
    public String chat(String prompt) {
        // 1. 构造请求体 (OpenAI/DeepSeek 标准格式)
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("model", "deepseek-chat"); // 这里的模型名字要确认和你的API支持的一致
        bodyMap.put("stream", false);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(new HashMap<String, String>() {{
            put("role", "user");
            put("content", prompt);
        }});
        bodyMap.put("messages", messages);

        try {
            // 2. 发送请求
            String response = HttpRequest.post(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(JSONUtil.toJsonStr(bodyMap))
                    .timeout(60000) // AI 分析可能比较慢，设置 60秒 超时
                    .execute()
                    .body();

            // 3. 解析结果
            JSONObject jsonResponse = JSONUtil.parseObj(response);

            // 检查是否有错误返回
            if (jsonResponse.containsKey("error")) {
                return "AI 调用失败: " + jsonResponse.getJSONObject("error").getStr("message");
            }

            // 获取内容: choices[0].message.content
            return jsonResponse.getByPath("choices[0].message.content", String.class);

        } catch (Exception e) {
            e.printStackTrace();
            return "AI 服务暂时不可用，请稍后再试。";
        }
    }

    // ... 原有的 generateAndSave 方法保持不变 ...
    @Override
    public AiQuestion generateAndSave(String keyword, Long userId) {
        // 1. 构造 Prompt
        String prompt = "你是一个算法专家。请围绕【" + keyword + "】出一道算法题。" +
                "严格以 JSON 格式返回：{title, difficulty, description, inputFormat, outputFormat, " +
                "sampleCase:{\"input\":\"\",\"output\":\"\"}, " +
                "testCases:[{\"input\":\"\",\"output\":\"\"}], tags}。" +
                "不要返回任何包含 ```json 的文字。";

        // 2. 使用 Hutool 构建请求体
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("model", "deepseek-chat");
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(new HashMap<String, String>() {{
            put("role", "user");
            put("content", prompt);
        }});
        bodyMap.put("messages", messages);

        // 3. 使用 Hutool 发送请求
        String response = HttpRequest.post(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(bodyMap)) // 对象转JSON字符串
                .execute().body();

        // 4. 使用 Hutool 解析响应
        JSONObject jsonResponse = JSONUtil.parseObj(response);
        // 获取 DeepSeek 返回的核心内容
        String aiContent = jsonResponse.getByPath("choices[0].message.content", String.class);

        // 5. 将 AI 的内容转为 Hutool 的 JSONObject
        JSONObject aiObj = JSONUtil.parseObj(aiContent);

        // 6. 赋值给实体类
        AiQuestion question = new AiQuestion();
        question.setCreatorId(userId);
        question.setTitle(aiObj.getStr("title"));
        question.setDifficulty(aiObj.getStr("difficulty"));
        question.setDescription(aiObj.getStr("description"));
        question.setInputFormat(aiObj.getStr("inputFormat"));
        question.setOutputFormat(aiObj.getStr("outputFormat"));
        question.setTags(aiObj.getStr("tags"));
        question.setStatus(0);

        // 【关键】直接使用 JSONUtil 将 JSON 对象/数组转为字符串存入 String 字段
        question.setSampleCase(JSONUtil.toJsonStr(aiObj.getJSONObject("sampleCase")));
        question.setTestCasesJson(JSONUtil.toJsonStr(aiObj.getJSONArray("testCases")));

        // 7. 调用原生 MyBatis 插入数据库
        aiQuestionMapper.insert(question);

        return question;
    }
}