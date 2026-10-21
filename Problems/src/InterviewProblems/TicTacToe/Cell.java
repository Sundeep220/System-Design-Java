package InterviewProblems.TicTacToe;

import javax.swing.plaf.synth.SynthButtonUI;
import java.util.Objects;

public class Cell {
    private int row;
    private int col;
    private Player player;

    public Cell(int row, int col){
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public boolean isEmpty(){
        return Objects.isNull(this.player);
    }
}
