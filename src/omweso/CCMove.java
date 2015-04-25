package omweso;

import java.util.Arrays;
import java.util.ArrayList;

import boardgame.Move;

public class CCMove extends Move{

    int player_id = -1;

    // There are two kinds of moves, plus one degenerate move.
    public enum MoveType{
        // Set initial seed locations
        INIT,

        // Standard move - choose a pit to begin sowing from
        PIT,

        NOTHING
    }

    int pit;
    int[] init;
    MoveType move_type;
    boolean from_board = false;

    /**
     * Create a degenerate move.
     */
    public CCMove(){
        this.move_type = MoveType.NOTHING;
    }

    /**
     * Create an initialization move.
     * @param init initial board configuration
     */
    public CCMove(int[] init){
        this.init = Arrays.copyOf(init, init.length);
        this.move_type = MoveType.INIT;
    }

    /**
     * Create a standard pit-choosing move.
     * @param pit which pit to start from
     */
    public CCMove(int pit){
        this.pit = pit;
        this.move_type = MoveType.PIT;
    }

    /**
     * Constructor from a string.
     * The string will be parsed and, if it is correct, will construct
     * the appropriate move. Mainly used by the server for reading moves
     * from a log file, and for constructing moves from strings sent
     * over the network.
     *
     * @param str The string to parse.
     */
    public CCMove(String str) {
        String[] components = str.split(" ");

        String s = "";

        String type_string = components[0];
        this.player_id = Integer.valueOf(components[1]);

        if(type_string.equals("NOTHING")){
            this.move_type = MoveType.NOTHING;

        }else if(type_string.equals("INIT")){
            ArrayList<String> filtered = new ArrayList<String>();

            this.from_board = Boolean.valueOf(components[2]);

            String[] pit_strings = components[3].split(",");

            for(int i = 0; i < pit_strings.length; i++){
                if(!pit_strings[i].equals("")){
                    filtered.add(pit_strings[i]);
                }
            }

            this.init = new int[filtered.size()];

            for(int i = 0; i < filtered.size(); i++){
                this.init[i] = Integer.valueOf(filtered.get(i));
            }

            this.move_type = MoveType.INIT;

        }else if(type_string.equals("PIT")){
            this.pit = Integer.valueOf(components[2]);
            this.move_type = MoveType.PIT;

        }else{
            throw new IllegalArgumentException(
                "Received a string that cannot be interpreted as a CCMove.");
        }
    }

    public MoveType getMoveType() {
        return move_type;
    }

    public int[] getInit() {
        return Arrays.copyOf(init, init.length);
    }

    public int getPit() {
        return pit;
    }

    /* Members below here are only used by the server; Player agents
     * needn't worry about them. */

    @Override
    public void setPlayerID(int player_id) {
        this.player_id = player_id;
    }

    @Override
    public int getPlayerID() {
        return player_id;
    }

    @Override
    public void setFromBoard(boolean from_board) {
        this.from_board = from_board;
    }

    public boolean getFromBoard() {
        return from_board;
    }

    public int[] getReceivers() {
        if(move_type == MoveType.INIT && !from_board){
            int[] value = {};
            return value;
        }else{
            return null;
        }
    }

    public boolean doLog(){
        boolean board_init_move = move_type == MoveType.INIT && from_board;
        boolean regular_move = move_type == MoveType.PIT;

        return board_init_move || regular_move;
    }

    @Override
    public String toPrettyString() {
        String s = "";

        switch(move_type){
            case NOTHING:
                s = String.format("Player %d ends turn.", player_id);
                break;
            case INIT:
                String array_string = Arrays.toString(init);

                if(from_board){
                    s = String.format(
                        "Player %d initializes with: %s", player_id, array_string);
                }else{
                    s = String.format(
                        "Player %d initialized", player_id);
                }
                break;
            case PIT:
                s = String.format("Player %d plays pit %d", player_id, pit);
                break;
        }

        return s;
    }

    @Override
    public String toTransportable() {
        String s = "";

        switch(move_type){
            case NOTHING:
                s = String.format("NOTHING %d", player_id);
                break;
            case INIT:
                String array_string = Arrays.toString(init);
                array_string = array_string.replace(", ", ",");
                array_string = array_string.replace("[", "");
                array_string = array_string.replace("]", "");

                s = String.format(
                    "INIT %d %b %s", player_id, from_board, array_string);
                break;
            case PIT:
                s = String.format("PIT %d %d", player_id, pit);
                break;
        }

        return s;
    }
}
