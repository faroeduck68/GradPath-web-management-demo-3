package com.service;

/**
 * 语音转文字服务接口
 */
public interface SttService {
    /**
     * 将音频数据转为文本
     * @param audioBase64 base64编码的音频
     * @param format 音频格式 (webm, wav等)
     * @return 识别出的文本
     */
    String recognize(String audioBase64, String format);
}
