package com.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 前端发送的音频数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioDataDTO {
    private String audio;      // base64编码音频
    private String format;     // webm
    private Integer sampleRate;// 16000
    private String turnId;
}
