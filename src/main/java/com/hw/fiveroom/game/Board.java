package com.hw.fiveroom.game;

import java.lang.reflect.Array;
import java.util.Arrays;

public class Board {
    public static final int SIZE =15;
    private final Chess[][] cells;

    public Board(){
        cells =new Chess[SIZE][SIZE];
        for(Chess[] row:cells){
            Arrays.fill(row,Chess.EMPTY);
        }
    }
    private void checkPosition(int row,int col){
        if (row<0 ||row>=SIZE || col<0 || col>=SIZE){
            throw new IllegalArgumentException("落子位置超出棋盘范围哟~");
        }
    }

    public Chess getChess(int row,int col){
        checkPosition(row,col);
        return cells[row][col];
    }

    public void place(int row,int col,Chess chess){
        checkPosition(row,col);
        if(chess ==Chess.EMPTY){
            throw new IllegalArgumentException("不能放置空棋子哟！");
        }
        if(cells[row][col]!=Chess.EMPTY){
            throw new IllegalArgumentException("该位置已经有棋子了");
        }
        cells[row][col]=chess;
    }
    public boolean isFull(){
        for (Chess[] row:cells){
            for(Chess chess:row){
                if (chess==Chess.EMPTY){
                    return false;
                }
            }
        }
        return true;
    }

    public String[][] getSnapshot(){
        String[][] snapshot=new String[SIZE][SIZE];
        for (int row=0;row<SIZE;row++){
            for(int col=0;col<SIZE;col++){
                snapshot[row][col]=cells[row][col].name();
            }
        }
        return snapshot;
    }

}
