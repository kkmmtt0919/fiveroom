package com.hw.fiveroom.game;

import org.springframework.stereotype.Component;

@Component
public class GameEngine {
    private final WinChecker winChecker;
    public GameEngine(WinChecker winChecker){
        this.winChecker=winChecker;
    }

    public void makeMove(
            Game game,String playerId,int row,int col
    ){
//        对game对象使用同步锁，同一局的玩家即使同时发送落子请求，也不会通过回合检查并修改棋盘；不同对局的game对象不同，不会因为一盘棋局操作而锁住其他房间
        synchronized (game){
            if(game.isFinished()){
                throw new IllegalStateException("游戏已经结束了哟");
            }
        }
        Chess playerChess;
        if(game.getBlackPlayerId().equals(playerId)){
            playerChess=Chess.BLACK;
        }else if (game.getWhitePlayerId().equals(playerId)){
            playerChess=Chess.WHITE;
        }else{
            throw new IllegalArgumentException("玩家不属于当前对局");
        }

//        检查是否轮到该玩家
        if(game.getCurrentTurn()!=playerChess){
            throw new IllegalStateException("当前不是该玩家的回合哟~");
        }
//        获取棋盘并放置棋子
        Board board=game.getBoard();
        board.place(row,col,playerChess);

//        成功落子先记录次数
        game.recordMove();

//        新增检查胜负
        if(winChecker.isWinningMove(board,row,col,playerChess)){
            game.finish(playerId);
            return;
        }
        if(board.isFull()){
            game.finishDraw();
            return;
        }
//        更新落子的次数和回合,没有胜者就切换回合
        game.switchTurn();
    }
}
