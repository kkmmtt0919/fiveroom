package com.hw.fiveroom.room.dto;

public record PlayerInfoResponse (
        String playerId,
        String nickname,
        boolean online
){
}
