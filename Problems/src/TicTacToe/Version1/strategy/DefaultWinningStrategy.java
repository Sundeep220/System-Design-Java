package TicTacToe.Version1.strategy;

import TicTacToe.Version1.Board;
import TicTacToe.Version1.Player;

public class DefaultWinningStrategy implements WinningStrategy {

    @Override
    public boolean checkWinner(Board board, Player player, int row, int col) {
        int size = board.getSize();

        // Check Row
        boolean rowWin = true;
        for (int c = 0; c < size; c++) {
            if (board.getCell(row, c).getPlayer() != player) {
                rowWin = false;
                break;
            }
        }

        // Check Column
        boolean colWin = true;
        for (int r = 0; r < size; r++) {
            if (board.getCell(r, col).getPlayer() != player) {
                colWin = false;
                break;
            }
        }

        // Check Main Diagonal
        boolean diagWin = true;
        if (row == col) {
            for (int i = 0; i < size; i++) {
                if (board.getCell(i, i).getPlayer() != player) {
                    diagWin = false;
                    break;
                }
            }
        } else {
            diagWin = false;
        }

        // Check Anti Diagonal
        boolean antiDiagWin = true;
        if (row + col == size - 1) {
            for (int i = 0; i < size; i++) {
                if (board.getCell(i, size - i - 1).getPlayer() != player) {
                    antiDiagWin = false;
                    break;
                }
            }
        } else {
            antiDiagWin = false;
        }

        return rowWin || colWin || diagWin || antiDiagWin;
    }
}
