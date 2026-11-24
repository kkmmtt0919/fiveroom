package com.hw.fiveroom.game;

import org.springframework.stereotype.Component;

@Component
public class WinChecker {
    private static final int[][] DIRECTIONS={{0,1},{1,0},{1,1},{1,-1}};
//    判断最后一次落子是否形成5连
    public boolean isWinningMove(
            Board board,int row,int col,Chess chess){
        if(chess==null||chess==Chess.EMPTY){
            throw new IllegalArgumentException("棋子类型无效哦");
        }
//        确认该位置确实是当前棋子
        if (board.getChess(row,col)!=chess){
            return false;
        }
//        逐个检查四个方向
        for(int[] direction:DIRECTIONS){
            int rowDirection=direction[0];
            int colDirection=direction[1];

            int count=1;
//            检查正反两个方向
            count+=countDirection(board,row,col,rowDirection,colDirection,chess);
            count+=countDirection(board,row,col,-rowDirection,-colDirection,chess);
            if (count>=5){
                return true;
            }
        }
        return false;
    }
    private int countDirection(
            Board board,int row,int col,int rowDirection,int colDirection,Chess chess
    ){
        int count=0;
        int currentRow=row+rowDirection;
        int currentCol=col+colDirection;
        while (
                currentRow>=0 && currentRow<Board.SIZE &&
                        currentCol>=0 && currentCol<Board.SIZE&&
                        board.getChess(currentRow,currentCol)==chess
        ){
            count++;
            currentRow+=rowDirection;
            currentCol+=colDirection;
        }
        return count;
    }
}
