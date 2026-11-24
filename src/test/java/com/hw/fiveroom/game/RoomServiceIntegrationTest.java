package com.hw.fiveroom.game;

import com.hw.fiveroom.room.Room;
import com.hw.fiveroom.room.RoomManager;
import com.hw.fiveroom.room.RoomService;
import com.hw.fiveroom.room.RoomStatus;
import com.hw.fiveroom.room.dto.CreateRoomRequest;
import com.hw.fiveroom.room.dto.CreateRoomResponse;
import com.hw.fiveroom.room.dto.JoinRoomRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

public class RoomServiceIntegrationTest {
    @Test
    void gameWinUpdateRoomStatus(){
        RoomManager roomManager=new RoomManager();
        GameEngine gameEngine=new GameEngine(new WinChecker());
        RoomService roomService=new RoomService(roomManager,gameEngine);
//创建房间，加入两名成员开始游戏
        CreateRoomResponse host=roomService.createRoom(new CreateRoomRequest("clarie"));
        String roomId=host.roomId();
        CreateRoomResponse guest=roomService.joinRoom(roomId,new JoinRoomRequest("player2"));

        Room room1=roomManager.getRoom(roomId);
        assertNotNull(room1,"创建后应该能找到房间");
        assertEquals(2,room1.getPlayerSnapshot().size(),"开始游戏前得有两名玩家");

        roomService.startRoom(roomId,host.playerId(),host.playerToken());


//        黑棋白旗交替落子,黑棋在第七行行成横向的五连
        for (int col=3;col<=6;col++){
            roomService.makeMove(roomId,host.playerId(), host.playerToken(), 7,col,"black-" + col);
            roomService.makeMove(roomId, guest.playerId(), guest.playerToken(),8,col,"white-" + col);
        }
        roomService.makeMove(roomId, host.playerId(), host.playerToken(), 7,7,"black-win" );
        Room room=roomManager.getRoom(roomId);
        assertNotNull(room);
        assertEquals(GameStatus.FINISHED,room.getGame().getGameStatus());
        assertEquals(host.playerId(),room.getGame().getWinnerId());
        assertEquals(9,room.getGame().getMoveCount());
//        查看room的状态
        assertEquals(RoomStatus.FINISHED,room.getStatus());
//        结束后不可以落子
        ResponseStatusException exception=assertThrows(
                ResponseStatusException.class,
                ()->roomService.makeMove(roomId, guest.playerId(),guest.playerToken(),8,7,"white-after-finish")
        );
        assertEquals(409,exception.getStatusCode().value());
    }
}
