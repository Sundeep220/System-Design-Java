package InterviewProblems.SnakeAndLadders;

import java.util.List;

public class Game {
    private final Board board;
    private final List<Player> players;
    private final Dice dice;
    private GameStatus status;
    private Player winner;
    private int currentIndex;

    public Game(Board board, List<Player> players, Dice dice){
        this.board = board;
        this.players = players;
        this.dice = dice;
        this.currentIndex = 0;
        this.status = GameStatus.NOT_STARTED;
        this.winner = null;
    }

    public void start(){
        if(status != GameStatus.NOT_STARTED) throw new IllegalArgumentException("Game is already started or finished");
        status = GameStatus.IN_PROGRESS;
    }

    public synchronized void playTurn(){
        if(status == GameStatus.FINISHED) return;
        if(status == GameStatus.NOT_STARTED) throw new IllegalArgumentException("Game is not yet started");

        Player current = players.get(currentIndex);
        int roll = dice.roll();
        movePlayer(current, roll);

        if(checkWin(current)){
            status = GameStatus.FINISHED;
            winner = current;
            return;
        }

        currentIndex = (currentIndex + 1) % players.size();
    }

    private void movePlayer(Player player, int steps){
        int current = board.getPlayerPosition(player);
        int next = board.getNextPosition(current, steps);
        board.setPlayerPosition(player, next);
    }

    private boolean checkWin(Player player){
        return board.getPlayerPosition(player) == board.getSize();
    }

    public Player getCurrentPlayer(){
        return players.get(currentIndex);
    }

    public Player getWinner(){
        return winner;
    }

    public GameStatus getStatus(){
        return status;
    }
}
