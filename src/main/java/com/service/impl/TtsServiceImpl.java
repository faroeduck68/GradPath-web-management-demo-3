package com.service.impl;

import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisAudioFormat;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.service.TtsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.Base64;

@Slf4j
@Service
public class TtsServiceImpl implements TtsService {

    @Value("${interview.tts.api-key}")
    private String apiKey;

    @Value("${interview.tts.voice}")
    private String voice;

    @Override
    public TtsResult synthesize(String text) {
        try {
            SpeechSynthesisParam param = SpeechSynthesisParam.builder()
                    .model("cosyvoice-v1")
                    .voice(voice)
                    .format(SpeechSynthesisAudioFormat.MP3_22050HZ_MONO_256KBPS)
                    .apiKey(apiKey)
                    .build();

            SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null, null, null);
            ByteBuffer audioBuffer = synthesizer.call(text);

            if (audioBuffer == null || !audioBuffer.hasRemaining()) {
                log.error("TTS合成返回空音频");
                return null;
            }

            byte[] audioBytes = new byte[audioBuffer.remaining()];
            audioBuffer.get(audioBytes);
            String audioBase64 = Base64.getEncoder().encodeToString(audioBytes);

            // 粗略估算时长: mp3 256kbps
            int durationMs = (int) (audioBytes.length * 8.0 / 256);

            log.info("TTS合成完成: {}字 -> {}bytes", text.length(), audioBytes.length);
            return new TtsResult(audioBase64, "mp3", durationMs);
        } catch (Exception e) {
            log.error("TTS合成异常: {}", e.getMessage(), e);
            return null;
        }
    }
}
