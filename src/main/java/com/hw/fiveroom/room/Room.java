package com.hw.fiveroom.room;

import com.hw.fiveroom.game.Game;
import com.hw.fiveroom.player.Player;
import org.testng.collections.Maps;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Room {
    private final String roomId;
    private final String hostPlayerId;
    private final List< Player> players;
    private RoomStatus status;
    private final LocalDateTime creatAt;

    private Game game;

    public Game getGame() {
        return game;
    }

    public Room(String roomId, Player host){
        this.roomId=roomId;
        this.hostPlayerId= host.getPlayerId();
        this.players=new ArrayList<>();
        this.players.add(host);
        this.status=RoomStatus.WAITING;
        this.creatAt=LocalDateTime.now();

    }

    public String getHostPlayerId() {
        return hostPlayerId;
    }


    public String getRoomId() {
        return roomId;
    }
    public List<Player> getPlayers(){
        return players;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatAt() {
        return creatAt;
    }
    public void setStatus(RoomStatus status){
        this.status=status;
    }
//上同步锁，同一时间只许一个线程进入
    public synchronized void addPlayer(Player player){
        if (status !=RoomStatus.WAITING){
            throw new IllegalStateException("当前房间不允许加入");
        }
        if (players.size()>=2){
            throw new IllegalStateException("房间已满");
        }
        players.add(player);
    }

    public synchronized  List<Player> getPlayerSnapshot(){
// 不使用原来的get players，而是返回一个不可修改的快照，防止其他意外操作改变房间数据，也降低并发读取时的问题
        return List.copyOf(players);
    }

//    让游戏开始与加入房间的操作，不会同时修改房间的状态使用同步锁
    public synchronized void startGame(String playerId,String playerToken){
        if (status!=RoomStatus.WAITING){
            throw new IllegalStateException("该房间不能开始游戏");
        }
        if (players.size() !=2){
            throw new IllegalStateException("必须有两名玩家才可以开始游戏");
        }
        //        检查玩家的身份
        Player player=players.stream()
                .filter(p->p.getPlayerId().equals(playerId))
                .findFirst()
                .orElseThrow(()->new SecurityException("玩家不属于该房间"));

        if (!player.getPlayerToken().equals(playerToken)){
            throw new SecurityException("身份凭证无效");
        }
//        检查是否为房主？
        if(!hostPlayerId.equals(playerId)){
            throw new SecurityException("只有房主才可以开始游戏");
        }
//        status=RoomStatus.PLAYING;
//        房主黑棋
        String whitePlayerId=players.stream()
                .filter(player1 -> !player1.getPlayerId().equals(hostPlayerId))
                .findFirst().orElseThrow(()->new IllegalStateException("未找到第二位玩家"))
                .getPlayerId();
//        创建新游戏,更新游戏状态
        this.game=new Game(hostPlayerId,whitePlayerId);
        this.status=RoomStatus.PLAYING;

    }
    public synchronized void syncGameStatus(){
        if(game==null){
            return;
        }
        if(game.isFinished()){
            this.status=RoomStatus.FINISHED;
        }
    }

    private final Map<String,MoveResult>processedMoves=new HashMap<>();
    MoveResult getProcessedMove(String requestKey){
        return processedMoves.get(requestKey);
    }
    public void saveProcessedMove(String requestKey,MoveResult result){
        processedMoves.put(requestKey,result);
    }
}
