package TicTacToe.Version1;

public class Cell {
    private final int row;
    private final int col;
    private Player player; // null means empty

    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public boolean isEmpty() {
        return player == null;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}

