package com.dto;

public class CodeRunDTO {
    private String language;      // 编程语言 (java, python, cpp)
    private String code;          // 用户写的代码
    private String input;         // 题目输入样例
    private String output;        // 题目输出样例 (提交时用)
    private String questionTitle; // 题目名称 (AI分析用)
    private String errorMsg;      // 错误信息 (AI分析用)

    //Getter and Setter
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }

    public String getQuestionTitle() { return questionTitle; }
    public void setQuestionTitle(String questionTitle) { this.questionTitle = questionTitle; }

    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
}