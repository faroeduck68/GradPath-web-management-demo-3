package com;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(scanBasePackages = {
        "com.controller",
        "com.service",
        "com.mapper",
        "com",
        "com.config",
        "com.pojo",
        "com.domain",

})
@MapperScan("com.mapper")
public class JobApplication {
    public static void main(String[] args) {
        System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(JobApplication.class, args);
    }
}