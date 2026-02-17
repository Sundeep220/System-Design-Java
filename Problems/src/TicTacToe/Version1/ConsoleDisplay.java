package TicTacToe.Version1;

public class ConsoleDisplay {

    public void printBoard(Board board) {
        int size = board.getSize();

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Cell cell = board.getCell(i, j);
                if (cell.isEmpty()) {
                    System.out.print("- ");
                } else {
                    System.out.print(cell.getPlayer().getSymbol().getValue() + " ");
                }
            }
            System.out.println();
        }
    }
}
