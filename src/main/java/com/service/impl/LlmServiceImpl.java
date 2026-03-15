package com.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Service
public class LlmServiceImpl implements LlmService {

    @Value("${deepseek.api-url}")
    private String apiUrl;

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${interview.llm.model}")
    private String model;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();

    @Override
    public void chatStream(String systemPrompt,
                           List<Map<String, String>> history,
                           Consumer<String> onDelta,
                           Consumer<String> onComplete) {
        try {
            JSONArray messages = new JSONArray();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.addAll(history);

            JSONObject body = new JSONObject();
            body.put("model", model);
            body.put("messages", messages);
            body.put("stream", true);

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(body.toJSONString(),
                            MediaType.parse("application/json")))
                    .build();

            StringBuilder fullText = new StringBuilder();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.error("LLM请求失败: {}", response.code());
                    return;
                }
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body().byteStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) continue;
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) break;

                    JSONObject chunk = JSON.parseObject(data);
                    JSONArray choices = chunk.getJSONArray("choices");
                    if (choices == null || choices.isEmpty()) continue;

                    JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
                    if (delta == null) continue;

                    String content = delta.getString("content");
                    if (content != null && !content.isEmpty()) {
                        fullText.append(content);
                        onDelta.accept(content);
                    }
                }
            }
            onComplete.accept(fullText.toString());
        } catch (Exception e) {
            log.error("LLM流式调用异常: {}", e.getMessage(), e);
        }
    }
}
