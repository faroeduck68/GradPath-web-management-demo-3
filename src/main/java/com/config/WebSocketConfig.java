package com.config;

import com.handler.InterviewWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final InterviewWebSocketHandler interviewHandler;

    public WebSocketConfig(InterviewWebSocketHandler interviewHandler) {
        this.interviewHandler = interviewHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(interviewHandler, "/ws/interview")
                .setAllowedOrigins("*");
    }
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();

        // 将文本消息的缓冲区大小增加到 512KB (具体数值可根据你的音频切片大小进行调整)
        container.setMaxTextMessageBufferSize(512 * 1024);

        // 如果你将来打算直接传输二进制流(Blob/ArrayBuffer)，顺便把二进制缓冲区也调大
        container.setMaxBinaryMessageBufferSize(512 * 1024);

        // 可选配置：设置最大会话空闲时间（毫秒），例如 15 分钟
        // container.setMaxSessionIdleTimeout(15 * 60 * 1000L);

        return container;
    }
}
