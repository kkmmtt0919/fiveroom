package com.hw.fiveroom.room.dto;

public record CreateRoomResponse (
        String roomId,
        String playerId,
        String playerToken,
        String status
)
{
}
