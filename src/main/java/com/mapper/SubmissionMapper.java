package com.mapper;

import com.domain.AiQuestion;
import com.domain.Submission;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface SubmissionMapper {
    // 插入提交记录
    int insert(Submission submission);

    // 根据ID查询提交详情
    Submission selectById(Long id);
}