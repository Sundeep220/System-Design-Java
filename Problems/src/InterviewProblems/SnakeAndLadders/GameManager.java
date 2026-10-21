package InterviewProblems.SnakeAndLadders;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {
    private static GameManager instance;
    private final Map<String, Game> games = new ConcurrentHashMap<>();

    private GameManager(){}

    public static synchronized GameManager getInstance(){
        if(instance == null){
            instance = new GameManager();
        }
        return instance;
    }

    public String createGame(Board board, List<Player> players, Dice dice){
        String id = UUID.randomUUID().toString();
        Game game = new Game(board, players, dice);
        games.put(id, game);
        return id;
    }

    public Game getGame(String id){
        return games.get(id);
    }

    public void removeGame(String id){
        games.remove(id);
    }
}
