package com.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 面试开始请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewStartDTO {
    private String position;
    private String difficulty;
    private String language;
}
