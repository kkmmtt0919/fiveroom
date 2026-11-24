package com.hw.fiveroom.websocket;

public record MoveRejectedMessage(
        String type,
        String requestId,
        int status,
        String reason
) {
}
