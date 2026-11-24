package com.hw.fiveroom.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WinCheckerTest {
    private final WinChecker winChecker=new WinChecker();

    @Test
    void horizontalFiveWins(){
        Board board=new Board();
        for (int col=3;col<=7;col++){
            board.place(7,col,Chess.BLACK);
        }
        assertTrue(
                winChecker.isWinningMove(board,7,7,Chess.BLACK)
        );
    }
    @Test
    void verticalFiveWins(){
        Board board=new Board();
        for (int row=3;row<=7;row++){
            board.place(row,5,Chess.BLACK);
        }
        assertTrue(
                winChecker.isWinningMove(board,7,5,Chess.BLACK)
        );
    }
    @Test
    void diagonalFiveWins(){
        Board board=new Board();
        for (int i=1;i<=5;i++){
            board.place(3+i,3+i,Chess.BLACK);
        }
        assertTrue(
                winChecker.isWinningMove(board,7,7,Chess.BLACK)
        );
    }
    @Test
    void antidiagonalFiveWins(){
        Board board=new Board();
        for (int i=1;i<=5;i++){
            board.place(3+i,7-i,Chess.BLACK);
        }
        assertTrue(
                winChecker.isWinningMove(board,7,3,Chess.BLACK)
        );
    }
    @Test
    void fournotWins(){
        Board board=new Board();
        for (int col=3;col<=6;col++){
            board.place(7,col,Chess.BLACK);
        }
        assertFalse(
                winChecker.isWinningMove(board,7,6,Chess.BLACK)
        );
    }
}
