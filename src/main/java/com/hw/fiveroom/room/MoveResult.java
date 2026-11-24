package com.hw.fiveroom.room;

public record MoveResult(
        String requestId,
        String playerId,
        int row,
        int col,
        int sequence,
        boolean duplicate
) {
}
