package com.hw.fiveroom.room;

import com.hw.fiveroom.game.Game;
import com.hw.fiveroom.game.GameEngine;
import com.hw.fiveroom.player.Player;
import com.hw.fiveroom.room.dto.*;
import com.hw.fiveroom.websocket.RoomSnapshotMessage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

@Service
public class RoomService {
    private final RoomManager roomManager;
    private final GameEngine gameEngine;
    private final SecureRandom random=new SecureRandom();
    public RoomService(RoomManager roomManager, GameEngine gameEngine){
        this.roomManager=roomManager;
        this.gameEngine = gameEngine;
    }

    public CreateRoomResponse createRoom(CreateRoomRequest request){
        String playerId = UUID.randomUUID().toString();
        String playerToken=UUID.randomUUID().toString();
        Player host =new Player(
                playerId,
                request.nickname(),
                playerToken
        );
        String roomId;
        while (true){
            roomId=String.format("%06d",random.nextInt(1_000_000));
            Room room=new Room(roomId,host);
            if (roomManager.addRoom(room)){
                break;
            }
        }
        return new CreateRoomResponse(
                roomId,playerId,playerToken,RoomStatus.WAITING.name()
        );
    }

    public CreateRoomResponse joinRoom(
            String roomId,
            JoinRoomRequest request
    ){
        Room room=roomManager.getRoom(roomId);
        if(room==null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"房间不存在");
        }
        String playerId=UUID.randomUUID().toString();
        String playerToken=UUID.randomUUID().toString();
        Player player=new Player(playerId, request.nickname(), playerToken);

        try{
            room.addPlayer(player);
        }catch (IllegalStateException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT,e.getMessage());
        }
        return new CreateRoomResponse(roomId,playerId,playerToken,room.getStatus().name());
    }

    public RoomDetailResponse getRoomDetail(String roomId){
        Room room=roomManager.getRoom(roomId);
        if(room==null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"房间不存在");
        }
        List<PlayerInfoResponse> players=room.getPlayerSnapshot()
                .stream().map(player -> new PlayerInfoResponse(
                        player.getPlayerId(),
                        player.getNickname(),
                        player.isOnline()
                )).toList();

        return new RoomDetailResponse(
                room.getRoomId(),
                room.getHostPlayerId(),
                room.getStatus().name(),
                players,
                room.getCreatAt()
        );
    }
    public RoomDetailResponse startRoom(
            String roomId,
            String playerId,
            String playerToken
    ){
        Room room =roomManager.getRoom(roomId);
        if(room==null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"房间不存在");
        }
        try{
            room.startGame(playerId,playerToken);
        }catch (SecurityException e){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,e.getMessage());
        }catch (IllegalStateException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT,e.getMessage());
        }
        return getRoomDetail(roomId);
    }


    public MoveResult makeMove(
            String roomId,String playerId,String playerToken,int row,int col,
            String requestId){
        if(requestId==null || requestId.isBlank() || requestId.length()>64){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"requestId无效");
        }
        Room room=roomManager.getRoom(roomId);
        if(room==null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"房间不存在哟");
        }
//        同一房间内落子,状态同步必须保持原子性
        synchronized (room){

            Player player=room.getPlayerSnapshot().stream()
                    .filter(p->p.getPlayerId().equals(playerId))
                    .findFirst()
                    .orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"玩家不属于该房间"));
            if(!player.getPlayerToken().equals(playerToken)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,"身份凭证无效");
            }
            Game game=room.getGame();

            if(game==null){
                throw new ResponseStatusException(HttpStatus.CONFLICT,"对局尚未初始化");
            }

            String requestKey =playerId +":"+requestId;
            MoveResult previous=room.getProcessedMove(requestKey);
            if(previous!=null){
                if(previous.row()!=row || previous.col()!=col){
                    throw new ResponseStatusException(HttpStatus.CONFLICT,"同一 requestId 不能对应不同落子");
                }
                return new MoveResult(
                        previous.requestId(),previous.playerId(),previous.row(),previous.col(),previous.sequence(),true
                );
            }
            if(room.getStatus()!=RoomStatus.PLAYING){
                throw new ResponseStatusException(HttpStatus.CONFLICT,"房间不在对局中哟");
            }

            try{
                gameEngine.makeMove(game,playerId,row,col);
            }catch (IllegalArgumentException e){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage());
            }catch (IllegalStateException e){
                throw new ResponseStatusException(HttpStatus.CONFLICT,e.getMessage());
            }
            room.syncGameStatus();
            MoveResult result=new MoveResult(requestId,playerId,row,col,game.getMoveCount(),false);
            room.saveProcessedMove(requestKey,result);
            return result;
        }

    }

    public void authenticatePlayer(
            String roomId,String playerId,String playerToken
    ){
        Room room=roomManager.getRoom(roomId);
        if(room==null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"房间不存在");
        }
        synchronized (room){
            Player player=room.getPlayerSnapshot().stream()
                    .filter(p->p.getPlayerId().equals(playerId))
                    .findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"玩家不属于该房间"));
            if(playerToken==null||!player.getPlayerToken().equals(playerToken)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,"身份凭证无效");
            }
        }
    }

    public GameResultResponse getGameSnapshot(String roomId) {
        Room room=roomManager.getRoom(roomId);
        if (room==null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"房间不存在");
        }
        synchronized (room){
            Game game=room.getGame();
            if(game==null){
                throw new ResponseStatusException(HttpStatus.CONFLICT,"游戏尚未开始");
            }
            return new GameResultResponse(game.getGameStatus().name(),game.getWinnerId());
        }
    }

    public boolean updatePlayerOnline(
            String roomId,String playerId,boolean online
    ){
        Room room=roomManager.getRoom(roomId);
        if (room==null){
            return false;
        }
        synchronized (room){
            Player player=room.getPlayerSnapshot().stream()
                    .filter(p->p.getPlayerId().equals(playerId))
                    .findFirst().orElse(null);
            if (player==null){
                return false;
            }
            if (player.isOnline()==online){
                return false;
            }
            player.setOnline(online);
            return true;

        }
    }

    public RoomSnapshotMessage getRoomSnapshot(
            String roomId,String playerId
    ){
        Room room=roomManager.getRoom(roomId);
        if(room==null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"房间不存在");
        }
        synchronized (room){
            boolean isMember=room.getPlayerSnapshot().stream()
                    .anyMatch(p->p.getPlayerId().equals(playerId));
            if(!isMember){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,"玩家不属于该房间");
            }
            Game game=room.getGame();
//            尚未开始游戏，也允许返回房间快照
            if(game==null){
                return new RoomSnapshotMessage(
                        "ROOM_SNAPSHOT",roomId,room.getStatus().name(),
                        null,null,null,null,
                        0,0,null,null
                );
            }
            synchronized (game){
                return new RoomSnapshotMessage(
                        "ROOM_SNAPSHOT",roomId,room.getStatus().name(),
                        game.getGameStatus().name(),
                        game.getBlackPlayerId(),game.getWhitePlayerId(),
                        game.getCurrentTurn().name(),
                        game.getMoveCount(),game.getMoveCount(),game.getWinnerId(),
                        game.getBoard().getSnapshot()
                );
            }
        }

    }
}
