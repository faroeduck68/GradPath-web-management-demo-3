package com.controller;

import cn.hutool.json.JSONObject;
import com.pojo.Result;
import com.dto.CodeRunDTO;
import com.service.JudgeService;
import com.service.AIService; // 保留这个
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

// ... 后面的代码不用动 ...
@RestController
@RequestMapping("/api/judge")
public class JudgeController {

    @Autowired
    private JudgeService judgeService;

    @Autowired
    private AIService aiService;

    /**
     * 1. 自测运行 (Debug Mode)
     * 特点：不传 expected_output，Judge0 只负责运行，返回 stdout
     */
    @PostMapping("/debug")
    public Result debugCode(@RequestBody CodeRunDTO dto) {
        JSONObject result = judgeService.runCode(
                dto.getLanguage(),
                dto.getCode(),
                dto.getInput(),
                null // <--- 关键：自测不传预期输出
        );
        return Result.success(result);
    }

    /**
     * 2. 正式提交 (Judge Mode)
     * 特点：传入 expected_output，Judge0 会严格比对，返回 Accepted 或 Wrong Answer
     */
    @PostMapping("/submit")
    public Result submitCode(@RequestBody CodeRunDTO dto) {
        JSONObject result = judgeService.runCode(
                dto.getLanguage(),
                dto.getCode(),
                dto.getInput(),
                dto.getOutput() // <--- 关键：传入题目定义的标准答案
        );
        return Result.success(result);
    }

    /**
     * 3. AI 错误分析接口
     */
    @PostMapping("/analyze")
    public Result analyzeError(@RequestBody CodeRunDTO dto) {
        // 这里的 prompt 可以根据需要调整
        String prompt = String.format(
                "我是编程初学者，正在做算法题。我的代码提交后报错了，请帮我分析原因。\n" +
                        "【题目】：%s\n" +
                        "【代码】：\n%s\n" +
                        "【报错信息】：%s\n" +
                        "【期望输出】：%s\n" +
                        "请用通俗易懂的语言解释错误，并给出修改后的代码片段。",
                dto.getQuestionTitle(), dto.getCode(), dto.getErrorMsg(), dto.getOutput()
        );

        // 调用你的 DeepSeek 或 GPT 服务
        String analysis = aiService.chat(prompt);
        return Result.success(analysis);
    }
}