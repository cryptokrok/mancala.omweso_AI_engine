package alphaBeta;

import boardgame.Board;
import boardgame.BoardState;
import boardgame.Move;
import boardgame.Player;
import omweso.CCBoardState;
import omweso.CCBoard;
import omweso.CCMove;

import java.util.ArrayList;
import java.util.Random;

/* Mini Max Algorithm with Alpha Beta Pruning */
public class alphaBetaPlayer extends Player {

    static private String default_name = "alphaBeta";

    public alphaBetaPlayer() { super(default_name); }
    public alphaBetaPlayer(String s) { super(s); }

    public Board createBoard() { return new CCBoard(); }

    public Move chooseMove(BoardState bs) //Method decides which hole to pick up seeds from
    {
        CCBoardState board_state = (CCBoardState) bs;
        if(!board_state.isInitialized()){ //Game Board needs to be initialized before selecting an optimal move
            return initialize(board_state);
        }else{
            return holeSelect(board_state); //Optimal hole is to be selected
        }
    }
    
    public CCMove initialize(CCBoardState board_state){ //Randomly assigns seeds 
        Random rand = new Random();
    	int[] holes_at_start = new int[2 * CCBoardState.SIZE];
        int num_seeds = CCBoardState.NUM_INITIAL_SEEDS;
        if(board_state.playFirst()){
            for(int i = 0; i < num_seeds; i++){
                int hole = rand.nextInt(2 * CCBoardState.SIZE); //Select hole indexs at random
                holes_at_start[hole]++; //Increase this hole's seed count by one
            }
        }else{
        	holes_at_start[0] = num_seeds; //Stuff the (remainder of) seeds in the first hole
        }
        return new CCMove(holes_at_start); //This is our first move
    }

    public CCMove holeSelect(CCBoardState board_state){ //Optimal holes is selected by Minimax w/ Alpha Beta Pruning
    	int depth = 10; //Set 10 Depth Level to allow higher probability of success against other student's AIs
    	int maxScore = Integer.MIN_VALUE; //Set low for comparison to update its value on first iteration 
    	int value;
    	int alpha = Integer.MIN_VALUE;
    	int beta = Integer.MAX_VALUE;    	
    	CCMove move = null;
    	boolean maxPlayer = depth%2==0 ? true : false; //Pick what type of player for Alpha Beta Pruning to be correct
    	ArrayList<CCMove> moves = board_state.getLegalMoves();
    	for(CCMove m : moves){
    		CCBoardState board = (CCBoardState) board_state.clone(); // Don't actually make moves on board, so we use clone to simulate moves
    		board.move(m); //Updates board with move
    		value = alphaBeta(board, depth-1, alpha,beta,maxPlayer); //Alpha beta pruning, will assign values for level 1 moves based on recursive pruning at lower levels
    		if(value >= maxScore){
        		maxScore = value; //Value from Alpha Beta Pruning on Mini Max Tree
        		move = m; //Keep this as the best move until updated/termination of loop
        	}
    	}
    	return move; //This move has highest value 
    }
    

    private int alphaBeta(CCBoardState board_state, int depth, int alpha, int beta, boolean maxPlayer){
    	if(depth ==0){
    		return scoreValue(board_state);
    	}
    	maxPlayer = depth%2==0 ? true : false; //Toggle between Max and Min Player
    	if(maxPlayer) return maximize(board_state, depth, alpha, beta, maxPlayer); // We want max score for a max player, pruning ensues
    	else return minimize(board_state, depth, alpha, beta, maxPlayer);
    }
    
    private int maximize(CCBoardState board_state,int depth, int alpha, int beta,boolean maxPlayer){  //Pruning for Max Player
    	int cmp = Integer.MIN_VALUE; //Updates upward after first iteration, allows for Max
    	ArrayList<CCMove> moves = board_state.getLegalMoves();
    	for(CCMove m : moves){
    		CCBoardState board = (CCBoardState) board_state.clone();
    		board.move(m); //Update board with move
    		cmp = Math.max(cmp, alphaBeta(board, depth-1, alpha, beta, maxPlayer)); //Recurse to find value of lower nodes 
    		alpha = Math.max(alpha, cmp); //Max is used here since its max player
    		if(beta <= alpha) break;
    	}
    	return cmp; //Returns max value
    }
    private int minimize(CCBoardState board_state,int depth, int alpha, int beta,boolean maxPlayer){
    	int cmp = Integer.MAX_VALUE; //Updates downward after first iteration , allows for Min
    	ArrayList<CCMove> moves = board_state.getLegalMoves();
    	for(CCMove m : moves){
    		CCBoardState board = (CCBoardState) board_state.clone();
    		board.move(m);
    		cmp = Math.min(cmp, alphaBeta(board, depth-1, alpha, beta, maxPlayer));
    		beta = Math.min(beta, cmp);//Min is used here since its min player
    		if(beta <= alpha) break;
    	}
    	return cmp; //Returns min value
    }
    
    public int scoreValue(CCBoardState board_state){
        int score = 0;//Score based on the seed advantage
        int seeds = 0; //The seed advantage of the player
        int[][] holes = board_state.getBoard();
        int[] player = holes[0]; //Our holes 
        int[] opponent = holes[1]; //Opponent's holes
        if(board_state.haveWon())score = Integer.MAX_VALUE;
        else if(board_state.haveLost()) score = Integer.MIN_VALUE;
        else if(board_state.gameOver()) score = 0;
        else{
            for(int i = 0; i < holes.length; i++){
                seeds += player[i]; //Increase seed advantage for seeds in your holes
                seeds -= opponent[i]; //Decrease seed advantage for seeds in opponent's holes
            }
            score = seeds; 
        }
        return score;
    }
}
