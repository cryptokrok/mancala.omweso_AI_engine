package omweso;

import java.util.Deque;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Random;

import boardgame.Board;
import boardgame.Move;
import boardgame.Player;

import omweso.CCBoard;
import omweso.CCMove.MoveType;
import omweso.CCMinimax;

/**
 *A deeper minimax Omweso player.
 */
public class CCMinimaxDeep extends CCMinimax {
	
    static private String default_name = "minimax_deep";

    private boolean verbose = false;
    Random rand = new Random();

    public int getMaxDepth(){return 2;}

    /** Provide a default public constructor */
    public CCMinimaxDeep() { super(default_name); }
    public CCMinimaxDeep(String s) { super(s); }
}