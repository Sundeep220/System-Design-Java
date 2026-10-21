package InterviewProblems.SnakeAndLadders;

public enum GameStatus {
    NOT_STARTED("NOT_STARTED"),
    IN_PROGRESS("IN_PROGRESS"),
    FINISHED("FINISHED");

    public final String text;

    GameStatus(String text){
        this.text = text;
    }
}
