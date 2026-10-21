package InterviewProblems.SnakeAndLadders;

public class Ladder extends BoardEntity{
    public Ladder(int bottom, int top){
        super(bottom, top);

        if(top <= bottom)
            throw new IllegalArgumentException("Top of ladder cant be lesser or equal to bottom of ladder");
    }
}
