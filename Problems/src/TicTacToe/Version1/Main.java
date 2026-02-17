package TicTacToe.Version1;

import TicTacToe.Version1.strategy.DefaultWinningStrategy;
import TicTacToe.Version1.strategy.WinningStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        // Board Size (Extensible)
        int boardSize = 3;
        Board board = new Board(boardSize);

        // Create Symbols
        Symbol x = new Symbol("X");
        Symbol o = new Symbol("O");

        // Create Players
        Player player1 = new Player("Player 1", x);
        Player player2 = new Player("Player 2", o);

        List<Player> players = new ArrayList<>();
        players.add(player1);
        players.add(player2);

        // Inject Winning Strategy
        WinningStrategy strategy = new DefaultWinningStrategy();

        // Create Game
        Game game = new Game(board, players, strategy);

        ConsoleDisplay display = new ConsoleDisplay();

        // Game Loop
        while (game.getStatus() == GameStatus.IN_PROGRESS) {

            display.printBoard(game.getBoard());

            Player currentPlayer = game.getCurrentPlayer();
            System.out.println(currentPlayer.getName() + "'s turn (" +
                    currentPlayer.getSymbol().getValue() + ")");

            System.out.print("Enter row: ");
            int row = scanner.nextInt();

            System.out.print("Enter col: ");
            int col = scanner.nextInt();

            try {
                game.makeMove(row, col);
            } catch (Exception e) {
                System.out.println("Invalid Move: " + e.getMessage());
            }
        }

        display.printBoard(game.getBoard());

        if (game.getStatus() == GameStatus.WIN) {
            System.out.println("Winner is: " + game.getWinner().getName());
        } else {
            System.out.println("Game ended in a draw!");
        }

        scanner.close();
    }
}
