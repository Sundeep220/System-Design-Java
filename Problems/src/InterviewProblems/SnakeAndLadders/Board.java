package InterviewProblems.SnakeAndLadders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Board {
    private int size;
    private Map<Integer, BoardEntity> jumps;
    private Map<Player, Integer> playerPositions;

    public Board(int size, List<Snake> snakes, List<Ladder> ladders, List<Player> players){
        if(size < 2) throw new IllegalArgumentException("Board size must be atleast 2");
        if(players == null || players.size() < 2) throw new IllegalArgumentException("Need atleast 2 players to start the game");
        this.size = size;
        this.jumps = new HashMap<>();
        this.playerPositions = new ConcurrentHashMap<>();

        for(Snake s: snakes){
            if(s.getStart() == size) throw new IllegalArgumentException("Snake cannot start at final cell");
            if (jumps.containsKey(s.getStart())) throw new IllegalArgumentException("Duplicate entity at cell " + s.getStart());
            jumps.put(s.getStart(), s);
        }

        for(Ladder l: ladders){
            if (jumps.containsKey(l.getStart())) throw new IllegalArgumentException("Duplicate entity at cell " + l.getStart());
            jumps.put(l.getStart(), l);
        }
        for (Player p : players) {
            playerPositions.put(p, 0);
        }
    }

    public int getSize(){
        return this.size;
    }

    public int getPlayerPosition(Player player){
        return playerPositions.getOrDefault(player, 0);
    }

    public void setPlayerPosition(Player player, int position) {
        playerPositions.put(player, position);
    }

    public int getNextPosition(int current, int steps) {
        int target = current + steps;
        if (target > size) return current;                   // exact finish rule: overshoot stays
        if (jumps.containsKey(target)) return jumps.get(target).getEnd();
        return target;
    }

}
