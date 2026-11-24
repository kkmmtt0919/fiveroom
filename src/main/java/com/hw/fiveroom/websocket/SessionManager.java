package com.hw.fiveroom.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionManager {
    // sessionId -> WebSocketSession
    private final Map<String, WebSocketSession> sessions=new ConcurrentHashMap<>();

//    注册链接
    public void addSession(WebSocketSession session){
        sessions.put(session.getId(),session);
    }
//    查询链接
    public WebSocketSession getSession(String sessionId){
        return sessions.get(sessionId);
    }
//    移除链接
    public record RemoveSession (
            PlayerBinding binding,boolean wasCurrent
){
    }
    public synchronized RemoveSession removeSession(String sessionId){
        sessions.remove(sessionId);
        PlayerBinding binding=bindings.remove(sessionId);
        if(binding==null){
            return new RemoveSession(null,false);
        }
//        只有当前有效链接才可以清除玩家的映射
        boolean wasCurrent=playerSessions.remove(binding.playerId,sessionId);
        return new RemoveSession(binding,wasCurrent);
    }
//    public synchronized void removeSession(String sessionId){
//        sessions.remove(sessionId);
//        PlayerBinding binding=bindings.remove(sessionId);
//        if(binding!=null){
//            playerSessions.remove(binding.playerId(),sessionId);
//        }
//    }
//    当前连接数
    public int getOnlineCount(){
        return sessions.size();
    }

    public void sendText(
            WebSocketSession session,String message
    ) throws IOException{
        synchronized (session){
            if(session.isOpen()){
                session.sendMessage(new TextMessage(message));
            }
        }
    }

    public record PlayerBinding(String roomId,String playerId,String playerToken){

    }
    private final Map<String,PlayerBinding> bindings=new ConcurrentHashMap<>();
    private final Map<String,String> playerSessions=new ConcurrentHashMap<>();

    public synchronized void bindPlayer(
            String sessionId,String roomId,String playerId,String playerToken
    ){
        if(!sessions.containsKey(sessionId)){
            throw new IllegalStateException("WebSocket连接不存在");
        }
        PlayerBinding existing =bindings.get(sessionId);
        if(existing!=null){
            throw new IllegalStateException("该连接已绑定玩家");
        }
//        同一个玩家只保留最新的身份绑定
        String oldSessionId=playerSessions.put(playerId,sessionId);
        if(oldSessionId!=null){
            bindings.remove(oldSessionId);
        }
        bindings.put(sessionId,new PlayerBinding(roomId,playerId,playerToken));
    }

    public synchronized PlayerBinding getBinding(String sessionId){
        PlayerBinding binding=bindings.get(sessionId);
        if (binding == null) {
            return null;
        }

// 走到这里，才能确定 binding 不为空
        String currentSessionId =
                playerSessions.get(binding.playerId());

        if (!sessionId.equals(currentSessionId)) {
            return null;
        }
        return binding;
    }
    public WebSocketSession getplayerSessions(String playerId){
        String sessionId=playerSessions.get(playerId);
        if(sessionId==null){
            return null;
        }
        return sessions.get(sessionId);
    }

//    向指定玩家发送消息
    public void sendToPlayer(
            String roomId,String playerId,String message
    )throws IOException{
        WebSocketSession session=getplayerSessions(playerId);

        if (session==null || !session.isOpen()){
            return;
        }
        PlayerBinding binding=getBinding(session.getId());

        if(binding==null || !binding.roomId().equals(roomId)
        || !binding.playerId().equals(playerId)){
            return;
        }
        synchronized (session){
            if(session.isOpen()){
                session.sendMessage(new TextMessage(message));
            }
        }
    }

    public synchronized boolean inCurrentPlayerSession(
            String sessionId,String playerId
    ){
        return sessionId.equals(playerSessions.get(playerId));
    }

    public synchronized boolean hasActivePlayerSession(String playerId){
        String sessionId=playerSessions.get(playerId);

        if(sessionId==null){
            return false;
        }
        WebSocketSession session=sessions.get(sessionId);
        return session!=null&&session.isOpen();
    }
}
