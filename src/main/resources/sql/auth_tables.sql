-- 用户表（学生端）
CREATE TABLE IF NOT EXISTS `user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `username`    VARCHAR(50)  NOT NULL UNIQUE,
  `password`    VARCHAR(100) NOT NULL,
  `nickname`    VARCHAR(50)  DEFAULT NULL,
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 管理员表
CREATE TABLE IF NOT EXISTS `admin` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `username`    VARCHAR(50)  NOT NULL UNIQUE,
  `password`    VARCHAR(100) NOT NULL,
  `nickname`    VARCHAR(50)  DEFAULT NULL,
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- 插入默认管理员账号（密码: admin123）
INSERT IGNORE INTO `admin` (`username`, `password`, `nickname`) VALUES ('admin', 'admin123', '超级管理员');

-- 插入测试学生账号（密码: 123456）
INSERT IGNORE INTO `user` (`username`, `password`, `nickname`) VALUES ('student', '123456', '测试学生');
