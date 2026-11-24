package com.hw.fiveroom.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @NotBlank(message="昵称不能为空")
        @Size(min=2,max=12,message="昵称长度为2到12个字符")
        String nickname
) {
}
