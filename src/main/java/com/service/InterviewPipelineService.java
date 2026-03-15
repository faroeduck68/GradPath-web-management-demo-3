package com.service;

import com.dto.AudioDataDTO;
import com.dto.InterviewStartDTO;
import com.service.TtsService.TtsResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewPipelineService {

    private final SttService sttService;
    private final LlmService llmService;
    private final TtsService ttsService;
    private final InterviewSessionManager sessionManager;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    // sessionId -> 当前活跃的turnId对应的取消标志
    private final ConcurrentHashMap<String, AtomicBoolean> activeTurns = new ConcurrentHashMap<>();

    // 句子切分正则：遇到句号、问号、感叹号、分号时切分
    private static final Pattern SENTENCE_END = Pattern.compile("[。？！；.?!;]");

    private static final String SYSTEM_PROMPT = """
            你是Kairo，一位专业、友善的AI面试官。你正在对候选人进行技术面试。
            规则：
            1. 每次只问一个问题，等候选人回答后再追问或换题
            2. 根据候选人的回答深度来调整追问方向
            3. 语气专业但不刻板，适当给予鼓励
            4. 回答要简洁，每次回复控制在2-3句话
            5. 如果候选人回答不上来，给予适当提示后换题
            """;

    /**
     * Pipeline回调接口
     */
    public interface Callback {
        void onSttResult(String text);
        void onAiText(int seq, String delta, boolean finished);
        void onAiAudio(int seq, String audioBase64, String format, int duration);
        void onTurnComplete();
        void onError(String code, String message);
    }

    /**
     * 生成面试开场白
     */
    public void generateGreeting(String sessionId, InterviewStartDTO dto, Consumer<String> onReady) {
        executor.submit(() -> {
            String prompt = String.format(
                    "请用1-2句话做自我介绍并开始面试。面试岗位：%s，难度：%s",
                    dto.getPosition(), dto.getDifficulty());

            List<Map<String, String>> history = List.of(
                    Map.of("role", "user", "content", prompt));

            StringBuilder greeting = new StringBuilder();
            llmService.chatStream(SYSTEM_PROMPT, history, greeting::append, (full) -> {
                sessionManager.appendHistory(sessionId, "assistant", full);
                onReady.accept(full);
            });
        });
    }

    /**
     * 核心处理管道：STT -> LLM(流式) -> TTS(按句切分，异步)
     */
    public void process(String sessionId, AudioDataDTO audioData, Callback callback) {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        activeTurns.put(sessionId, cancelled);

        executor.submit(() -> {
            try {
                // === 1. STT ===
                String userText = sttService.recognize(audioData.getAudio(), audioData.getFormat());
                if (userText == null || userText.isBlank()) {
                    callback.onError("STT_FAILED", "语音识别失败，请重试");
                    return;
                }
                if (cancelled.get()) return;
                callback.onSttResult(userText);

                // 保存用户发言到历史
                sessionManager.appendHistory(sessionId, "user", userText);

                // === 2. LLM 流式 + 按句切分 -> 3. TTS 异步合成 ===
                List<Map<String, String>> history = sessionManager.getHistory(sessionId);
                AtomicInteger seq = new AtomicInteger(0);
                StringBuilder sentenceBuffer = new StringBuilder();
                // TTS任务队列，保证音频按顺序返回
                BlockingQueue<CompletableFuture<TtsResult>> ttsQueue = new LinkedBlockingQueue<>();

                // 启动TTS消费线程：按顺序等待TTS结果并推送
                CompletableFuture<Void> ttsConsumer = CompletableFuture.runAsync(() -> {
                    try {
                        while (!cancelled.get()) {
                            CompletableFuture<TtsResult> future = ttsQueue.poll(30, TimeUnit.SECONDS);
                            if (future == null) break;
                            TtsResult result = future.get(30, TimeUnit.SECONDS);
                            if (cancelled.get()) break;
                            if (result != null) {
                                callback.onAiAudio(result.durationMs(), result.audioBase64(),
                                        result.format(), result.durationMs());
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        log.error("TTS消费异常: {}", e.getMessage());
                    }
                }, executor);

                llmService.chatStream(SYSTEM_PROMPT, history,
                        // onDelta: 每收到一个token
                        (delta) -> {
                            if (cancelled.get()) return;
                            sentenceBuffer.append(delta);

                            // 检查是否有完整句子
                            String text = sentenceBuffer.toString();
                            var matcher = SENTENCE_END.matcher(text);
                            int lastEnd = 0;
                            while (matcher.find()) {
                                int end = matcher.end();
                                String sentence = text.substring(lastEnd, end).trim();
                                lastEnd = end;

                                if (!sentence.isEmpty()) {
                                    int currentSeq = seq.getAndIncrement();
                                    callback.onAiText(currentSeq, sentence, false);

                                    // 异步提交TTS任务
                                    String s = sentence;
                                    CompletableFuture<TtsResult> ttsFuture =
                                            CompletableFuture.supplyAsync(() -> ttsService.synthesize(s), executor);
                                    ttsQueue.offer(ttsFuture);
                                }
                            }
                            // 保留未切分的部分
                            if (lastEnd > 0) {
                                sentenceBuffer.delete(0, lastEnd);
                            }
                        },
                        // onComplete: LLM生成完毕
                        (fullText) -> {
                            if (cancelled.get()) return;
                            // 处理buffer中剩余的文本
                            String remaining = sentenceBuffer.toString().trim();
                            if (!remaining.isEmpty()) {
                                int currentSeq = seq.getAndIncrement();
                                callback.onAiText(currentSeq, remaining, true);
                                CompletableFuture<TtsResult> ttsFuture =
                                        CompletableFuture.supplyAsync(() -> ttsService.synthesize(remaining), executor);
                                ttsQueue.offer(ttsFuture);
                            } else {
                                // 发送finished标记
                                callback.onAiText(seq.get(), "", true);
                            }

                            sessionManager.appendHistory(sessionId, "assistant", fullText);

                            // 等待所有TTS完成后发送turn.complete
                            ttsConsumer.thenRun(() -> {
                                if (!cancelled.get()) {
                                    callback.onTurnComplete();
                                }
                            });
                        }
                );
            } catch (Exception e) {
                log.error("Pipeline处理异常: {}", e.getMessage(), e);
                callback.onError("PIPELINE_ERROR", "处理失败: " + e.getMessage());
            }
        });
    }

    /**
     * 取消指定turn的任务
     */
    public void cancelTurn(String sessionId, String turnId) {
        AtomicBoolean cancelled = activeTurns.get(sessionId);
        if (cancelled != null) {
            cancelled.set(true);
            log.info("已取消turn: session={}, turn={}", sessionId, turnId);
        }
    }

    /**
     * 取消会话的所有任务
     */
    public void cancelAll(String sessionId) {
        cancelTurn(sessionId, null);
        activeTurns.remove(sessionId);
    }

    /**
     * 生成面试总结
     */
    public void generateSummary(String sessionId, Consumer<Map<String, Object>> onSummary) {
        executor.submit(() -> {
            List<Map<String, String>> history = sessionManager.getHistory(sessionId);
            int turnCount = history.stream()
                    .filter(m -> "assistant".equals(m.get("role")))
                    .mapToInt(m -> 1).sum();

            String summaryPrompt = "请根据以上面试对话，给出面试评价。" +
                    "包含：整体评价(evaluation)、知识(knowledge 1-10分)、" +
                    "表达(expression 1-10分)、逻辑(logic 1-10分)。用中文回答。";

            List<Map<String, String>> msgs = new ArrayList<>(history);
            msgs.add(Map.of("role", "user", "content", summaryPrompt));

            StringBuilder result = new StringBuilder();
            llmService.chatStream(SYSTEM_PROMPT, msgs, result::append, (full) -> {
                Map<String, Object> summary = new HashMap<>();
                summary.put("sessionId", sessionId);
                summary.put("totalTurns", turnCount);
                summary.put("evaluation", full);
                onSummary.accept(summary);
            });
        });
    }
}
