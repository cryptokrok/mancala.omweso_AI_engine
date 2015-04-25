package omweso;

import java.util.Deque;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Random;

import boardgame.Board;
import boardgame.BoardState;
import boardgame.Move;
import boardgame.Player;

import omweso.CCBoardState;
import omweso.CCMove.MoveType;

/**
 *A minimax Omweso player.
 */
public class CCMinimax extends Player {

    static private String default_name = "minimax";

    private boolean verbose = false;
    Random rand = new Random();

    /** Provide a default public constructor */
    public CCMinimax() { super(default_name); }
    public CCMinimax(String s) { super(s); }

    public Board createBoard() { return new CCBoard(); }

    public int getMaxDepth(){return 1;}

    public static class Node {
        private CCBoardState state;
        private CCMove move;
        private Node parent;
        private float score;
        private ArrayList<Node> children;
        private int depth;

        // index of the best child
        private int best;

        public Node(CCBoardState state, CCMove move, Node parent, int depth){
            this.state = state;
            this.move = move;
            this.parent = parent;
            this.children = new ArrayList<Node>();
            this.depth = depth;

            this.score = (depth % 2 == 0) ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
        }

        public CCBoardState getState(){
            return state;
        }

        public void clearState(){
            this.state = null;
        }

        public CCMove getMove(){
            return move;
        }

        public CCMove getBestMove(){
            return children.get(best).getMove();
        }

        public Node getParent(){
            return parent;
        }

        public void setScore(float score){
            this.score = score;

            if(parent == null){
                return;
            }

            float parent_score = parent.getScore();

            if((depth % 2 == 0 && parent_score > score) ||
                    (depth % 2 == 1 && parent_score < score)){

                parent.setScore(score);
                parent.setBest(parent.getChildren().indexOf(this));
            }
        }

        public float getScore(){
            return score;
        }

        public void addChild(Node child){
            this.children.add(child);
        }

        public ArrayList<Node> getChildren(){
            return children;
        }

        public int getDepth(){
            return depth;
        }

        public void setBest(int best){
            this.best = best;
        }

        public int getBest(){
            return best;
        }

        public String toPrettyString(){
            StringBuilder sb = new StringBuilder();

            sb.append("** NODE **");
            sb.append("\n");

            if(state != null)
                sb.append("State: " + state.toString());
            else
                sb.append("State: null");
            sb.append("\n");

            if(move != null)
                sb.append("Move: " + move.toPrettyString());
            else
                sb.append("Move: null");

            sb.append("\n");

            if(parent != null)
                sb.append("Parent: " + parent);
            else
                sb.append("Parent: null");

            sb.append("\n");

            sb.append("Score: " + score);
            sb.append("\n");

            sb.append("Num children: " + children.size());
            sb.append("\n");

            sb.append("Depth: " + depth);
            sb.append("\n");

            sb.append("Best: " + best);
            sb.append("\n");

            return sb.toString();
        }
    }

    public CCMove chooseInit(CCBoardState board_state){
        int[] initial_pits = new int[2 * CCBoardState.SIZE];
        int num_seeds = CCBoardState.NUM_INITIAL_SEEDS;

        if(board_state.playFirst()){
            // Throw each starting seed in a random pit.
            for(int i = 0; i < num_seeds; i++){
                int pit = rand.nextInt(2 * CCBoardState.SIZE);
                initial_pits[pit]++;
            }
        }else{
            initial_pits[0] = num_seeds;
        }

        return new CCMove(initial_pits);
    }

    public float scorePosition(CCBoardState board_state){
        float score = 0;

        if(board_state.haveWon()){
            score = Float.POSITIVE_INFINITY;
        }else if(board_state.haveLost()){
            score = Float.NEGATIVE_INFINITY;
        }else if(board_state.gameOver()){
            // DRAW
            score = 0;
        }else{
            // Get the board_state so we can use it to make decisions.
            int[][] pits = board_state.getBoard();

            // Our pits in first row of array, opponent pits in second row.
            int[] my_pits = pits[0];
            int[] op_pits = pits[1];

            int seed_advantage = 0;

            for(int i = 0; i < my_pits.length; i++){
                seed_advantage += my_pits[i];
                seed_advantage -= op_pits[i];
            }

            score = seed_advantage;
        }

        return score;
    }

    public CCMove choosePit(CCBoardState board_state){
        Node root = new Node(board_state, null, null, 0);

        Deque<Node> stack = new LinkedList<Node>();

        stack.push(root);

        while(!stack.isEmpty()){
            Node node = stack.pop();
            CCBoardState current_bs = node.getState();
            int d = node.getDepth();

            if(d < getMaxDepth()){
                ArrayList<CCMove> moves = current_bs.getLegalMoves();

                if(moves.isEmpty()){
                    float score = scorePosition(current_bs);
                    node.setScore(score);
                }else{
                    for(CCMove m : moves){
                        CCBoardState b = (CCBoardState) current_bs.clone();
                        b.move(m);
                        Node n = new Node(b, m, node, d+1);
                        node.addChild(n);
                        stack.push(n);
                    }
                }
            }else{
                float score = scorePosition(current_bs);
                node.setScore(score);
            }
            node.clearState();
        }
        return root.getBestMove();
    }

    /** Implement a somewhat smart way of picking moves */
    public Move chooseMove(BoardState bs)
    {
        // Cast the arguments to the objects we want to work with.
        CCBoardState board_state = (CCBoardState) bs;

        if(!board_state.isInitialized()){
            return chooseInit(board_state);
        }else{
            return choosePit(board_state);
        }
    }
}