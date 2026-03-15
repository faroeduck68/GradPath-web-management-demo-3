package com.mapper;

import com.domain.AiQuestion;
import com.domain.Submission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface AiQuestionMapper {
    // 插入题目
    int insert(AiQuestion aiQuestion);

    // 根据ID查询题目
    AiQuestion selectById(Long id);

    // 查询所有题目列表
    List<AiQuestion> selectAll();

    // 分页查询题目列表（支持排序和搜索）
    List<AiQuestion> selectByPage(
            @Param("keyword") String keyword,
            @Param("orderBy") String orderBy,
            @Param("orderDirection") String orderDirection
    );
}
