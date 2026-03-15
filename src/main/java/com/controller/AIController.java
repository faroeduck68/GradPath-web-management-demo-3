package com.controller;

import cn.hutool.json.JSONObject;
import com.domain.AiQuestion;
import com.pojo.Result;
import com.service.AIService;
import com.service.JudgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.mapper.AiQuestionMapper;
import java.util.List;
import java.util.Map;
import com.mapper.SubmissionMapper;
import com.domain.Submission;
import java.util.Date;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.HashMap;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private AIService aiService;

    @Autowired
    private AiQuestionMapper aiQuestionMapper;

    @Autowired
    private JudgeService judgeService;


    // 【新增】注入 SubmissionMapper，用于操作数据库
    @Autowired
    private SubmissionMapper submissionMapper;

    @PostMapping("/run")
    public Result runCode(@RequestBody Map<String, Object> params) { // 注意改为 Object 以便接收数字
        String code = (String) params.get("code");
        String language = (String) params.get("language");
        String input = (String) params.get("input");
        String expected = (String) params.get("output");

        // 获取题目ID (前端需要传，如果为空则默认为0)
        Long questionId = params.get("questionId") != null ? Long.valueOf(params.get("questionId").toString()) : 0L;
        // 用户ID (暂时写死，后续从Token获取)
        Long userId = 1L;

        // 1. 调用判题机
        JSONObject result = judgeService.runCode(language, code, input, expected);

        if (result.containsKey("error")) {
            return Result.error(result.getStr("error"));
        }

        JSONObject status = result.getJSONObject("status");
        if (status == null) return Result.error("判题机返回数据异常");

        int statusId = status.getInt("id");
        String description = status.getStr("description");
        String stdout = result.getStr("stdout");
        String stderr = result.getStr("stderr");
        String compileOutput = result.getStr("compile_output");

        // 获取耗时 (Judge0返回的是秒，转为毫秒)
        String timeStr = result.getStr("time");
        int execTime = timeStr != null ? (int) (Double.parseDouble(timeStr) * 1000) : 0;

        // 2. 【核心修复】保存记录到数据库
        Submission submission = new Submission();
        submission.setUserId(userId);
        submission.setQuestionId(questionId);
        submission.setLanguage(language);
        submission.setCode(code);
        submission.setExecutionTime(execTime);
        submission.setCreateTime(new Date());

        // 根据状态设置 AC/WA/CE
        if (statusId == 3) {
            submission.setStatus("AC"); // Accepted
        } else if (statusId == 4) {
            submission.setStatus("WA"); // Wrong Answer
            submission.setErrorMsg("预期输出: " + expected + "\n实际输出: " + stdout);
        } else if (statusId == 6) {
            submission.setStatus("CE"); // Compile Error
            submission.setErrorMsg(compileOutput);
        } else {
            submission.setStatus("RE"); // Runtime Error
            submission.setErrorMsg(stderr != null ? stderr : description);
        }

        // 插入数据库
        submissionMapper.insert(submission);

        // 3. 构造返回信息
        StringBuilder msg = new StringBuilder();
        if (statusId == 3) {
            msg.append("✅ 恭喜！通过测试用例。\n");
            msg.append("⏱️ 耗时: ").append(execTime).append("ms");
            if (stdout != null) msg.append("\n【程序输出】:\n").append(stdout);
        } else {
            msg.append("❌ 未通过: ").append(description).append("\n");
            if (compileOutput != null) msg.append("【编译错误】:\n").append(compileOutput).append("\n");
            if (stderr != null) msg.append("【运行时错误】:\n").append(stderr).append("\n");
            if (stdout != null) msg.append("【实际输出】:\n").append(stdout).append("\n");
        }

        return Result.success(msg.toString());
    }


    // ============== 题目列表接口（支持分页、排序和搜索）==============
    @GetMapping("/list")
    public Result list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "9") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false) String orderDirection
    ) {
        // 设置默认排序
        if (orderBy == null || orderBy.isEmpty()) {
            orderBy = "create_time";
            orderDirection = "DESC";
        }
        if (orderDirection == null || orderDirection.isEmpty()) {
            orderDirection = "DESC";
        }

        // 启动分页
        PageHelper.startPage(pageNum, pageSize);
        List<AiQuestion> questions = aiQuestionMapper.selectByPage(keyword, orderBy, orderDirection);
        PageInfo<AiQuestion> pageInfo = new PageInfo<>(questions);

        // 构造返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("list", pageInfo.getList());
        result.put("total", pageInfo.getTotal());
        result.put("pageNum", pageInfo.getPageNum());
        result.put("pageSize", pageInfo.getPageSize());
        result.put("pages", pageInfo.getPages());

        return Result.success(result);
    }

    // ============== 题目详情接口 ==============
    @GetMapping("/detail/{id}")
    public Result detail(@PathVariable Long id) {
        // 调用 Mapper 查询数据库
        AiQuestion question = aiQuestionMapper.selectById(id);

        // 判空处理（可选，提升体验）
        if (question == null) {
            return Result.error("未找到该题目");
        }

        return Result.success(question);
    }

    // ============== AI 生成题目接口 (新增) ==============
    // 对应前端请求：POST /api/ai/generate?keyword=二叉树
    @PostMapping("/generate")
    public Result generate(@RequestParam String keyword) {
        // 1. 参数校验
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.error("关键词不能为空");
        }

        try {
            // 2. 调用 Service 生成题目
            // 注意：暂时硬编码 userId = 1L，实际项目中建议从登录 Token 获取
            Long userId = 1L;
            AiQuestion question = aiService.generateAndSave(keyword, userId);

            return Result.success(question);
        } catch (Exception e) {
            // 3. 异常处理（如 API Key 错误、网络超时、JSON 解析失败等）
            e.printStackTrace();
            return Result.error("AI 生成失败: " + e.getMessage());
        }
    }
    // ============== 运行代码接口 =============
}
