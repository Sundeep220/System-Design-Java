package InterviewProblems.TicTacToe;

import TicTacToe.Version1.GameStatus;

import java.util.List;

public class Game {
    private final Board board;
    private final List<Player> players;
    private final WinningStrategy strategy;

    private int currentPlayerIdx;
    private GameStatus status;
    private Player winner;

    public Game(Board board, List<Player> players){
        this.board = board;
        this.players = players;
        this.strategy = new WinningStrategy();
        this.status = GameStatus.IN_PROGRESS;
        this.currentPlayerIdx = 0;
    }

    public void makeMove(int row, int col) throws Exception{
        if(status != GameStatus.IN_PROGRESS)
            throw new IllegalArgumentException("Game already ended");

        Cell cell = board.getCell(row, col);

        if(!cell.isEmpty())
            throw new IllegalArgumentException("Cell is already full");

        Player currentPlayer = players.get(currentPlayerIdx);
        cell.setPlayer(currentPlayer);

        // Check Winner
        if (strategy.checkWinner(board, currentPlayer, row, col)) {
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
        currentPlayerIdx = (currentPlayerIdx + 1) % players.size();
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
        return players.get(currentPlayerIdx);
    }
}
