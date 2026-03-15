package com.service;

/**
 * 文字转语音服务接口
 */
public interface TtsService {
    /**
     * 将文本合成为音频
     * @param text 要合成的文本
     * @return TtsResult 包含base64音频和时长
     */
    TtsResult synthesize(String text);

    record TtsResult(String audioBase64, String format, int durationMs) {}
}
