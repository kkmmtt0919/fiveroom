package com.hw.fiveroom.room.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RoomDetailResponse (
        String roomId,
        String hostPlayerId,
        String status,
        List<PlayerInfoResponse> players,
        LocalDateTime createdAt
) {
}
