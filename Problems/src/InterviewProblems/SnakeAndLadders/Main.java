package InterviewProblems.SnakeAndLadders;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<Player> players = List.of(new Player("Alice"), new Player("Bob"));

        List<Snake> snakes = List.of(
            new Snake(17, 7),
            new Snake(54, 34),
            new Snake(62, 19),
            new Snake(64, 60),
            new Snake(87, 24),
            new Snake(93, 73),
            new Snake(95, 75),
            new Snake(99, 78)
        );

        List<Ladder> ladders = List.of(
            new Ladder(1, 38),
            new Ladder(4, 14),
            new Ladder(9, 31),
            new Ladder(20, 38),
            new Ladder(28, 84),
            new Ladder(40, 59),
            new Ladder(51, 67),
            new Ladder(63, 81),
            new Ladder(71, 91)
        );

        Board board = new Board(100, snakes, ladders, players);
        Dice dice = new SingleDice();

        GameManager manager = GameManager.getInstance();
        String sessionId = manager.createGame(board, players, dice);
        Game game = manager.getGame(sessionId);

        game.start();
        System.out.println("Game started! Session: " + sessionId);

        int turnLimit = 1000;
        int turn = 0;
        while (game.getStatus() != GameStatus.FINISHED && turn < turnLimit) {
            Player current = game.getCurrentPlayer();
            game.playTurn();
            System.out.printf("%-8s rolled -> now at cell %d%n",
                current.name(), board.getPlayerPosition(current));
            turn++;
        }

        if (game.getStatus() == GameStatus.FINISHED) {
            System.out.println("\nWinner: " + game.getWinner().name());
        } else {
            System.out.println("\nGame ended after turn limit.");
        }

        manager.removeGame(sessionId);
    }
}
