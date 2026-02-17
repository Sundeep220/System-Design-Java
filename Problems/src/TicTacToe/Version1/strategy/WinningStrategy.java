package TicTacToe.Version1.strategy;

import TicTacToe.Version1.Board;
import TicTacToe.Version1.Player;

public interface WinningStrategy {
    boolean checkWinner(Board board, Player player, int row, int col);
}
