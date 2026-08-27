import java.util.BitSet;

/**
 * Run with:  javac BloomFilterDemo.java && java BloomFilterDemo
 *
 * Covers:
 *   Part 1 — What is a BitSet (standalone demo)
 *   Part 2 — How a Bloom Filter uses a BitSet (step-by-step visual)
 *   Part 3 — Full Bloom Filter with optimal sizing + false-positive measurement
 */
public class BloomFilterDemo {

    // ═══════════════════════════════════════════════════════════════════════════
    //  PART 1 — WHAT IS A BITSET?
    // ═══════════════════════════════════════════════════════════════════════════

    static void part1_whatIsBitSet() {
        separator("PART 1: What is a BitSet?");

        // A BitSet is an array of BITS (0s and 1s), not bytes, not ints.
        // Default: all bits start at 0 (false).
        //
        // Regular boolean[] uses 1 BYTE (8 bits) per boolean — wasteful!
        // BitSet uses exactly 1 BIT per boolean — 8x more memory-efficient.
        //
        // Internally stored as long[] (each long = 64 bits).
        // BitSet of 64 bits  → 1 long  → 8 bytes
        // boolean[] of 64    → 64 bytes (8x bigger!)

        System.out.println("Creating a BitSet of 20 bits...");
        BitSet bits = new BitSet(20);   // 20 bits, ALL start as 0

        System.out.println("Initial state (all 0):  " + visualize(bits, 20));

        // ── SET individual bits (turn 0 → 1) ──────────────────────────────
        bits.set(3);    // bit at index 3 = 1
        bits.set(7);    // bit at index 7 = 1
        bits.set(15);   // bit at index 15 = 1

        System.out.println("After set(3,7,15):       " + visualize(bits, 20));
        //  Index: 0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19
        //  Bits:  0  0  0  1  0  0  0  1  0  0  0  0  0  0  0  1  0  0  0  0

        // ── GET — check if a specific bit is 1 ────────────────────────────
        System.out.println("\nbits.get(3)  = " + bits.get(3));   // true  (we set it)
        System.out.println("bits.get(5)  = " + bits.get(5));     // false (we didn't)
        System.out.println("bits.get(15) = " + bits.get(15));    // true

        // ── CLEAR — turn a bit back to 0 ──────────────────────────────────
        bits.clear(7);
        System.out.println("\nAfter clear(7):          " + visualize(bits, 20));
        System.out.println("bits.get(7) after clear  = " + bits.get(7));  // false

        // ── FLIP — toggle a bit ────────────────────────────────────────────
        bits.flip(0);   // 0 → 1
        bits.flip(3);   // 1 → 0 (was set earlier)
        System.out.println("After flip(0) flip(3):   " + visualize(bits, 20));

        // ── CARDINALITY — count of 1-bits ─────────────────────────────────
        System.out.println("\nCardinality (# of 1-bits): " + bits.cardinality());

        // ── MEMORY comparison ─────────────────────────────────────────────
        System.out.println("\n-- Memory comparison ----------------------------------");
        int N = 1_000_000;
        // boolean[] of 1M = 1M bytes = 1 MB
        // BitSet of 1M    = 1M bits  = 125 KB
        System.out.printf("boolean[%,d]: ~%,d bytes (1 byte per slot)%n", N, N);
        System.out.printf("BitSet(%,d):  ~%,d bytes (1 bit per slot - 8x less!)%n",
                N, N / 8);

        // ── Bitwise operations ─────────────────────────────────────────────
        System.out.println("\n-- Bitwise ops ----------------------------------------");
        BitSet a = new BitSet(); a.set(1); a.set(3); a.set(5);
        BitSet b = new BitSet(); b.set(3); b.set(5); b.set(7);

        System.out.println("A:         " + visualize(a, 10));
        System.out.println("B:         " + visualize(b, 10));

        BitSet andResult = (BitSet) a.clone(); andResult.and(b);
        System.out.println("A AND B:   " + visualize(andResult, 10));   // intersection

        BitSet orResult = (BitSet) a.clone(); orResult.or(b);
        System.out.println("A OR  B:   " + visualize(orResult, 10));    // union

        BitSet xorResult = (BitSet) a.clone(); xorResult.xor(b);
        System.out.println("A XOR B:   " + visualize(xorResult, 10));   // symmetric diff
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PART 2 — BLOOM FILTER STEP BY STEP (how BitSet is used)
    // ═══════════════════════════════════════════════════════════════════════════

    static void part2_bloomFilterStepByStep() {
        separator("PART 2: How a Bloom Filter uses a BitSet - Step by Step");

        // Small Bloom Filter: 20 bits, 3 hash functions — easy to visualize
        int m = 20;  // bit array size
        int k = 3;   // number of hash functions
        BitSet bloomBits = new BitSet(m);

        System.out.println("Bloom Filter: m=" + m + " bits, k=" + k + " hash functions");
        System.out.println("Initial:  " + visualize(bloomBits, m));
        System.out.println();

        // ── INSERT "alice" ─────────────────────────────────────────────────
        System.out.println("INSERT \"alice\":");
        int[] alicePositions = hashPositions("alice", k, m);
        System.out.printf("  hash1(\"alice\") %% %d = %d%n", m, alicePositions[0]);
        System.out.printf("  hash2(\"alice\") %% %d = %d%n", m, alicePositions[1]);
        System.out.printf("  hash3(\"alice\") %% %d = %d%n", m, alicePositions[2]);
        for (int pos : alicePositions) bloomBits.set(pos);
        System.out.println("  After:    " + visualize(bloomBits, m));
        System.out.println();

        // ── INSERT "bob" ───────────────────────────────────────────────────
        System.out.println("INSERT \"bob\":");
        int[] bobPositions = hashPositions("bob", k, m);
        System.out.printf("  hash1(\"bob\") %% %d = %d%n", m, bobPositions[0]);
        System.out.printf("  hash2(\"bob\") %% %d = %d%n", m, bobPositions[1]);
        System.out.printf("  hash3(\"bob\") %% %d = %d%n", m, bobPositions[2]);
        for (int pos : bobPositions) bloomBits.set(pos);
        System.out.println("  After:    " + visualize(bloomBits, m));
        System.out.println();

        // ── CHECK "alice" (should return true — was inserted) ─────────────
        System.out.println("CHECK \"alice\" (was inserted):");
        checkAndExplain(bloomBits, "alice", k, m);
        System.out.println();

        // ── CHECK "dave" (was NOT inserted) ───────────────────────────────
        System.out.println("CHECK \"dave\" (was NOT inserted):");
        checkAndExplain(bloomBits, "dave", k, m);
        System.out.println();

        // ── INSERT more words to cause a false positive ────────────────────
        System.out.println("Inserting more words to fill the filter...");
        String[] moreWords = {"charlie", "eve", "frank", "grace", "henry"};
        for (String word : moreWords) {
            for (int pos : hashPositions(word, k, m)) bloomBits.set(pos);
            System.out.println("  Inserted \"" + word + "\": " + visualize(bloomBits, m));
        }
        System.out.println();

        // ── Find a false positive ──────────────────────────────────────────
        System.out.println("Searching for FALSE POSITIVES (items never inserted):");
        String[] candidates = {"zara", "ivan", "julia", "kate", "leo",
                               "mike", "nina", "oscar", "paul", "quinn"};
        for (String candidate : candidates) {
            boolean result = mightContain(bloomBits, candidate, k, m);
            if (result) {
                System.out.println("  FALSE POSITIVE found! \"" + candidate +
                        "\" -> 'probably present' but was NEVER inserted!");
                checkAndExplain(bloomBits, candidate, k, m);
                break;
            }
        }
    }

    static void checkAndExplain(BitSet bits, String word, int k, int m) {
        int[] positions = hashPositions(word, k, m);
        boolean allSet = true;
        for (int i = 0; i < k; i++) {
            boolean bitVal = bits.get(positions[i]);
            System.out.printf("  hash%d(\"%s\") = %d -> bit[%d] = %d %s%n",
                    i + 1, word, positions[i], positions[i],
                    bitVal ? 1 : 0, bitVal ? "[set]" : "[0] <- NOT SET -> definitely absent!");
            if (!bitVal) allSet = false;
        }
        if (allSet) {
            System.out.println("  All bits = 1 -> \"Probably present\" " +
                    (word.equals("alice") || word.equals("bob") ? "[CORRECT]" : "[WARNING: might be false positive]"));
        } else {
            System.out.println("  Result: \"Definitely NOT present\" [100% accurate]");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PART 3 — FULL BLOOM FILTER WITH OPTIMAL SIZING
    // ═══════════════════════════════════════════════════════════════════════════

    static void part3_fullBloomFilter() {
        separator("PART 3: Full Bloom Filter - Optimal Sizing + False Positive Rate");

        int expectedInsertions = 10_000;
        double targetFPR = 0.01;  // 1% false positive rate

        SimpleBloomFilter filter = new SimpleBloomFilter(expectedInsertions, targetFPR);
        System.out.printf("Filter for %,d elements at %.0f%% FPR:%n",
                expectedInsertions, targetFPR * 100);
        System.out.printf("  Bit array size (m): %,d bits = %.2f KB%n",
                filter.m, filter.m / 8192.0);
        System.out.printf("  Hash functions (k): %d%n", filter.k);
        System.out.printf("  vs HashSet of same data: ~%.0f KB%n",
                expectedInsertions * 50 / 1024.0); // avg 50-byte string
        System.out.println();

        // Insert 10,000 known user IDs
        System.out.printf("Inserting %,d user IDs...%n", expectedInsertions);
        for (int i = 0; i < expectedInsertions; i++) {
            filter.add("user-id-" + i);
        }

        // ── Verify: no false NEGATIVES (must be 0) ─────────────────────────
        System.out.println("\nChecking for false negatives (must be 0):");
        int falseNegatives = 0;
        for (int i = 0; i < expectedInsertions; i++) {
            if (!filter.mightContain("user-id-" + i)) {
                falseNegatives++;
            }
        }
        System.out.println("  False negatives: " + falseNegatives +
                (falseNegatives == 0 ? " [PASS - impossible by design!]" : " [BUG!]"));

        // ── Measure actual false positive rate ─────────────────────────────
        System.out.println("\nMeasuring actual false positive rate:");
        int testCount = 100_000;
        int falsePositives = 0;
        for (int i = expectedInsertions; i < expectedInsertions + testCount; i++) {
            // "user-id-10000" through "user-id-110000" were NEVER inserted
            if (filter.mightContain("user-id-" + i)) {
                falsePositives++;
            }
        }
        double actualFPR = (double) falsePositives / testCount;
        System.out.printf("  Total queries: %,d (all non-existent)%n", testCount);
        System.out.printf("  False positives: %,d%n", falsePositives);
        System.out.printf("  Actual FPR: %.2f%% (target was %.2f%%)%n",
                actualFPR * 100, targetFPR * 100);
        System.out.printf("  These would cause unnecessary DB lookups (but NO wrong answers)%n");

        // ── Production simulation: cache miss prevention ────────────────────
        separator("Production Simulation: Preventing unnecessary DB lookups");
        System.out.println("Scenario: 1000 requests, mix of valid and invalid user IDs");

        int validRequests = 500;    // users that exist
        int invalidRequests = 500;  // users that don't exist (potential cache penetration attack)
        int dbLookups = 0;
        int savedLookups = 0;

        for (int i = 0; i < validRequests; i++) {
            // Valid user — filter says "probably present" → go to DB (correct!)
            if (filter.mightContain("user-id-" + i)) {
                dbLookups++;
            }
        }
        for (int i = expectedInsertions + testCount; i < expectedInsertions + testCount + invalidRequests; i++) {
            // Invalid user — filter says?
            if (filter.mightContain("user-id-" + i)) {
                dbLookups++;  // false positive → unnecessary DB lookup
                System.out.println("  FP: user-id-" + i + " -> sent to DB unnecessarily");
            } else {
                savedLookups++;  // "definitely not present" → skip DB!
            }
        }

        System.out.printf("%nResults:%n");
        System.out.printf("  Total requests: %d%n", validRequests + invalidRequests);
        System.out.printf("  DB lookups: %d%n", dbLookups);
        System.out.printf("  Saved DB lookups: %d (%.1f%% of invalid requests blocked!)%n",
                savedLookups, (double) savedLookups / invalidRequests * 100);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  BLOOM FILTER IMPLEMENTATION
    // ═══════════════════════════════════════════════════════════════════════════

    static class SimpleBloomFilter {
        final BitSet bits;
        final int m;   // bit array size
        final int k;   // number of hash functions

        SimpleBloomFilter(int expectedInsertions, double falsePositiveRate) {
            this.m = optimalM(expectedInsertions, falsePositiveRate);
            this.k = optimalK(m, expectedInsertions);
            this.bits = new BitSet(m);
        }

        // m = -n * ln(p) / (ln(2))^2
        private int optimalM(int n, double p) {
            return (int) Math.ceil(-n * Math.log(p) / (Math.log(2) * Math.log(2)));
        }

        // k = (m/n) * ln(2)
        private int optimalK(int m, int n) {
            return Math.max(1, (int) Math.round((double) m / n * Math.log(2)));
        }

        void add(String element) {
            for (int i = 0; i < k; i++) {
                bits.set(hash(element, i));
            }
        }

        boolean mightContain(String element) {
            for (int i = 0; i < k; i++) {
                if (!bits.get(hash(element, i))) {
                    return false;   // bit is 0 → DEFINITELY absent
                }
            }
            return true;            // all bits 1 → probably present
        }

        // Simple but decent hash with seed variation
        private int hash(String data, int seed) {
            int h = seed * 0x9747b28c;
            for (char c : data.toCharArray()) {
                h ^= c;
                h *= 0x5bd1e995;
                h ^= (h >>> 15);
            }
            return Math.abs(h % m);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  HELPERS for Part 2 (tiny fixed filter)
    // ═══════════════════════════════════════════════════════════════════════════

    static int[] hashPositions(String word, int k, int m) {
        int[] positions = new int[k];
        for (int i = 0; i < k; i++) {
            int h = (i + 1) * 0x9747b28c;
            for (char c : word.toCharArray()) {
                h ^= c;
                h *= 0x5bd1e995;
                h ^= (h >>> 15);
            }
            positions[i] = Math.abs(h % m);
        }
        return positions;
    }

    static boolean mightContain(BitSet bits, String word, int k, int m) {
        for (int pos : hashPositions(word, k, m)) {
            if (!bits.get(pos)) return false;
        }
        return true;
    }

    // ── Visual helpers ─────────────────────────────────────────────────────────

    static String visualize(BitSet bits, int size) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < size; i++) {
            sb.append(bits.get(i) ? "1" : "0");
            if (i < size - 1) sb.append(" ");
        }
        sb.append("]");
        return sb.toString();
    }

    static void separator(String title) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  " + title);
        System.out.println("=".repeat(70) + "\n");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  MAIN
    // ═══════════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        part1_whatIsBitSet();
        part2_bloomFilterStepByStep();
        part3_fullBloomFilter();
    }
}
