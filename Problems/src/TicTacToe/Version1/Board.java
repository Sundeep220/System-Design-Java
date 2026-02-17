package TicTacToe.Version1;

import java.util.ArrayList;
import java.util.List;

public class Board {

    private final int size;
    private final List<List<Cell>> grid;

    public Board(int size) {
        this.size = size;
        this.grid = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            List<Cell> row = new ArrayList<>();
            for (int j = 0; j < size; j++) {
                row.add(new Cell(i, j));
            }
            grid.add(row);
        }
    }

    public int getSize() {
        return size;
    }

    public Cell getCell(int row, int col) {
        return grid.get(row).get(col);
    }

    public boolean isFull() {
        for (List<Cell> row : grid) {
            for (Cell cell : row) {
                if (cell.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}
