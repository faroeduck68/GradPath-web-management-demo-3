-- ============================================================
-- GradPath AI 模拟面试模块 - MySQL 建表脚本
-- 数据库: duck5
-- ============================================================

USE duck5;

-- -----------------------------------------------------------
-- 1. 面试会话主表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS interview_chat_log;
DROP TABLE IF EXISTS interview_session;

CREATE TABLE interview_session (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id         BIGINT       NOT NULL                COMMENT '面试者用户ID',
    job_title       VARCHAR(100) NOT NULL                COMMENT '面试岗位',
    difficulty      VARCHAR(20)  NOT NULL DEFAULT 'medium' COMMENT '难度: easy/medium/hard',
    status          TINYINT      NOT NULL DEFAULT 0      COMMENT '状态: 0-进行中 1-已完成 2-异常中断',
    overall_score   INT                   DEFAULT NULL   COMMENT 'AI综合评分(0-100)',
    knowledge_score INT                   DEFAULT NULL   COMMENT '知识维度评分(1-10)',
    expression_score INT                  DEFAULT NULL   COMMENT '表达维度评分(1-10)',
    logic_score     INT                   DEFAULT NULL   COMMENT '逻辑维度评分(1-10)',
    ai_evaluation   TEXT                  DEFAULT NULL   COMMENT 'AI综合评价与改进建议',
    total_turns     INT          NOT NULL DEFAULT 0      COMMENT '对话总轮数',
    start_time      DATETIME     NOT NULL                COMMENT '面试开始时间',
    end_time        DATETIME              DEFAULT NULL   COMMENT '面试结束时间',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试会话主表';

-- -----------------------------------------------------------
-- 2. 面试对话流水表
-- -----------------------------------------------------------
CREATE TABLE interview_chat_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    session_id      BIGINT       NOT NULL                COMMENT '关联的面试会话ID',
    turn_id         VARCHAR(20)           DEFAULT NULL   COMMENT '对话轮次标识(如turn_001)',
    sender_role     VARCHAR(10)  NOT NULL                COMMENT '发送者角色: AI / USER',
    content         TEXT         NOT NULL                COMMENT '文本内容(STT识别文本或AI回复)',
    audio_url       VARCHAR(500)          DEFAULT NULL   COMMENT '阿里云OSS录音文件地址',
    seq             INT          NOT NULL DEFAULT 0      COMMENT '同一轮内的句子序号',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '对话发生时间',
    PRIMARY KEY (id),
    INDEX idx_session_id (session_id),
    INDEX idx_turn_id (session_id, turn_id),
    CONSTRAINT fk_chat_session FOREIGN KEY (session_id) REFERENCES interview_session(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试对话流水表';

-- -----------------------------------------------------------
-- 3. 面试预设配置表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS interview_config;

CREATE TABLE interview_config (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    preset_name     VARCHAR(50)  NOT NULL                COMMENT '预设名称',
    system_prompt   TEXT         NOT NULL                COMMENT 'AI面试官系统提示词',
    tts_voice       VARCHAR(50)  NOT NULL DEFAULT 'longxiaochun' COMMENT 'TTS语音角色',
    difficulty      VARCHAR(20)  NOT NULL DEFAULT 'medium' COMMENT '适用难度',
    is_default      TINYINT      NOT NULL DEFAULT 0      COMMENT '是否默认预设: 0-否 1-是',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE INDEX idx_preset_name (preset_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试预设配置表';

-- ============================================================
-- 测试数据
-- ============================================================

-- 面试会话测试数据
INSERT INTO interview_session (id, user_id, job_title, difficulty, status, overall_score, knowledge_score, expression_score, logic_score, ai_evaluation, total_turns, start_time, end_time) VALUES
(1, 1001, 'Java后端开发工程师', 'medium', 1, 82, 8, 7, 9,
 '候选人对Spring Boot核心原理掌握扎实，能清晰阐述自动配置和IOC容器的工作机制。在MySQL索引优化方面有实战经验，回答条理清晰。建议加强分布式系统（如微服务拆分、分布式事务）方面的知识储备，面对开放性架构问题时可以更主动地展开思路。',
 6, '2026-03-03 10:00:00', '2026-03-03 10:32:00'),

(2, 1002, '前端开发工程师', 'easy', 1, 68, 7, 6, 7,
 '候选人对Vue 3组合式API有基本了解，能说出ref和reactive的区别。CSS布局基础较好，但对性能优化（虚拟列表、懒加载）和工程化（Webpack/Vite原理）的理解偏浅。建议多做项目实践，关注前端性能监控和优化手段。',
 5, '2026-03-03 14:00:00', '2026-03-03 14:25:00');

-- 对话流水测试数据（会话1: Java后端面试）
INSERT INTO interview_chat_log (session_id, turn_id, sender_role, content, seq, create_time) VALUES
(1, NULL,      'AI',   '你好，我是Kairo，今天由我来面试你Java后端开发工程师这个岗位。我们开始吧，先请你简单介绍一下自己。', 0, '2026-03-03 10:00:05'),
(1, 'turn_001', 'USER', '面试官好，我叫张三，有三年Java开发经验，主要做过电商和后台管理系统，技术栈是Spring Boot加MyBatis加MySQL。', 0, '2026-03-03 10:00:35'),
(1, 'turn_001', 'AI',   '不错，有实际项目经验。那我想问一下，你能说说Spring Boot的自动配置原理是怎么工作的吗？', 0, '2026-03-03 10:00:42'),
(1, 'turn_002', 'USER', 'Spring Boot通过@EnableAutoConfiguration注解触发自动配置，它会读取META-INF下的spring.factories文件，加载里面配置的自动配置类，然后通过@Conditional系列注解来判断是否生效。', 0, '2026-03-03 10:01:20'),
(1, 'turn_002', 'AI',   '回答得很准确。那追问一下，如果两个自动配置类产生了冲突，Spring Boot是怎么处理优先级的？', 0, '2026-03-03 10:01:28'),
(1, 'turn_003', 'USER', '可以通过@AutoConfigureBefore和@AutoConfigureAfter来控制加载顺序，也可以用@AutoConfigureOrder设置优先级。如果有冲突，后加载的会覆盖先加载的Bean定义。', 0, '2026-03-03 10:02:05'),
(1, 'turn_003', 'AI',   '很好，理解得比较深入。我们换个方向，聊聊MySQL吧。你在项目中是怎么做SQL优化的？', 0, '2026-03-03 10:02:12');

-- 对话流水测试数据（会话2: 前端面试）
INSERT INTO interview_chat_log (session_id, turn_id, sender_role, content, seq, create_time) VALUES
(2, NULL,      'AI',   '你好，我是Kairo。今天面试前端开发工程师岗位，难度是入门级，放轻松就好。先介绍一下你自己吧。', 0, '2026-03-03 14:00:05'),
(2, 'turn_001', 'USER', '你好，我是李四，学前端一年多了，主要用Vue 3做过几个小项目。', 0, '2026-03-03 14:00:30'),
(2, 'turn_001', 'AI',   '好的。那你能说说Vue 3里ref和reactive有什么区别吗？', 0, '2026-03-03 14:00:36'),
(2, 'turn_002', 'USER', 'ref用来包装基本类型，访问的时候要加.value。reactive用来包装对象，直接访问属性就行，不用加.value。', 0, '2026-03-03 14:01:10'),
(2, 'turn_002', 'AI',   '说得对。那如果把一个reactive对象解构赋值给变量，响应性还在吗？怎么解决？', 0, '2026-03-03 14:01:18');

-- 预设配置测试数据
INSERT INTO interview_config (preset_name, system_prompt, tts_voice, difficulty, is_default) VALUES
('标准技术面试', '你是Kairo，一位专业、友善的AI面试官。你正在对候选人进行技术面试。每次只问一个问题，等候选人回答后再追问或换题。根据候选人的回答深度来调整追问方向。语气专业但不刻板，适当给予鼓励。回答要简洁，每次回复控制在2-3句话。', 'longxiaochun', 'medium', 1),
('压力面试', '你是一位严格的技术面试官。你会对候选人的回答进行深入追问，指出不足之处，测试候选人在压力下的表现。但不要人身攻击，保持专业。每次追问要有针对性。', 'longxiaochun', 'hard', 0),
('轻松入门面试', '你是一位温和的面试官，正在面试初级开发者。用鼓励的语气提问，如果候选人答不上来就给提示。问题以基础概念为主，不要问太深入的架构问题。', 'zhixiaobai', 'easy', 0);
