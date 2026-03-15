package com.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JudgeService {

    private static final String SUBMISSION_FIELDS =
            "stdout,stderr,compile_output,message,status,exit_code,time,memory";

    @Value("${judge0.base-url:http://127.0.0.1:2358}")
    private String judge0BaseUrl;

    @Value("${judge0.api-key:}")
    private String judge0ApiKey;

    @Value("${judge0.poll-interval-ms:500}")
    private long pollIntervalMs;

    @Value("${judge0.max-poll-count:20}")
    private int maxPollCount;

    public JSONObject runCode(String language, String code, String input, String expectedOutput) {
        try {
            JSONObject createResponse = createSubmission(language, code, input, expectedOutput);
            String token = createResponse.getStr("token");
            if (token == null || token.isBlank()) {
                return createErrorResult("Judge0 未返回 submission token", -1);
            }

            JSONObject submission = pollSubmissionResult(token);
            return formatResult(submission, expectedOutput);
        } catch (IllegalArgumentException e) {
            return createErrorResult(e.getMessage(), -1);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResult("连接本地 Judge0 失败: " + e.getMessage(), -1);
        }
    }

    private JSONObject createSubmission(String language, String code, String input, String expectedOutput) {
        JSONObject body = new JSONObject();
        body.set("source_code", code);
        body.set("language_id", getJudge0LanguageId(language));
        body.set("stdin", input == null ? "" : input);
        if (expectedOutput != null && !expectedOutput.isBlank()) {
            body.set("expected_output", expectedOutput);
        }

        String url = normalizeBaseUrl() + "/submissions/?base64_encoded=false&wait=false";
        String responseBody = executePost(url, body);
        return JSONUtil.parseObj(responseBody);
    }

    private JSONObject pollSubmissionResult(String token) throws InterruptedException {
        String url = normalizeBaseUrl() + "/submissions/" + token
                + "?base64_encoded=false&fields=" + SUBMISSION_FIELDS;

        for (int attempt = 0; attempt < maxPollCount; attempt++) {
            String responseBody = executeGet(url);
            JSONObject response = JSONUtil.parseObj(responseBody);
            Integer statusId = response.getByPath("status.id", Integer.class);
            if (statusId != null && statusId > 2) {
                return response;
            }
            Thread.sleep(pollIntervalMs);
        }

        throw new IllegalStateException("Judge0 判题超时，请稍后重试");
    }

    private JSONObject formatResult(JSONObject response, String expectedOutput) {
        if (response == null) {
            return createErrorResult("Judge0 未返回结果", -1);
        }

        JSONObject result = new JSONObject();
        String stdout = response.getStr("stdout", "");
        String stderr = response.getStr("stderr", "");
        String compileOutput = response.getStr("compile_output", "");
        String message = response.getStr("message", "");
        Integer exitCode = response.getInt("exit_code");
        Integer statusId = response.getByPath("status.id", Integer.class);
        String statusDescription = response.getByPath("status.description", String.class);

        result.set("stdout", stdout);
        result.set("stderr", stderr);
        result.set("memory", response.get("memory"));
        result.set("time", response.get("time"));
        result.set("exit_code", exitCode == null ? 0 : exitCode);

        if (statusId == null) {
            return createErrorResult("Judge0 返回状态缺失", -1);
        }

        if (statusId == 3) {
            boolean hasExpectedOutput = expectedOutput != null && !expectedOutput.isBlank();
            result.set("status", "success");
            if (hasExpectedOutput) {
                result.set("correct", true);
            }
            return result;
        }

        if (statusId == 4) {
            result.set("status", "wrong_answer");
            result.set("correct", false);
            result.set("error", buildWrongAnswerMessage(expectedOutput, stdout));
            return result;
        }

        String errorMessage = firstNonBlank(compileOutput, stderr, message, statusDescription, "Judge0 执行失败");
        result.set("status", mapStatus(statusId));
        result.set("error", errorMessage);
        result.set("stderr", firstNonBlank(stderr, compileOutput, message, ""));
        result.set("exit_code", exitCode == null ? 1 : exitCode);
        return result;
    }

    private String executePost(String url, JSONObject body) {
        HttpRequest request = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .body(body.toString())
                .timeout(30000);
        addAuthHeader(request);
        try (HttpResponse response = request.execute()) {
            return validateResponse(response);
        }
    }

    private String executeGet(String url) {
        HttpRequest request = HttpRequest.get(url).timeout(30000);
        addAuthHeader(request);
        try (HttpResponse response = request.execute()) {
            return validateResponse(response);
        }
    }

    private void addAuthHeader(HttpRequest request) {
        if (judge0ApiKey != null && !judge0ApiKey.isBlank() && !"none".equalsIgnoreCase(judge0ApiKey)) {
            request.header("X-Auth-Token", judge0ApiKey);
        }
    }

    private String validateResponse(HttpResponse response) {
        int status = response.getStatus();
        String body = response.body();
        if (status >= 200 && status < 300) {
            return body;
        }
        throw new IllegalStateException("Judge0 响应异常(" + status + "): " + body);
    }

    private String normalizeBaseUrl() {
        if (judge0BaseUrl.endsWith("/")) {
            return judge0BaseUrl.substring(0, judge0BaseUrl.length() - 1);
        }
        return judge0BaseUrl;
    }

    private String buildWrongAnswerMessage(String expectedOutput, String actualOutput) {
        return "期望输出:\n" + (expectedOutput == null ? "" : expectedOutput)
                + "\n\n你的输出:\n" + (actualOutput == null ? "" : actualOutput);
    }

    private String mapStatus(int statusId) {
        return switch (statusId) {
            case 5 -> "time_limit_exceeded";
            case 6 -> "compile_error";
            case 7, 8, 9, 10, 11, 12, 14 -> "runtime_error";
            case 13 -> "system_error";
            default -> "error";
        };
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private int getJudge0LanguageId(String lang) {
        if (lang == null || lang.isBlank()) {
            return 71;
        }

        return switch (lang.toLowerCase()) {
            case "c" -> 50;
            case "cpp", "c++" -> 54;
            case "java" -> 62;
            case "javascript", "js" -> 63;
            case "typescript", "ts" -> 74;
            case "python", "python3" -> 71;
            case "csharp", "c#" -> 51;
            case "go" -> 60;
            case "ruby" -> 72;
            case "rust" -> 73;
            case "php" -> 68;
            case "kotlin" -> 78;
            case "swift" -> 83;
            default -> throw new IllegalArgumentException("暂不支持的语言: " + lang);
        };
    }

    private JSONObject createErrorResult(String errorMessage, int exitCode) {
        JSONObject error = new JSONObject();
        error.set("status", "error");
        error.set("error", errorMessage);
        error.set("exit_code", exitCode);
        error.set("stdout", "");
        error.set("stderr", errorMessage);
        return error;
    }
}
