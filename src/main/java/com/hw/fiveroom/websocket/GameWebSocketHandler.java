
package com.hw.fiveroom.websocket;

import com.hw.fiveroom.room.MoveResult;
import com.hw.fiveroom.room.RoomService;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.json.JsonMapper;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log= LoggerFactory.getLogger(GameWebSocketHandler.class);
    private SessionManager sessionManager;
    private  RoomService roomService;
    private JsonMapper jsonMapper;
    public GameWebSocketHandler(
            SessionManager sessionManager,
            RoomService roomService,
            JsonMapper jsonMapper
    ){
        this.sessionManager=sessionManager;
        this.roomService=roomService;
        this.jsonMapper=jsonMapper;

    }


    @Override
    public void afterConnectionEstablished(
            WebSocketSession session) throws Exception {

        sessionManager.addSession(session);
        log.info("WebSocket连接成功，sessionId={}，当前连接数={}",session.getId(),sessionManager.getOnlineCount());

        session.sendMessage(
                new TextMessage("Welcome to FiveRoom!")
        );
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status) {
        var removed=sessionManager.removeSession(session.getId());
        if (removed.wasCurrent()&&removed.binding()!=null){
            var binding=removed.binding();

            boolean changed=false;
            if(!sessionManager.hasActivePlayerSession(binding.playerId())){
                changed=roomService.updatePlayerOnline(binding.roomId(),binding.playerId(),false);
            }

            if(changed){
                broadcastPlayerStatus(binding.roomId(), binding.playerId(),false);
            }
        }

        log.info("WebSocket连接关闭，sessionId={}，当前连接数={}",session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session,Throwable e)throws Exception{
        log.warn("websocket连接异常，sessionId={}",session.getId(),e);
        if(session.isOpen()){
            session.close(CloseStatus.SERVER_ERROR);
        }
//        sessionManager.removeSession(session.getId());
    }


    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message
    )throws Exception{
        String payload=message.getPayload();
//        只读取信息类型不处理业务内容
        tools.jackson.databind.JsonNode node;

        try{
            node=jsonMapper.readTree(payload);
        }catch (Exception e){
            session.sendMessage(new TextMessage("INVALID_MESSAGE"));
            return;
        }
        if (node == null || !node.has("type")) {
            session.sendMessage(new TextMessage("INVALID_MESSAGE"));
            return;
        }
        String type=node.get("type").asText();
        switch (type){
            case "AUTH" ->handleAuth(session,payload);
            case "MOVE" ->handleMove(session,payload);
            default -> session.sendMessage(new TextMessage("UNSUPPORTED_MESSAGE"));

        }

    }

    private void handleAuth(
            WebSocketSession session,
            String payload
    )throws Exception{
        AuthMessage auth;
        try{
            auth=jsonMapper.readValue(payload,AuthMessage.class);
        }catch (Exception e){
            session.sendMessage(new TextMessage("INVALID_MESSAGE"));
            return;
        }
        if (auth == null || !"AUTH".equals(auth.type())) {
            session.sendMessage(new TextMessage("INVALID_MESSAGE"));
            return;
        }
        if(sessionManager.getBinding(session.getId())!=null){
            session.sendMessage(new TextMessage("ALREADY_AUTHENTICATED"));
            return;
        }
        try{
            roomService.authenticatePlayer(auth.roomId(), auth.playerId(), auth.playerToken());
            sessionManager.bindPlayer(session.getId(), auth.roomId(), auth.playerId(),auth.playerToken());

            boolean changed=roomService.updatePlayerOnline(auth.roomId(),auth.playerId(),true);
            sessionManager.sendText(session,"AUTH_SUCCESS");
            //            认证成功后，返回当前房间完整快照
            RoomSnapshotMessage snapshot=roomService.getRoomSnapshot(auth.roomId(), auth.playerId());
            sessionManager.sendText(session,jsonMapper.writeValueAsString(snapshot));
            if(changed){
                broadcastPlayerStatus(auth.roomId(),auth.playerId(),true);
            }


        }catch (org.springframework.web.server.ResponseStatusException | IllegalStateException e){
            session.sendMessage(new TextMessage("AUTH_FAILED"));


        }
    }

    private void handleMove(
            WebSocketSession session,String payload
    )throws Exception{
        SessionManager.PlayerBinding binding=sessionManager.getBinding(session.getId());
//        未认证的链接不可以落子
        if(binding==null){
            session.sendMessage(new TextMessage("AUTH_REQUIRED"));
            return;
        }
        MoveMessage move;
        try{
            move=jsonMapper.readValue(payload,MoveMessage.class);
        }catch (Exception e){
            session.sendMessage(new TextMessage("INVALID_MESSAGE"));
            return;
        }
        if (move==null || move.row()==null || move.col()==null || move.roomId()==null
                ||!move.roomId().equals(binding.roomId())){
            session.sendMessage(new TextMessage("INVALID_MOVE"));
            return;
        }
        // 不信任客户端传来的玩家 ID
        // 只使用经过 AUTH 验证的绑定身份
        String playerId= binding.playerId();
        // MVP 先从房间中获取身份 Token
        // 后续改为独立的认证上下文
//        String playerToken=roomService.getPlayerTokenForSession(binding.roomId(),playerId);
//        try{
//            roomService.makeMove(binding.roomId() ,playerId,playerToken, move.row(),move.col());
//            session.sendMessage(new TextMessage("MOVE_SUCCESS"));
//        }catch (org.springframework.web.server.ResponseStatusException e){
//            session.sendMessage(new TextMessage("MOVE_REJECTED"));
//        }
        // 使用 AUTH 成功后保存的身份，不使用客户端自报的玩家身份

        try {
            MoveResult result=roomService.makeMove(
                    binding.roomId(),
                    binding.playerId(),
                    binding.playerToken(),
                    move.row(),
                    move.col(),
                    move.requestId()
            );

            if(result.duplicate()){
                sessionManager.sendText(session,"MOVE_DUPLICATE");
                return;
            }

            MoveAppliedMessage event =new MoveAppliedMessage(
                    "MOVE_APPLIED", move.requestId(), binding.roomId(),
                    binding.playerId(),move.row(), move.col(),result.sequence()
            );
            String json=jsonMapper.writeValueAsString(event);
            val roomDetail=roomService.getRoomDetail(binding.roomId());
            for(var player : roomDetail.players()){
                try{
                    sessionManager.sendToPlayer(binding.roomId(), player.playerId(),json);
                }catch (java.io.IOException e){
                    log.warn("落子广播失败：roomId={}, playerId={}",binding.roomId(),player.playerId(),e);
                }
            }
            //            如果本次路子结束的游戏继续广播game over
            broadcastGameOver(binding.roomId());
            session.sendMessage(new TextMessage("MOVE_SUCCESS"));

        } catch (ResponseStatusException e) {
            sendMoveRejected(session, move.requestId(),e.getStatusCode().value(),
                    e.getReason()==null?"落子失败":e.getReason());
        }
    } // handleMove 方法结束

    private void sendMoveRejected(
            WebSocketSession session,String requestId,
            int status,String reason
    ) throws Exception{
        MoveRejectedMessage event=new MoveRejectedMessage(
                "MOVE_REJECTED",requestId,status,reason
        );
        sessionManager.sendText(session,jsonMapper.writeValueAsString(event));
    }

    private void broadcastGameOver(String roomId) throws Exception{
        var room=roomService.getRoomDetail(roomId);
        if(!"FINISHED".equals(room.status())){
            return;
        }
        var game=roomService.getGameSnapshot(roomId);
        GameOverMessage event=new GameOverMessage(
                "GAME_OVER",roomId,game.winnerId(),game.winnerId()==null?"DRAW":"WIN"
        );
        String json =jsonMapper.writeValueAsString(event);

        for(var player:room.players()){
            try{
                sessionManager.sendToPlayer(roomId, player.playerId(), json);
            }catch (java.io.IOException e){
                log.warn("游戏结束广播失败：roomId={}, playerId={}",roomId,player.playerId(),e);
            }
        }
    }

    private void broadcastPlayerStatus(
            String roomId,String playerId,boolean online
    ){
        try{
            var room=roomService.getRoomDetail(roomId);

            String json=jsonMapper.writeValueAsString(
                    java.util.Map.of(
                            "type",online?"PLAYER_ONLINE":"PLAYER_OFFLINE",
                            "roomId",roomId,"playerId",playerId
                    )
            );

            for (var player:room.players()){
                try{
                    sessionManager.sendToPlayer(roomId,player.playerId(),json);
                }catch (java.io.IOException e){
                    log.warn("玩家状态广播失败,roomId={}",roomId,e);
                }
            }
        }catch (Exception e){
            log.warn("玩家状态广播处理失败,roomId={}",roomId,e);
        }
    }

    }

