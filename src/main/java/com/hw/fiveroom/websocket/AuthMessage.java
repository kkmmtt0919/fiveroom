package com.hw.fiveroom.websocket;

public record AuthMessage(
        String type,
        String roomId,
        String playerId,
        String playerToken
) {


}
