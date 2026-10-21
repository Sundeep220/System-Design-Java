package InterviewProblems.TicTacToe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Board {
    private List<List<Cell>> cells;
    private int size;

    public Board(int size){
        this.size = size;
        cells = new ArrayList<>();

        for(int i = 0; i < size; i++){
            List<Cell> cell = new ArrayList<>();
            for(int j = 0; j < size; j++){
                Cell current = new Cell(i, j);
                cell.add(current);
            }
            cells.add(cell);
        }
    }

    public List<List<Cell>> getCells() {
        return Collections.unmodifiableList(cells);
    }


    public int getSize() {
        return size;
    }

    public Cell getCell(int row, int col){
        return cells.get(row).get(col);
    }


    public boolean isFull(){
        for(List<Cell> row: cells){
            for(Cell cell: row){
                if(!cell.isEmpty()){
                    return false;
                }
            }
        }
        return true;
    }
}
