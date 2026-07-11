package InterviewProblems.LRUCache;

public class Node {
    int key;
    int value;
    Node left;
    Node right;

    public Node(int key, int value) {
        this.key = key;
        this.value = value;
        this.left = null;
        this.right = null;
    }
}
