package InterviewProblems.LRUCache;

public class Main {

    public static void main(String[] args) {

        LRUCache cache = new LRUCache(2);

        cache.put(1, 10);
        cache.put(2, 20);

        System.out.println(cache.get(1)); // 10

        // Cache order:
        // 1 (MRU), 2 (LRU)

        cache.put(3, 30); // Evicts key 2

        System.out.println(cache.get(2)); // -1
        System.out.println(cache.get(3)); // 30

        // Cache order:
        // 3 (MRU), 1 (LRU)

        cache.put(4, 40); // Evicts key 1

        System.out.println(cache.get(1)); // -1
        System.out.println(cache.get(3)); // 30
        System.out.println(cache.get(4)); // 40

        // Update an existing key
        cache.put(3, 300);

        System.out.println(cache.get(3)); // 300

        // Final cache:
        // 3 (MRU), 4 (LRU)

        cache.put(5, 50); // Evicts key 4

        System.out.println(cache.get(4)); // -1
        System.out.println(cache.get(5)); // 50
        System.out.println(cache.get(3)); // 300
    }
}