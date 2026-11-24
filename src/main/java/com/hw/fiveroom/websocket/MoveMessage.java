package com.hw.fiveroom.websocket;

public record MoveMessage(
        String type,String requestId,String roomId,Integer row,Integer col
) {
}
