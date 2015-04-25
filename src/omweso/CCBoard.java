package omweso;

import java.util.Arrays;
import java.util.ArrayList;

import boardgame.Board;
import boardgame.BoardState;
import boardgame.BoardPanel;
import boardgame.Move;

import omweso.CCMove.MoveType;

public class CCBoard extends Board{

    private CCBoardState board_state = new CCBoardState();

    public static int SIZE = CCBoardState.SIZE;
    public static int NUM_INITIAL_SEEDS = CCBoardState.NUM_INITIAL_SEEDS;

    // Records the state of the board while a human player initializes it.
    private int[][] init_state = new int[2][2*SIZE];

    // Number of initialization moves seen so far by this board.
    private int init_turn;

    // Number of seeds remaining to place. Used during initialization.
    public int[] seeds_remaining = new int[2];

    private CCBoard(CCBoardState board_state, int[][] init_state, int init_turn, int[] seeds_remaining){

        super();

        this.board_state = (CCBoardState) board_state.clone();

        this.init_state[0] = Arrays.copyOf(init_state[0], 2 * SIZE);
        this.init_state[1] = Arrays.copyOf(init_state[1], 2 * SIZE);

        this.init_turn = init_turn;

        this.seeds_remaining = Arrays.copyOf(seeds_remaining, 2);
    }

    public CCBoard() {
        board_state = new CCBoardState();

        this.init_state[0] = new int[2 * SIZE];
        this.init_state[1] = new int[2 * SIZE];

        init_turn = 0;

        seeds_remaining[0] = NUM_INITIAL_SEEDS;
        seeds_remaining[1] = NUM_INITIAL_SEEDS;
    }

    @Override
    public void move(Move m) throws IllegalArgumentException {
        CCMove ccm = (CCMove) m;

        if(!isInitialized()){

            if(!(ccm.getMoveType() == MoveType.INIT && !ccm.getFromBoard())){
                // Ignore initialization moves sent from clients. They are handled in filterMove.
                board_state.move(ccm);
            }

            init_turn = (init_turn + 1) % 2;
        }else{
            board_state.move(ccm);
        }
    }

    public BoardState getStateFromPerspective(int player_id){
        // CCBoard stores its board state from the perspective of
        // player 0, so only have to switch perspective if player_id == 1.
        // In either case, we clone the board state.
        if(player_id == 0){
            return (BoardState) board_state.clone();
        }else{
            CCBoardState bs = (CCBoardState) board_state.clone();
            bs.switchPerspective();
            return (BoardState) bs;
        }
    }

    /* Methods called by the GUI during initialization. */

    boolean isInitialized(){
        return board_state.isInitialized();
    }

    boolean isLegal(CCMove move){
        return board_state.isLegal(move);
    }

    /** Return the number of seeds in a given pit. */
    int getNumSeeds(int player_id, int pit){
        if(!board_state.isInitialized()){
            return init_state[player_id][pit];
        }else{
            return board_state.getBoard()[player_id][pit];
        }
    }

    /** Get number of seeds that current player has left to place. */
    public int getSeedsRemaining(){
        return seeds_remaining[getTurn()];
    }

    /** Add a seed to a pit during initialization. */
    public int addSeed(int player_id, int pit){
        if(!board_state.isInitialized()){
            if(player_id == init_turn && seeds_remaining[init_turn] > 0){
                init_state[init_turn][pit]++;
                seeds_remaining[init_turn]--;
                return seeds_remaining[init_turn];
            }else{
                throw new IllegalArgumentException("Adding seed out of turn");
            }
        }else{
            throw new IllegalStateException(
                "Adding seed, but initialization has already been done.");
        }
    }

    /** Remove a seed from a pit during initialization. */
    public int removeSeed(int player_id, int pit){
        if(!board_state.isInitialized()){
            if(player_id == init_turn){
                if(init_state[init_turn][pit] > 0){
                    init_state[init_turn][pit]--;
                    seeds_remaining[init_turn]++;
                    return seeds_remaining[init_turn];
                }

                return seeds_remaining[init_turn];
            }else{
                throw new IllegalArgumentException("Removing seed out of turn");
            }
        }else{
            throw new IllegalStateException(
                "Removing seed, but initialization has already been done.");
        }
    }

    /**
     * Get a Move implementing the initialization for the player
     * whose turn it currently is. The initialization must
     * be implemented as a Move so that it is communicated to the
     * clients, and so that it is recorded by the logging system. */
    public CCMove getInitMove(){
        int turn_player = getTurn();

        int total_seeds = 0;
        for(int i = 0; i < 2 * SIZE; i++){
            total_seeds += init_state[turn_player][i];
        }

        if(total_seeds == NUM_INITIAL_SEEDS){
            CCMove ccm = new CCMove(Arrays.copyOf(init_state[turn_player], 2 * SIZE));
            ccm.setPlayerID(turn_player);
            return ccm;
        }else{
            throw new IllegalStateException(
                "Trying to create initialization move, but not all " +
                "seeds have been placed.");
        }
    }

    /* Methods called by the Server. */

    @Override
    public Move getRandomMove(){
        return board_state.getRandomMove();
    }

    @Override
    public int getWinner() {
        return board_state.getWinner();
    }

    @Override
    public void forceWinner(int player_id) {
        board_state.setWinner(player_id);
    }

    @Override
    public int getTurn() {
        if(!board_state.isInitialized()){
            return init_turn;
        }

        return board_state.getTurn();
    }

    @Override
    public int getTurnsPlayed() {
        return board_state.getTurnsPlayed();
    }

    @Override
    public Object filterMove(Move m) throws IllegalArgumentException {
        CCMove ccm = (CCMove) m;

        if(ccm.move_type == MoveType.INIT && !ccm.getFromBoard()){
            if(init_turn == 0){
                init_state[0] = ccm.getInit();
                return m;
            }else{
                CCMove[] initialization_moves = new CCMove[3];

                initialization_moves[0] = ccm;

                initialization_moves[1] = new CCMove(init_state[0]);
                initialization_moves[1].setPlayerID(0);
                initialization_moves[1].setFromBoard(true);

                initialization_moves[2] = new CCMove(ccm.getInit());
                initialization_moves[2].setPlayerID(1);
                initialization_moves[2].setFromBoard(true);

                return initialization_moves;
            }
        }else{
            return m;
        }
    }

    @Override
    public String getNameForID(int p) {
        return String.format("Player-%d", p);
    }

    @Override
    public int getIDForName(String s) {
        return Integer.valueOf(s.split("-")[1]);
    }

    @Override
    public int getNumberOfPlayers() {
        return 2;
    }

    @Override
    public Move parseMove(String str)
               throws NumberFormatException, IllegalArgumentException {

        return new CCMove(str);
    }

    @Override
    public Object clone() {
        return new CCBoard(
            board_state, init_state, init_turn, seeds_remaining);
    }

    @Override
    public String toString(){
        return board_state.toString();
    }

    @Override
    public BoardPanel createBoardPanel() { return new CCBoardPanel(); }
}
