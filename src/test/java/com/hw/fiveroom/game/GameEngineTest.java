package com.hw.fiveroom.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//简单的单元测试
public class GameEngineTest {
    private final WinChecker winChecker=new WinChecker();
    private final GameEngine engine=new GameEngine(winChecker);
//    测试黑棋第一步落在77的格子,预期结果为成功落子，并切换到白棋
//    正向测试
    @Test
    void blackMoveFirst(){
        Game game =new Game("player1","player2");
        engine.makeMove(game,"player1",7,7);
        assertEquals(Chess.BLACK,game.getBoard().getChess(7,7));
        assertEquals(Chess.WHITE,game.getCurrentTurn());
        assertEquals(1,game.getMoveCount());
    }
//    白棋试图抢先落子，预期被拒绝，棋盘不变
//    负向测试
    @Test
    void rejectWrongTurn(){
        Game game=new Game("player1","player2");
//        预期这里要抛一个异常，不抛则会报错
        assertThrows(
                IllegalStateException.class,
                ()->engine.makeMove(game,"player2",7,7)
        );
        assertEquals(0,game.getMoveCount());
    }

    @Test
    void gameEcdWhenPlayerWins(){
        Game game=new Game("player1","player2");
        Board board=game.getBoard();
        for(int col =3;col<=6;col++){
            board.place(7,col,Chess.BLACK);
        }

        engine.makeMove(game,"player1",7,7);
        assertEquals(GameStatus.FINISHED,game.getGameStatus());
        assertEquals("player1",game.getWinnerId());
        assertEquals(1,game.getMoveCount());

    }

    @Test
    void cannotmoveAfterGameFinished(){
        Game game=new Game("player1","player2");
        game.finish("player1");
        assertThrows(
                IllegalStateException.class,
                ()->engine.makeMove(game,"player2",7,7)
        );
        assertEquals(0,game.getMoveCount());
    }

    @Test
    void moveCountInceasesNormally(){
        Game game=new Game("player1","player2");
        engine.makeMove(game,"player1",7,7);
        engine.makeMove(game,"player2",7,8);

        assertEquals(2,game.getMoveCount());
        assertEquals(Chess.BLACK,game.getCurrentTurn());
    }

    @Test
    void fullBoardWithoutWinIsDraw(){
        Game game=new Game("player1","player2");
        Board board=game.getBoard();

        for (int row=0;row<Board.SIZE;row++){
            for (int col=0;col<Board.SIZE;col++){
                if (row == 14 && col == 14) {
                    continue;
                }
                Chess chess=((row + 2 * col) % 5 < 3)?Chess.BLACK:Chess.WHITE;
                board.place(row,col,chess);
            }
        }
        engine.makeMove(game,"player1",14,14);
        assertEquals(GameStatus.FINISHED,game.getGameStatus());
        assertNull(game.getWinnerId());
        assertTrue(board.isFull());
    }
}
