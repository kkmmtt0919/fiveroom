package com.hw.fiveroom.websocket;

public record MoveAppliedMessage(
        String type,String requestId,String roomId,
        String playerId,int row,int col,int sequence
) {
}
