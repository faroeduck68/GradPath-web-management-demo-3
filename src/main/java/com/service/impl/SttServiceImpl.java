package com.service.impl;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.service.SttService;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.Base64;

@Slf4j
@Service
public class SttServiceImpl implements SttService {

    @Value("${interview.stt.api-key}")
    private String apiKey;

    @Override
    public String recognize(String audioBase64, String format) {
        try {
            byte[] audioBytes = Base64.getDecoder().decode(audioBase64);

            // 前端发送的是 WAV 格式 (16kHz 单声道 16bit PCM)
            // WAV 文件头 44 字节，跳过后是纯 PCM 数据
            int headerSize = 44;
            int pcmOffset = Math.min(headerSize, audioBytes.length);

            RecognitionParam param = RecognitionParam.builder()
                    .model("paraformer-realtime-v2")
                    .format("pcm")
                    .sampleRate(16000)
                    .apiKey(apiKey)
                    .build();

            Recognition recognition = new Recognition();

            // 将 PCM 数据（跳过 WAV 头）包装为 Flowable 流，按 3200 字节分片发送
            Flowable<ByteBuffer> audioStream = Flowable.create(emitter -> {
                int chunkSize = 3200;
                for (int i = pcmOffset; i < audioBytes.length; i += chunkSize) {
                    int end = Math.min(i + chunkSize, audioBytes.length);
                    emitter.onNext(ByteBuffer.wrap(audioBytes, i, end - i));
                }
                emitter.onComplete();
            }, io.reactivex.BackpressureStrategy.BUFFER);

            // 调用流式识别，收集所有句子结果
            StringBuilder result = new StringBuilder();
            Flowable<RecognitionResult> resultFlowable = recognition.streamCall(param, audioStream);
            resultFlowable.blockingForEach(r -> {
                if (r.isSentenceEnd() && r.getSentence() != null) {
                    result.append(r.getSentence().getText());
                }
            });

            String text = result.toString().trim();
            log.info("STT识别结果: {}", text);
            return text.isEmpty() ? null : text;
        } catch (Exception e) {
            log.error("STT识别异常: {}", e.getMessage(), e);
            return null;
        }
    }
}