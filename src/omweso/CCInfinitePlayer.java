package omweso;

import boardgame.Board;
import boardgame.BoardState;
import boardgame.Move;
import boardgame.Player;

import java.util.ArrayList;

/** An infinite Omweso player. */
public class CCInfinitePlayer extends Player {
    public CCInfinitePlayer() { super("infinite"); }
    public CCInfinitePlayer(String s) { super(s); }

    public Board createBoard() { return new CCBoard(); }

    /** Use this method to take actions when the game is over. */
    public void gameOver( String msg, BoardState bs) {
        CCBoardState board_state = (CCBoardState) bs;

        if(board_state.haveWon()){
            System.out.println("I won!");
        }else if(board_state.haveLost()){
            System.out.println("I lost!");
        }else if(board_state.tieGame()){
            System.out.println("Draw!");
        }else{
            System.out.println("Undecided!");
        }
    }

    /** Implement a way of picking moves such that the turn will be infinitely long. */
    public Move chooseMove(BoardState bs)
    {
        // Cast the arguments to the objects we want to work with.
        CCBoardState board_state = (CCBoardState) bs;

        if(!board_state.isInitialized()){
            return new CCMove(new int[]{2, 2, 1, 2, 1, 2, 1, 2, 1, 3, 2, 3, 2, 3, 2, 3});
        }else{
            return new CCMove(0);
        }
    }
}
