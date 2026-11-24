package com.hw.fiveroom.player;

public class Player {


    private final String playerId;
    private final String nickname;
    private final String playerToken;

    private volatile boolean online;
    public Player(String playerId,String nickname,String playerToken){
        this.playerId=playerId;
        this.nickname=nickname;
        this.playerToken=playerToken;
        this.online=true;
    }
     public String getPlayerId(){
        return playerId;
     }
    public String getNickname() {
        return nickname;
    }

    public String getPlayerToken() {
        return playerToken;
    }

    public boolean isOnline() {
        return online;
    }
    public void setOnline(boolean online){
        this.online=online;
    }
}
