package TicTacToe.Version1;

import TicTacToe.Version1.strategy.WinningStrategy;

import java.util.List;

public class Game {

    private final Board board;
    private final List<Player> players;
    private final WinningStrategy winningStrategy;

    private int currentPlayerIndex;
    private GameStatus status;
    private Player winner;

    public Game(Board board, List<Player> players, WinningStrategy strategy) {
        this.board = board;
        this.players = players;
        this.winningStrategy = strategy;
        this.status = GameStatus.IN_PROGRESS;
        this.currentPlayerIndex = 0;
    }

    public void makeMove(int row, int col) {
        if (status != GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("Game already finished");
        }

        Cell cell = board.getCell(row, col);

        if (!cell.isEmpty()) {
            throw new IllegalArgumentException("Cell already occupied");
        }

        Player currentPlayer = players.get(currentPlayerIndex);
        cell.setPlayer(currentPlayer);

        // Check Winner
        if (winningStrategy.checkWinner(board, currentPlayer, row, col)) {
            status = GameStatus.WIN;
            winner = currentPlayer;
            return;
        }

        // Check Draw
        if (board.isFull()) {
            status = GameStatus.DRAW;
            return;
        }

        // Switch Turn
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    public GameStatus getStatus() {
        return status;
    }

    public Player getWinner() {
        return winner;
    }

    public Board getBoard() {
        return board;
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }
}
