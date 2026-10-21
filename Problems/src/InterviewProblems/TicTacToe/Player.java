package InterviewProblems.TicTacToe;

public class Player {
    private String name;
    private Symbol symbol;

    public Player(String name, Symbol symbol){
        this.name = name;
        this.symbol = symbol;
    }

    public Player getPlayer(){
        return this;
    }

    public String getName(){
        return String.valueOf(this.name);
    }

    public Symbol getSymbol(){
        return this.symbol;
    }
}
