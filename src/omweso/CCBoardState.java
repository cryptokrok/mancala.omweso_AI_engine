package omweso;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Random;

import boardgame.Board;
import boardgame.BoardState;
import boardgame.Move;

import omweso.CCMove.MoveType;

/** Stores the state of an Omweso board. The public members of this class
 * can be used by agents to get information about the current state of the game,
 * such as the current contents of both their own and their opponents pits, whether
 * either player has won the game, which player will get to play first, etc. The ``move''
 * method of this class is used by the server to update the game state in response
 * to moves submitted by the player, but can also be used by agents to determine the
 * consequences of a move.
 *
 * This class is intended to give the state of the board from an agent's perspective,
 * and the agents should always assume they are player 0. The methods haveWon and haveLost
 * operate from the perspective of the agent operating on this instance. */
public class CCBoardState extends BoardState{

    // Width of the board, in pits.
    public final static int SIZE = 8;

    // Number of seeds that each player starts with
    public final static int NUM_INITIAL_SEEDS = 32;

    // Number of turns before a draw is declared
    public final static int MAX_TURN = 5000;

    // Maximum number of iterations to implement a move.
    public final static int MAX_TURN_LENGTH = 200;

    Random rand = new Random();

    public enum Direction{
        CCW, CW
    }

    /** Number of seeds in each of the pits.
     * board[0] : Player 0's pits. Player 0 is the owner of this board instance
     * board[1] : Player 1's pits. Player 1 is the owner's opponent.
     * In each sub array, the pits are enumerated in CCW order, starting
     * from the left most pit (from that player's perspective) closest to
     * that player. So board[0][0] is the left most pit closest to Player 0,
     * board[0][2 * SIZE - 1] is the left most pit second closest to Player 0
     * (all from Player 0's perspective). Similarly, board[1][0] is the left
     * most pit (from Player 1's perspective) that is closest to Player 1, etc.*/
    private int[][] board = new int[2][2*SIZE];

    private int turn_number;

    private int winner;

    // ID of the player whose turn it is.
    private int turn_player;

    // ID of the player that plays first after init step
    private int plays_first;

    private CCBoardState(int[][] board, int turn_number, int winner, int turn_player, int plays_first){
        super();

        this.board[0] = Arrays.copyOf(board[0], 2 * SIZE);
        this.board[1] = Arrays.copyOf(board[1], 2 * SIZE);

        this.turn_number = turn_number;
        this.winner = winner;
        this.turn_player = turn_player;
        this.plays_first = plays_first;
    }

    public CCBoardState() {
        this.board[0] = new int[2 * SIZE];
        this.board[1] = new int[2 * SIZE];

        turn_number = 0;
        winner = Board.NOBODY;
        turn_player = 0;
        plays_first = 0;
    }

    /* Methods for use by agent code. */

    /**
     * Return whether the initialization phase has been completed. Agents
     * should use this to decide whether to provide an initialization move or
     * a normal pit-selection move. */
    public boolean isInitialized(){
        return turn_number > 0;
    }

    /** Return the ID of the player whose turn it is. The player owning
     * the board always has id 0. */
    public int getTurn() {
        return turn_player;
    }

    /** Return the number of turns that have been played so far. */
    public int getTurnsPlayed() {
        return turn_number;
    }

    /** Whether the player owning this board state has won. */
    public boolean haveWon() {
        return winner == 0;
    }

    /** Whether the player owning this board state has lost. */
    public boolean haveLost() {
        return winner == 1;
    }

    /** Whether the game has ended in a draw. */
    public boolean tieGame() {
        return winner == Board.DRAW;
    }

    /** Returns the player id of the player who has won, or Board.NOBODY
     * if the game is not over, or Board.DRAW if the game ended in a draw */
    public int getWinner(){
        return winner;
    }

    /** Used by the server to force a winner in the event of an error. */
    public void setWinner(int winner){
        this.winner = winner;
    }

    /** Whether the owner of the board state will play first after the
     * initialization step is completed. */
    public boolean playFirst() {
        return plays_first == 0;
    }

    /** Whether the game represented by this board state has completed. */
    public boolean gameOver() {
        return winner != Board.NOBODY;
    }

    /** Return a random legal move. */
    public Move getRandomMove(){
        if(!isInitialized()){
            // Throw each seed into a random pit.
            int[] initial_pits = new int[2 * SIZE];

            for(int i = 0; i < NUM_INITIAL_SEEDS; i++){
                int pit = rand.nextInt(2 * SIZE);
                initial_pits[pit]++;
            }

            return new CCMove(initial_pits);
        }else{
            ArrayList<CCMove> moves = getLegalMoves();
            return moves.get(rand.nextInt(moves.size()));
        }
    }

    /**
     * Get board from the perspective of the owner of the board.
     * First sub array gives that player's pit information, second
     * sub array gives opponent's pit information. */
    public int[][] getBoard(){
        return board;
    }

    /**
     * Get all legal move for the current board state. Only works
     * for normal pit-selection moves, cannot be used to get possible
     * initialization moves. (That would be a lot of moves!)
     *
     * Returned moves are assumed to be moves for the player whose turn
     * it currently is. */
    public ArrayList<CCMove> getLegalMoves(){
        ArrayList<CCMove> legal_moves = new ArrayList<CCMove>();

        for(int i = 0; i < 2 * SIZE; i++){
            CCMove move = new CCMove(i);
            if(isLegal(move)){
                legal_moves.add(move);
            }
        }

        return legal_moves;
    }

    /**
     * Return whether the supplied move is legal given the current
     * state of the board. */
    public boolean isLegal(CCMove m){

        if(!isInitialized()){
            if(m.getMoveType() == MoveType.INIT){
                int[] init = m.getInit();

                int total_seeds = 0;
                for(int i = 0; i < 2 * SIZE; i++){
                    total_seeds += init[i];
                }

                return total_seeds == NUM_INITIAL_SEEDS;
            }else{
                return false;
            }
        }

        return m.move_type == MoveType.PIT && board[turn_player][m.getPit()] > 1;
    }

    /**
     * Apply the given move to the board, updating the board's state.
     * Handles two separate types of moves: initialization moves,
     * which specify an initial assignment of seeds to pits for a
     * player, and standard pit-selection moves.
     *
     * This is used by the server to implement the game logic, but can
     * also be used by the player to explore the consequences of making
     * moves.
     * */
    public void move(Move m) throws IllegalArgumentException {
        CCMove ccm = (CCMove) m;

        if(!isLegal(ccm)){
            throw new IllegalArgumentException(
                "Invalid move for current context. " +
                "Move: " + ccm.toPrettyString());
        }

        if(!isInitialized()){
            board[turn_player] = Arrays.copyOf(ccm.getInit(), 2 * SIZE);
        }else{
            int start_pit = ccm.getPit();
            int end_pit = runMove(start_pit);
        }

        if(turn_player == 1){
            turn_number++;
        }

        turn_player = (turn_player + 1) % 2;
        updateWinner(turn_player);
    }

    /* Helper methods for implementing game logic. */

    /**
     * Given a starting pit, a number of seeds, and a direction,
     * adds one seed to each of the next `num_seeds` pits
     * in the given direction from the supplied pit. */
    private int sowSeeds(int pit, int num_seeds, Direction d){
        while(num_seeds > 0){
            pit = getNextPit(pit, d);
            board[turn_player][pit]++;
            num_seeds--;
        }

        return pit;
    }

    /**
     * Implements a normal move, using the given pit as the starting
     * pit. Implements all relays and captures caused by the move,
     * so this function does not return until the final seed in a
     * sowing sequence is placed in an empty pit. */
    private int runMove(int start_pit){
        int num_seeds = board[turn_player][start_pit];
        board[turn_player][start_pit] = 0;

        int end_pit = 0;
        int num_iterations = 0;

        while(true){
            if(num_iterations >= MAX_TURN_LENGTH){
                winner = turn_player == 0 ? Board.CANCELLED0 : Board.CANCELLED1;
                return end_pit;
            }

            end_pit = sowSeeds(start_pit, num_seeds, Direction.CCW);
            num_iterations++;

            if(board[turn_player][end_pit] > 1){
                // Landed in an occupied pit
                if(canCapture(turn_player, end_pit)){
                    num_seeds = capture(turn_player, end_pit);
                }else{
                    num_seeds = board[turn_player][end_pit];
                    board[turn_player][end_pit] = 0;
                    start_pit = end_pit;
                }
            }else{
                // Landed in an empty pit
                break;
            }
        }

        return end_pit;
    }

    /**
     * Given a pit index and a direction, returns the index of
     * the next pit in that direction. */
    public int getNextPit(int pit, Direction d){
        if(d == Direction.CCW){
            return (pit + 1) % (SIZE * 2);
        }else if(d == Direction.CW){
            return (pit - 1) % (SIZE * 2);
        }else{
            throw new IllegalArgumentException("Invalid direction.");
        }
    }

    /**
     * Return whether a capture is possible at the pit specified
     * by the given player_id and pit index, given the current state
     * of the board. Assumes sowing has already been carried out. Should
     * be a pit in the second row on the player's side. If the supplied
     * pit is on the given player's side but in the first row, the result
     * will always be false. Will also always be false on the first turn
     * (i.e., when turn_number == 1).*/
    private boolean canCapture(int player_id, int pit){
        if(turn_number <= 1){
            return false;
        }

        if(pit < SIZE || pit >= 2 * SIZE){
            return false;
        }

        boolean test = true;

        int opponent_player_id = (player_id + 1) % 2;
        int opponent_pit = pit - SIZE;

        test &= board[opponent_player_id][opponent_pit] > 0;
        test &= board[opponent_player_id][2 * SIZE - 1 - opponent_pit] > 0;
        return test;
    }

    /**
     * Implement a capture at the pit specified by the given
     * player_id and pit index. Assumes that a capture is possible
     * (i.e. canCapture returns true), and behaviour is undefined
     * otherwise. */
    private int capture(int player_id, int pit){
        int opponent_player_id = (player_id + 1) % 2;
        int opponent_pit = pit - SIZE;

        int captured_seeds = board[opponent_player_id][opponent_pit];
        captured_seeds += board[opponent_player_id][2 * SIZE - 1 - opponent_pit];

        board[opponent_player_id][opponent_pit] = 0;
        board[opponent_player_id][2 * SIZE - 1 - opponent_pit] = 0;

        return captured_seeds;
    }

    /** Return the number of seeds on the given player's side. */
    private int totalSeeds(int player_id){
        int num_seeds = 0;
        int pit = 0;

        for(int i = 0; i < 2 * SIZE; i++){
            num_seeds += board[player_id][pit];
            pit = getNextPit(pit, Direction.CCW);
        }

        return num_seeds;
    }

    /** Return whether the given player has any more valid moves. */
    private boolean hasValidMoves(int player_id){
        int pit = 0;

        for(int i = 0; i < 2 * SIZE; i++){
            if (board[player_id][pit] > 1){
                return true;
            }

            pit = getNextPit(pit, Direction.CCW);
        }

        return false;
    }

    /** Detect when a player has won. Called at the end of a turn. A player
     * wins when their opponent is about to play but has no legal moves.*/
    private void updateWinner(int next_to_play){

        if(winner != Board.NOBODY){
            return;
        }

        if(!isInitialized() && next_to_play == 1){
            return;
        }

        if(!hasValidMoves(next_to_play)){
            winner = (next_to_play + 1) % 2;
            return;
        }

        if(turn_number > MAX_TURN){
            winner = Board.DRAW;
        }
    }

    /** Change the perspective of this board. We want whoever is using this board
     * state instance to have player id 0. Agents shouldn't need this method. */
    public void switchPerspective(){
        turn_player = (turn_player + 1) % 2;
        plays_first = (plays_first + 1) % 2;
        if(winner == 0 || winner == 1){
            winner = (winner + 1) % 2;
        }

        int[] temp = board[1];
        board[1] = board[0];
        board[0] = temp;
    }

    @Override
    public Object clone() {
        return new CCBoardState(board, turn_number, winner, turn_player, plays_first);
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("Omweso board:\n");
        sb.append("Player 0 (Owner): \n");
        for(int i = 0; i < 2 * SIZE; i++){
            if(i > 0)
                sb.append(",");

            sb.append(Integer.toString(board[0][i]));
        }

        sb.append("\nPlayer 1 (Opponent): \n");
        for(int i = 0; i < 2 * SIZE; i++){
            if(i > 0)
                sb.append(",");

            sb.append(Integer.toString(board[0][i]));
        }

        sb.append("\nNext to play: " + turn_player);
        sb.append("\nPlays first: " + plays_first);
        sb.append("\nWinner: " + winner);
        sb.append("\nTurns played: " + turn_number);

        return sb.toString();
    }
}
