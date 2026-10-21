package InterviewProblems.SnakeAndLadders;

public abstract class BoardEntity {
    int start;
    int end;

    BoardEntity(int start, int end){
        this.start = start;
        this.end = end;
    }

    public int getStart() { return start; }

    public int getEnd() { return end; }
}
