package com.domain;

import lombok.Data;

import java.util.Date;

@Data
public class Submission {
    private Long id;
    private Long userId;
    private Long questionId;
    private String language;
    private String code;
    private String status;
    private Integer passCases;
    private Integer totalCases;
    private Integer executionTime;
    private String errorMsg;
    private String aiAnalysis;
    private Date createTime;
}