package com.hw.fiveroom.game;

import com.hw.fiveroom.room.MoveResult;
import com.hw.fiveroom.room.RoomManager;
import com.hw.fiveroom.room.RoomService;
import com.hw.fiveroom.room.RoomStatus;
import com.hw.fiveroom.room.dto.CreateRoomRequest;
import com.hw.fiveroom.room.dto.CreateRoomResponse;
import com.hw.fiveroom.room.dto.JoinRoomRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class RoomConcurrencyTest {
    private final RoomManager roomManager=new RoomManager();
    private final RoomService roomService=new RoomService(
            roomManager,new GameEngine(new WinChecker())
    );
    private CreateRoomResponse createPlayingRoom(){
        var host =roomService.createRoom(new CreateRoomRequest("Claire"));
        roomService.joinRoom(host.roomId(),new JoinRoomRequest("Player2"));
        roomService.startRoom(
                host.roomId(),host.playerId(),host.playerToken()
        );
        return host;
    }

    @Test
    void concurrentMoveOnlyOneSucceeds() throws Exception{
        var host=createPlayingRoom();
        ExecutorService executor= Executors.newFixedThreadPool(2);
        CountDownLatch start=new CountDownLatch(1);

        Callable<Boolean> move1=()->{
            start.await();
            try{
                roomService.makeMove(host.roomId(),host.playerId(),host.playerToken(),7,7,"req-A");
                return true;
            }catch (ResponseStatusException e){
                return false;
            }
        };
        Callable<Boolean>move2=()->{
            start.await();;
            try{
                roomService.makeMove(
                        host.roomId(), host.playerId(), host.playerToken(), 7,8,"req-B"
                );
                return true;
            }catch (ResponseStatusException e){
                return false;
            }
        };
        try {
            Future<Boolean> result1=executor.submit(move1);
            Future<Boolean> result2=executor.submit(move2);
            start.countDown();
            int successCount=(result1.get(5,TimeUnit.SECONDS)?1:0)+(result2.get(5,TimeUnit.SECONDS)?1:0);
            assertEquals(1,successCount);
            var game=roomManager.getRoom(host.roomId()).getGame();
            assertEquals(1,game.getMoveCount());
        }finally {
            executor.shutdown();
        }
    }

    @Test
    void duplicateRequestOnlyExecutesOnce() throws Exception{
        var host=createPlayingRoom();
        ExecutorService executor=Executors.newFixedThreadPool(2);
        CountDownLatch start=new CountDownLatch(1);

        Callable<MoveResult> task=()->{
            start.await();

            return roomService.makeMove(
                    host.roomId(),host.playerId(),host.playerToken(),7,7,"same-request"
            );
        };
        try{
            Future<MoveResult> first=executor.submit(task);
            Future<MoveResult> second=executor.submit(task);
            start.countDown();
            MoveResult result1=first.get(5,TimeUnit.SECONDS);
            MoveResult result2=second.get(5,TimeUnit.SECONDS);

            assertNotEquals(result1.duplicate(),result2.duplicate());
            assertEquals(1,result1.sequence());
            assertEquals(1,result2.sequence());
            assertEquals(1,roomManager.getRoom(host.roomId()).getGame().getMoveCount());

        }finally {
            executor.shutdown();
        }
    }

    @Test
    void differentRoomsDoNotInterfere() {

        // 创建两间独立的游戏房间
        var hostA = createPlayingRoom();
        var hostB = createPlayingRoom();

        // 房间 A 落子
        roomService.makeMove(
                hostA.roomId(),
                hostA.playerId(),
                hostA.playerToken(),
                7, 7, "room-a-move"
        );

        // 房间 B 落子
        roomService.makeMove(
                hostB.roomId(),
                hostB.playerId(),
                hostB.playerToken(),
                8, 8, "room-b-move"
        );

        var gameA = roomManager.getRoom(hostA.roomId()).getGame();
        var gameB = roomManager.getRoom(hostB.roomId()).getGame();

        assertNotSame(gameA, gameB);
        assertNotSame(gameA.getBoard(), gameB.getBoard());

        assertEquals(1, gameA.getMoveCount());
        assertEquals(1, gameB.getMoveCount());

        assertEquals(
                Chess.BLACK,
                gameA.getBoard().getChess(7, 7)
        );

        assertEquals(
                Chess.BLACK,
                gameB.getBoard().getChess(8, 8)
        );

        // 房间 A 的落子不会出现在房间 B
        assertEquals(
                Chess.EMPTY,
                gameB.getBoard().getChess(7, 7)
        );
    }

    @Test
    void finishedRoomRejectsFurtherMoves() {

        var host = createPlayingRoom();

        // 获取第二位玩家
        var room = roomManager.getRoom(host.roomId());

        var guest = room.getPlayerSnapshot().stream()
                .filter(p -> !p.getPlayerId().equals(host.playerId()))
                .findFirst()
                .orElseThrow();

        // 黑棋横向五连，白棋正常交替落子
        for (int col = 3; col <= 6; col++) {
            roomService.makeMove(
                    host.roomId(), host.playerId(),
                    host.playerToken(), 7, col,
                    "black-" + col
            );

            roomService.makeMove(
                    host.roomId(), guest.getPlayerId(),
                    guest.getPlayerToken(), 8, col,
                    "white-" + col
            );
        }

        roomService.makeMove(
                host.roomId(), host.playerId(),
                host.playerToken(), 7, 7,
                "winning-move"
        );

        assertEquals(RoomStatus.FINISHED, room.getStatus());
        assertEquals(9, room.getGame().getMoveCount());

        // 游戏结束后，新的落子请求应被拒绝
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> roomService.makeMove(
                        host.roomId(), guest.getPlayerId(),
                        guest.getPlayerToken(), 8, 7,
                        "after-game-over"
                )
        );

        assertEquals(409, exception.getStatusCode().value());

        // 拒绝操作不能改变棋盘和落子次数
        assertEquals(9, room.getGame().getMoveCount());
        assertEquals(
                Chess.EMPTY,
                room.getGame().getBoard().getChess(8, 7)
        );

        // 已经成功的获胜请求仍可幂等重试
        MoveResult retry = roomService.makeMove(
                host.roomId(), host.playerId(),
                host.playerToken(), 7, 7,
                "winning-move"
        );

        assertTrue(retry.duplicate());
        assertEquals(9, retry.sequence());
    }

}
