package com.hw.fiveroom.websocket;

public record GameOverMessage(
        String type,String roomId,String winnerId,
        String result
) {
}
