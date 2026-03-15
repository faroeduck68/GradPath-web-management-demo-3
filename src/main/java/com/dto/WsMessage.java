package com.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * WebSocket 统一消息信封
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsMessage {
    private String type;
    private Map<String, Object> data;
    private Long timestamp;

    public static WsMessage of(String type, Map<String, Object> data) {
        return WsMessage.builder()
                .type(type)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
