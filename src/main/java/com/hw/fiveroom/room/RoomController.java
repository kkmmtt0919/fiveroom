package com.hw.fiveroom.room;

import com.hw.fiveroom.room.dto.CreateRoomRequest;
import com.hw.fiveroom.room.dto.CreateRoomResponse;
import com.hw.fiveroom.room.dto.JoinRoomRequest;
import com.hw.fiveroom.room.dto.RoomDetailResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
//处理HTTP请求的控制器
@RequestMapping("api/rooms")
//设置统一的接口路径前缀
public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService){
        this.roomService=roomService;
    }

    @PostMapping
    public CreateRoomResponse createRoom(
            @Valid @RequestBody CreateRoomRequest request
    ){
        return roomService.createRoom(request);
    }

    @PostMapping("/{roomId}/join")
    public CreateRoomResponse joinRoom(
            @PathVariable String roomId,
            @Valid @RequestBody JoinRoomRequest request
            ){
        return roomService.joinRoom(roomId,request);
    }

    @GetMapping("/{roomId}")
    public RoomDetailResponse getRoomDetail(
            @PathVariable String roomId
    ){
        return roomService.getRoomDetail(roomId);
    }

    @PostMapping("/{roomId}/start")
    public RoomDetailResponse startRoom(
            @PathVariable String roomId,
            @RequestHeader("X-Player-Id") String playerId,
            @RequestHeader("X-Player-Token") String playerToken
    ){
        return roomService.startRoom(
                roomId,playerId,playerToken
        );
    }
}
