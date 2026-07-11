package InterviewProblems.LRUCache;

import java.util.HashMap;
import java.util.Map;

public class LRUCache {

    private final int capacity;
    private final Map<Integer, Node> map;
    private final Node head;
    private final Node tail;

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.map = new HashMap<>();

        head = new Node(0, 0);
        tail = new Node(0, 0);

        head.right = tail;
        tail.left = head;
    }

    public int get(int key) {
        Node node = map.get(key);

        if (node == null) {
            return -1;
        }

        moveToHead(node);
        return node.value;
    }

    public void put(int key, int value) {

        // Capacity is zero, nothing can be stored.
        if (capacity == 0) {
            return;
        }

        Node node = map.get(key);

        if (node != null) {
            node.value = value;
            moveToHead(node);
            return;
        }

        Node newNode = new Node(key, value);

        addNodeToFront(newNode);
        map.put(key, newNode);

        if (map.size() > capacity) {
            Node lru = popTail();
            map.remove(lru.key);
        }
    }

    private void addNodeToFront(Node node) {
        node.left = head;
        node.right = head.right;

        head.right.left = node;
        head.right = node;
    }

    private void removeNode(Node node) {
        node.left.right = node.right;
        node.right.left = node.left;

        // Optional: helps debugging and GC
        node.left = null;
        node.right = null;
    }

    private void moveToHead(Node node) {
        removeNode(node);
        addNodeToFront(node);
    }

    private Node popTail() {
        Node lru = tail.left;
        removeNode(lru);
        return lru;
    }
}