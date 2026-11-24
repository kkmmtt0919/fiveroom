package com.hw.fiveroom.game;

import lombok.Data;

@Data
public class Game {
    private final Board board;
    private final String blackPlayerId;
    private final String whitePlayerId;

    private Chess currentTurn;

    private String winnerId;

    private int moveCount;
    private GameStatus gameStatus;

    public Game(String blackPlayerId,String whitePlayerId){
        if (blackPlayerId ==null || whitePlayerId==null||blackPlayerId.equals(whitePlayerId)){
            throw new IllegalArgumentException("对局中必须要有两名不同的玩家哟~");
        }
        this.board=new Board();
        this.blackPlayerId=blackPlayerId;
        this.whitePlayerId=whitePlayerId;

//        黑棋先手
        this.currentTurn=Chess.BLACK;
        this.winnerId=null;
        this.moveCount=0;
        this.gameStatus=GameStatus.PLAYING;
    }
////    没有写public表示，这个方法只允许同一个包中的类访问
//    void advanceTurn(){
//        moveCount++;
//        currentTurn=currentTurn==Chess.BLACK?Chess.WHITE:Chess.BLACK;
//    }
    void recordMove(){
        moveCount++;
    }
    void switchTurn(){
        currentTurn=currentTurn==Chess.BLACK?Chess.WHITE:Chess.BLACK;
    }
    void finish(String winnerId){
        this.winnerId=winnerId;
        this.gameStatus=GameStatus.FINISHED;
    }
    public boolean isFinished(){
        return gameStatus==GameStatus.FINISHED;
    }
//    区别获胜或无胜者的结局
    void finishDraw(){
        this.winnerId=null;
        this.gameStatus=GameStatus.FINISHED;
    }
//    public Board getBoard(){
//        return board;
//    }
//
//    public String getWhitePlayerId() {
//        return whitePlayerId;
//    }
//
//    public String getBlackPlayerId() {
//        return blackPlayerId;
//    }
//
//    public Chess getCurrentTurn() {
//        return currentTurn;
//    }
//
//    public String getWinnerId() {
//        return winnerId;
//    }
//
//    public int getMoveCount() {
//        return moveCount;
//    }
}
