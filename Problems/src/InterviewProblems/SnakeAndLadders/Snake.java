package InterviewProblems.SnakeAndLadders;

public class Snake extends BoardEntity {

    public Snake(int head, int tail) {
        super(head, tail);
        if (tail >= head) {
            throw new IllegalArgumentException("Snake tail must be below head. head=" + head + ", tail=" + tail);
        }
    }
}
