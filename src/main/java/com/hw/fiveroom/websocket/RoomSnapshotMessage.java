package com.hw.fiveroom.websocket;

public record RoomSnapshotMessage(
        String type,
        String roomId,
        String roomStatus,
        String gameStatus,
        String blackPlayerId,
        String whitePlayerId,
        String currentTurn,
        int moveCount,
        int sequence,
        String winnerId,
        String[][] board
) {
}
