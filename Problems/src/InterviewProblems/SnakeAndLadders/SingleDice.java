package InterviewProblems.SnakeAndLadders;

import java.util.Random;

public class SingleDice implements Dice {
    private final Random random = new Random();

    public int roll() {
        return random.nextInt(6) + 1;
    }
}
