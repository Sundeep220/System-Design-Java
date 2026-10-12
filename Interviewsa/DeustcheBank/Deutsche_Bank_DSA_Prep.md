# Deutsche Bank — DSA Interview Prep (Java)

> **Interview Tomorrow — Practice Checklist**  
> Solve each problem without looking at the solution first, then verify.  
> For every question: state the approach → write code → test edge cases → state complexity.

---

## Quick Checklist

- [ ] 2.1 Remove duplicates from a string
- [ ] 2.2 Merge sort
- [ ] 2.3 Maximum subarray sum (Kadane's)
- [ ] 2.4 Connect N ropes with minimum cost
- [ ] 2.5 Count paths in N×M grid (DP)
- [ ] 2.6 Longest common substring
- [ ] 2.7 Rotate linked list right by k
- [ ] 2.8 Detect cycle & return start node
- [ ] 2.9 Two Sum
- [ ] 2.10 Swap two strings
- [ ] 2.11 Replace zero digits with 8
- [ ] 2.12 Armstrong number
- [ ] 2.13 Palindrome & string reversal
- [ ] 2.14 Queue using two stacks
- [ ] 2.15 Find duplicates and their frequencies
- [ ] 2.16 LRU Cache
- [ ] 2.17 Heap sort
- [ ] 2.18 Count digits and letters
- [ ] 2.19 Lexicographically smallest string (sum = k)
- [ ] 2.20 Bubble sort
- [ ] 2.21 Binary Search
- [ ] 2.22 Reverse a linked list
- [ ] 2.23 Check anagram
- [ ] 2.24 Fibonacci (DP / memoization)
- [ ] 2.25 Count elements greater than mean of neighbours
- [ ] 2.26 Range updates — difference array

---

## 2.1 Remove Duplicates from a String

**Problem:** Given a string, remove repeated characters so each character appears once, keeping its first occurrence. Example: `"geeksforgeeks"` becomes `"geksfor"`.

**Link:** [GFG — Remove Duplicates from a Given String](https://www.geeksforgeeks.org/dsa/remove-duplicates-from-a-given-string/)  
**Intuition:** Scan left to right. The first time a character appears it is new and must be kept; every later sighting is a duplicate. You only need an O(1) answer to "have I seen this character?", which a lookup table indexed by character code provides.

**Approach:**
1. Create `boolean[256]` to mark seen characters.
2. Walk the string once; if the character is unseen, mark it and append it to a `StringBuilder`.
3. Return the builder's contents.

**Pattern:** `Hashing / Membership table` - detect repeats in one pass using a set or array for O(1) lookup.  
**Complexity:** O(n) time · O(1) space (fixed 256-entry table)

```java
static String removeDuplicates(String s) {
    boolean[] seen = new boolean[256];
    StringBuilder result = new StringBuilder();

    for (int i = 0; i < s.length(); i++) {
        char ch = s.charAt(i);
        if (!seen[ch]) {
            seen[ch] = true;
            result.append(ch);
        }
    }
    return result.toString();
}
// Input:  "geeksforgeeks"
// Output: "geksfor"
```

**Edge cases:** empty string, all-same characters, Unicode (use `HashSet<Character>` instead).  
**Follow-ups:** Case-sensitive? Preserve first or last occurrence?

---

## 2.2 Merge Sort

**Problem:** Sort an integer array in ascending order using merge sort.

**Link:** [GFG — Merge Sort](https://www.geeksforgeeks.org/merge-sort/)  
**Intuition:** Two sorted halves can be merged in linear time by repeatedly taking the smaller front element. Splitting until each piece has one element (trivially sorted) gives about log n levels, and each level costs O(n) to merge, so the total is O(n log n).

**Approach:**
1. Base case: a range with 0 or 1 elements is already sorted.
2. Split at `mid`, recursively sort the left and right halves.
3. Merge with two pointers, copying the smaller head into a temp array; then copy any leftovers.
4. Copy the temp array back into the original range.

**Pattern:** `Divide and Conquer` - split, solve the halves, combine in linear work. The same merge step solves counting inversions and merging k sorted lists.  
**Complexity:** O(n log n) time · O(n) auxiliary space · Stable

```java
static void mergeSort(int[] a, int left, int right) {
    if (left >= right) return;

    int mid = left + (right - left) / 2;
    mergeSort(a, left, mid);
    mergeSort(a, mid + 1, right);
    merge(a, left, mid, right);
}

static void merge(int[] a, int left, int mid, int right) {
    int[] temp = new int[right - left + 1];
    int i = left, j = mid + 1, k = 0;

    while (i <= mid && j <= right) {
        if (a[i] <= a[j]) temp[k++] = a[i++];
        else               temp[k++] = a[j++];
    }
    while (i <= mid) temp[k++] = a[i++];
    while (j <= right) temp[k++] = a[j++];

    for (int x = 0; x < temp.length; x++) a[left + x] = temp[x];
}
// Call: mergeSort(arr, 0, arr.length - 1);
```

**Key talking points:** Why it's stable · Compare with quicksort (in-place vs stable) · O(n) space cost.

---

## 2.3 Maximum Subarray Sum — Kadane's Algorithm

**Problem:** Find the contiguous subarray with the largest sum. Example: `[-2,1,-3,4,-1,2,1,-5,4]` gives `6` (subarray `[4,-1,2,1]`).

**Link:** [GFG — Largest Sum Contiguous Subarray](https://www.geeksforgeeks.org/dsa/largest-sum-contiguous-subarray/) · [LeetCode 53](https://leetcode.com/problems/maximum-subarray/)  
**Intuition:** At each index ask: is it better to extend the best subarray that ends at the previous index, or to start fresh here? If the running total has become negative it can only hurt future sums, so drop it. `current` is the best sum ending at index i; `best` is the best sum seen anywhere.

**Approach:**
1. Initialise `current` and `best` to `nums[0]`.
2. For each later element: `current = max(nums[i], current + nums[i])`.
3. Update `best = max(best, current)`.
4. Return `best`.

**Pattern:** `Kadane's / Local vs global optimum` - keep the best value ending here and the best overall. Variants: maximum product subarray, circular maximum subarray.  
**Complexity:** O(n) time · O(1) space

```java
static int maxSubArray(int[] nums) {
    if (nums == null || nums.length == 0)
        throw new IllegalArgumentException("Array must not be empty");

    int current = nums[0];
    int best    = nums[0];

    for (int i = 1; i < nums.length; i++) {
        current = Math.max(nums[i], current + nums[i]);
        best    = Math.max(best, current);
    }
    return best;
}
// Input:  [-2, 1, -3, 4, -1, 2, 1, -5, 4]
// Output: 6  (subarray [4, -1, 2, 1])
```

**Edge cases:** All-negative array (answer is the least-negative element) · Single element.

---

## 2.4 Connect N Ropes with Minimum Cost

**Problem:** Joining two ropes costs the sum of their lengths. Given all rope lengths, find the minimum total cost to join them into one rope. Example: `[4,3,2,6]` gives `29`.

**Link:** [GFG — Connect N Ropes with Minimum Cost](https://www.geeksforgeeks.org/dsa/connect-n-ropes-minimum-cost/)  
**Intuition:** Every time a rope takes part in a join, its length is paid again. Joining short ropes first means they are counted in fewer joins, which is the same idea as Huffman coding. So the greedy choice is always to combine the two currently shortest ropes.

**Approach:**
1. Insert all lengths into a min-heap.
2. While more than one rope remains: poll the two smallest, add their sum to the total cost, and push the sum back.
3. Return the total cost.

**Pattern:** `Greedy + Min-Heap (Priority Queue)` - whenever you repeatedly combine the smallest items, use a heap. Same pattern for Huffman coding and merging k sorted lists.  
**Complexity:** O(n log n) time · O(n) space

```java
static long minCostToConnectRopes(int[] ropes) {
    PriorityQueue<Long> minHeap = new PriorityQueue<>();
    for (int rope : ropes) minHeap.offer((long) rope);

    long totalCost = 0;
    while (minHeap.size() > 1) {
        long first    = minHeap.poll();
        long second   = minHeap.poll();
        long combined = first + second;
        totalCost += combined;
        minHeap.offer(combined);
    }
    return totalCost;
}
// Input:  [4, 3, 2, 6]
// Output: 29
```

**Why greedy works:** Joining shorter ropes first minimises the number of times each rope's length contributes to the total cost (Huffman-coding argument).

---

## 2.5 Count Paths in an N×M Grid (DP)

**Problem:** Count the distinct paths from the top-left to the bottom-right cell of an R×C grid, moving only right or down. Example: a 3×3 grid gives `6`.

**Link:** [GFG — Count Possible Paths in N×M Matrix](https://www.geeksforgeeks.org/dsa/count-possible-paths-top-left-bottom-right-nxm-matrix/) · [LeetCode 62](https://leetcode.com/problems/unique-paths/)  
**Intuition:** To reach cell (r, c) you must come from above (r-1, c) or from the left (r, c-1), so `paths(r, c) = paths(r-1, c) + paths(r, c-1)`. Processing row by row, `dp[c]` still holds the value from above and `dp[c-1]` has already been updated for the current row, so one 1-D array is enough.

**Approach:**
1. Create `dp` of length `cols` with `dp[0] = 1`.
2. For each row, apply `dp[c] += dp[c-1]` for `c = 1..cols-1`.
3. The answer is `dp[cols-1]` after all rows.

**Pattern:** `Grid DP / Path counting` - define the state from its neighbours, then compress to a rolling array. For obstacles, set blocked cells to 0.  
**Complexity:** O(rows × cols) time · O(cols) space

```java
static long countPaths(int rows, int cols) {
    if (rows <= 0 || cols <= 0) return 0;

    long[] dp = new long[cols];
    dp[0] = 1;

    for (int r = 0; r < rows; r++) {
        for (int c = 1; c < cols; c++) {
            dp[c] += dp[c - 1];
        }
    }
    return dp[cols - 1];
}
// Input:  rows=3, cols=3
// Output: 6
```

**Variants to discuss:** Obstacles (set dp[r][c]=0) · 4-directional movement (BFS/DFS) · Overflow (`BigInteger` or modulo).

---

## 2.6 Longest Common Substring (DP)

**Problem:** Return the length of the longest substring (contiguous characters) that appears in both strings. Example: `"abcde"` and `"abfce"` give `2` (`"ab"`).

**Link:** [GFG — Longest Common Substring](https://www.geeksforgeeks.org/dsa/longest-common-substring-dp-29/)  
**Intuition:** Let `L[i][j]` be the length of the common run that ends at `a[i-1]` and `b[j-1]`. If the characters match, the run extends the diagonal: `L[i][j] = L[i-1][j-1] + 1`. If they differ, the run breaks and the value is 0. The answer is the maximum over all cells. Do not confuse this with LCS, which takes the max of neighbours instead of resetting to 0.

**Approach:**
1. Keep two rows, `prev` and `curr`, each of length `m+1`.
2. For each `i` and `j`: on a match set `curr[j] = prev[j-1] + 1` and update `best`; otherwise leave it 0.
3. After each outer iteration, `prev = curr`.

**Pattern:** `2-D DP with reset on mismatch` - track the length of a contiguous match and reset when the match breaks.  
**Complexity:** O(mn) time · O(n) space

```java
static int longestCommonSubstring(String a, String b) {
    int[] prev = new int[b.length() + 1];
    int best = 0;

    for (int i = 1; i <= a.length(); i++) {
        int[] curr = new int[b.length() + 1];
        for (int j = 1; j <= b.length(); j++) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) {
                curr[j] = prev[j - 1] + 1;
                best = Math.max(best, curr[j]);
            }
        }
        prev = curr;
    }
    return best;
}
// Input:  "abcde", "abfce"
// Output: 2  ("ab")
```

**Contrast with LCS:** Subsequence allows gaps; substring must be contiguous.

---

## 2.7 Rotate Linked List Right by k

**Problem:** Rotate a singly linked list to the right by `k` places. Example: `1->2->3->4->5` with `k=2` gives `4->5->1->2->3`.

**Link:** [GFG — Rotate a Linked List](https://www.geeksforgeeks.org/rotate-a-linked-list/) · [LeetCode 61](https://leetcode.com/problems/rotate-list/)  
**Intuition:** Rotating right by k moves the last k nodes to the front, so the new head is the node at index `length - k` (0-based). If k is larger than the length, only `k % length` matters. Closing the list into a ring turns the cut-and-reconnect into two pointer updates.

**Approach:**
1. Handle empty list, single node, and `k == 0` by returning `head`.
2. Walk to the tail and count the length.
3. Set `k = k % length`; if it becomes 0, return `head`.
4. Link the tail to the head to form a ring, then walk `length - k - 1` steps to the new tail.
5. The new head is `newTail.next`; set `newTail.next = null`.

**Pattern:** `Two Pointers / Index arithmetic` - convert a relative position into an absolute one using the length.  
**Complexity:** O(n) time · O(1) space

```java
static class ListNode {
    int val;
    ListNode next;
    ListNode(int val) { this.val = val; }
}

static ListNode rotateRight(ListNode head, int k) {
    if (head == null || head.next == null || k == 0) return head;

    int length = 1;
    ListNode tail = head;
    while (tail.next != null) { tail = tail.next; length++; }

    k %= length;
    if (k == 0) return head;

    tail.next = head;                          // form a ring
    int stepsToNewTail = length - k - 1;
    ListNode newTail = head;
    for (int i = 0; i < stepsToNewTail; i++) newTail = newTail.next;

    ListNode newHead = newTail.next;
    newTail.next = null;
    return newHead;
}
// Input:  1->2->3->4->5, k=2
// Output: 4->5->1->2->3
```

**Clarify:** Left vs right rotation · k larger than length (use modulo).

---

## 2.8 Detect Cycle in Linked List & Return Start Node

**Problem:** Given the head of a linked list, return the node where a cycle begins, or `null` if there is no cycle.

**Link:** [LeetCode 142 — Linked List Cycle II](https://leetcode.com/problems/linked-list-cycle-ii/) · [GFG](https://www.geeksforgeeks.org/detect-and-remove-loop-in-a-linked-list/)  
**Intuition:** A slow pointer moves 1 step and a fast pointer moves 2. If a cycle exists, the fast pointer gains one step per iteration on the slow one, so they must meet inside the cycle. Let `a` be the distance from head to the cycle start. From the meeting point, walking forward until the cycle start also takes `a` steps. So a pointer restarted at head and a pointer at the meeting point, both moving 1 step at a time, meet exactly at the cycle start.

**Approach:**
1. Move `slow` by 1 and `fast` by 2 until they meet or `fast` reaches `null`.
2. If `fast` reached `null`, there is no cycle.
3. Reset one pointer to `head`, then advance both by 1 until they meet. That node is the cycle start.

**Pattern:** `Fast & Slow Pointers (Floyd's cycle detection)` - also used for finding the middle node and for finding a duplicate in an array (LeetCode 287).  
**Complexity:** O(n) time · O(1) space

```java
static ListNode detectCycleStart(ListNode head) {
    ListNode slow = head, fast = head;

    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;

        if (slow == fast) {
            ListNode fromHead = head;
            while (fromHead != slow) {
                fromHead = fromHead.next;
                slow = slow.next;
            }
            return fromHead;   // cycle entry node
        }
    }
    return null;   // no cycle
}
```

**Explain the math:** Distance from head to cycle start = distance from meeting point to cycle start.

---

## 2.9 Two Sum

**Problem:** Given an array and a target, return the indices of two different elements that add up to the target. Assume exactly one solution exists. Example: `[2,7,11,15]` with target `9` gives `[0,1]`.

**Link:** [LeetCode 1 — Two Sum](https://leetcode.com/problems/two-sum/)  
**Intuition:** For each number `x`, the partner you need is `target - x`. Checking all pairs costs O(n²). Instead, remember each value and its index as you go; a hash lookup then answers "has the complement already appeared?" in O(1).

**Approach:**
1. Create a map from value to index.
2. For each index `i`, compute `needed = target - nums[i]`. If `needed` is in the map, return the two indices.
3. Otherwise store `nums[i] -> i`.

**Pattern:** `Hash Map complement lookup` - if the array were sorted, two pointers from both ends would work in O(1) extra space.  
**Complexity:** O(n) average time · O(n) space

```java
static int[] twoSum(int[] nums, int target) {
    Map<Integer, Integer> indexByValue = new HashMap<>();

    for (int i = 0; i < nums.length; i++) {
        int needed = target - nums[i];
        if (indexByValue.containsKey(needed)) {
            return new int[]{indexByValue.get(needed), i};
        }
        indexByValue.put(nums[i], i);
    }
    return new int[]{-1, -1};   // no solution found
}
// Input:  nums=[2,7,11,15], target=9
// Output: [0, 1]
```

**Variants:** Sorted array → two-pointer O(1) space · All pairs · No duplicates in output.

---

## 2.10 Swap Two Strings

**Problem:** Given two strings `a` and `b`, return them swapped.

**Link:** [TryExponent — Deutsche Bank SE Experience](https://www.tryexponent.com/experiences/deutsche-bank-software-engineer-interview-ea98bb)  
**Intuition:** Swapping needs a holding place so that neither value is lost when it is overwritten. In Java you only move references; the characters are not copied. `String` is immutable, so there is nothing to mutate in place.

**Approach:**
1. Save `a` in `temp`.
2. Set `a = b`.
3. Set `b = temp`.

**Pattern:** `Temporary variable swap` - XOR and arithmetic tricks apply to primitives or char arrays only. Clarify with the interviewer what "no third variable" means.  
**Complexity:** O(1)

```java
static String[] swapStrings(String a, String b) {
    String temp = a;
    a = b;
    b = temp;
    return new String[]{a, b};
}

// If truly no third variable is allowed (swapping primitives conceptually):
static void swapDemo() {
    String a = "hello", b = "world";
    // XOR trick works on int/char arrays, not String references in Java.
    // The only meaningful "no third variable" trick for Strings:
    a = a + b;          // a = "helloworld"
    b = a.substring(0, a.length() - b.length()); // b = "hello"
    a = a.substring(b.length());                  // a = "world"
    // But this is just a party trick; clarify with interviewer.
}
```

**Interview point:** Explain why XOR does not work on Java `String` references.

---

## 2.11 Replace Zero Digits with 8 (No String Conversion)

**Problem:** Replace every digit `0` in a number with `8` without converting the number to a string. Example: `10203` gives `18283`; `0` gives `8`.

**Link:** [GFG — Deutsche Bank FTE On-Campus Virtual 2020](https://www.geeksforgeeks.org/interview-experiences/deutsche-bank-interview-experience-for-fte-on-campus-virtual-2020/)  
**Intuition:** Process digits from the least significant end: `n % 10` gives the last digit and `n / 10` removes it. Rebuild the answer by placing each digit (after replacement) at its power of 10 using a `place` multiplier.

**Approach:**
1. Return 8 if the input is 0.
2. Record the sign and work with the absolute value.
3. Loop: `digit = n % 10`; if it is 0 use 8; `result += digit * place`; then `place *= 10` and `n /= 10`.
4. Reapply the sign.

**Pattern:** `Digit manipulation with % and /` - the same loop handles reversing a number, digit sums, Armstrong numbers, and palindrome numbers.  
**Complexity:** O(d) where d = number of digits · O(1) space

```java
static long replaceZeroWithEight(long number) {
    if (number == 0) return 8;

    boolean negative = number < 0;
    long n = Math.abs(number);   // Note: Math.abs(Long.MIN_VALUE) overflows — discuss this
    long result = 0;
    long place  = 1;

    while (n > 0) {
        long digit = n % 10;
        if (digit == 0) digit = 8;
        result += digit * place;
        place  *= 10;
        n      /= 10;
    }
    return negative ? -result : result;
}
// Input:  10203  →  Output: 18283
// Input:  0      →  Output: 8
```

---

## 2.12 Armstrong Number

**Problem:** Check whether `n` is an Armstrong number, meaning it equals the sum of its digits, each raised to the power of the digit count. Example: `153 = 1³ + 5³ + 3³`, so it returns `true`.

**Link:** [GFG — Deutsche Bank FTE On-Campus Virtual 2020](https://www.geeksforgeeks.org/interview-experiences/deutsche-bank-interview-experience-for-fte-on-campus-virtual-2020/)  
**Intuition:** The exponent depends on the number of digits `d`, so you must know `d` before summing. Count the digits first, then peel them off with `% 10` and `/ 10`, adding `digit^d` each time.

**Approach:**
1. Return false for negative numbers.
2. Count the digits `d`.
3. Loop over the digits, adding `digit^d` to `sum`.
4. Return `sum == n`.

**Pattern:** `Digit decomposition` - use `long` for the sum so large powers do not overflow.  
**Complexity:** O(d) where d = number of digits · O(1) space

```java
static boolean isArmstrong(int n) {
    if (n < 0) return false;

    int digits = countDigits(n);
    int value  = n;
    long sum   = 0;

    while (value > 0) {
        int digit = value % 10;
        sum  += intPow(digit, digits);
        value /= 10;
    }
    return sum == n;
}

static int countDigits(int n) {
    if (n == 0) return 1;
    int count = 0;
    while (n > 0) { count++; n /= 10; }
    return count;
}

static long intPow(int base, int exp) {
    long result = 1;
    for (int i = 0; i < exp; i++) result *= base;
    return result;
}
// Armstrong numbers: 1, 153, 370, 371, 407
// isArmstrong(153) → true  (1³ + 5³ + 3³ = 153)
```

---

## 2.13 Palindrome Check & String Reversal

**Problem:** (a) Check whether a string reads the same forwards and backwards. (b) Reverse a string. Common variants: ignore non-alphanumeric characters, or reverse only the vowels.

**Intuition:** A palindrome matches at mirrored positions, so compare the outermost characters and move inward. Reversal swaps the same mirrored pairs, so the same two-pointer walk works.

**Approach:**
1. Set `left = 0` and `right = n - 1`.
2. While `left < right`, compare the characters; on a mismatch return false, otherwise move both pointers inward.
3. For reversal, swap `chars[left]` and `chars[right]` in a char array.
4. For the alphanumeric variant, skip non-alphanumeric characters with inner loops and compare case-insensitively.

**Pattern:** `Two Pointers (converging from both ends)` - for reversing only vowels, move each pointer to the next vowel before swapping.

**Links:**  
- [LeetCode 125 — Valid Palindrome](https://leetcode.com/problems/valid-palindrome/)  
- [LeetCode 344 — Reverse String](https://leetcode.com/problems/reverse-string/)  
- [LeetCode 345 — Reverse Vowels of a String](https://leetcode.com/problems/reverse-vowels-of-a-string/)

**Complexity:** O(n) time · O(1) extra space (in-place char array)

```java
static boolean isPalindrome(String s) {
    int left = 0, right = s.length() - 1;
    while (left < right) {
        if (s.charAt(left) != s.charAt(right)) return false;
        left++;
        right--;
    }
    return true;
}

// LeetCode 125 variant: ignore non-alphanumeric, case-insensitive
static boolean isPalindromeAlphanumeric(String s) {
    int left = 0, right = s.length() - 1;
    while (left < right) {
        while (left < right && !Character.isLetterOrDigit(s.charAt(left)))  left++;
        while (left < right && !Character.isLetterOrDigit(s.charAt(right))) right--;
        if (Character.toLowerCase(s.charAt(left)) !=
            Character.toLowerCase(s.charAt(right))) return false;
        left++;
        right--;
    }
    return true;
}

static String reverseString(String s) {
    char[] chars = s.toCharArray();
    int left = 0, right = chars.length - 1;
    while (left < right) {
        char temp    = chars[left];
        chars[left]  = chars[right];
        chars[right] = temp;
        left++; right--;
    }
    return new String(chars);
}
```

---

## 2.14 Queue Using Two Stacks

**Problem:** Implement a FIFO queue with `offer`, `poll`, and `peek` using only LIFO stacks.

**Link:** [GFG — Queue Using Stacks](https://www.geeksforgeeks.org/queue-using-stacks/) · [LeetCode 232](https://leetcode.com/problems/implement-queue-using-stacks/)  
**Intuition:** A stack reverses order, and reversing twice restores it. Push new items onto an `in` stack. When a dequeue is needed, pop everything from `in` onto `out`; this reverses them, so the oldest item ends up on top of `out`.

**Approach:**
1. `offer` pushes onto `in`.
2. `poll` and `peek` first move everything from `in` to `out`, but only if `out` is empty.
3. Then pop or peek on `out`.

**Pattern:** `Two-Stack technique / Amortized analysis` - each element moves at most once, so n operations cost O(n) in total, which is amortized O(1) each.  
**Complexity:** Amortised O(1) enqueue & dequeue · O(n) space

```java
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;

class QueueUsingStacks {
    private final Deque<Integer> in  = new ArrayDeque<>();
    private final Deque<Integer> out = new ArrayDeque<>();

    void offer(int x) {
        in.push(x);
    }

    int poll() {
        transfer();
        if (out.isEmpty()) throw new NoSuchElementException("Queue is empty");
        return out.pop();
    }

    int peek() {
        transfer();
        if (out.isEmpty()) throw new NoSuchElementException("Queue is empty");
        return out.peek();
    }

    boolean isEmpty() { return in.isEmpty() && out.isEmpty(); }

    private void transfer() {
        if (out.isEmpty()) {
            while (!in.isEmpty()) out.push(in.pop());
        }
    }
}
```

**Key insight:** Each element moves from `in` to `out` at most once → amortised O(1).

---

## 2.15 Find Duplicates & Their Frequencies

**Problem:** Count how often each value occurs in an array, and return the values that occur more than once. Example: `[4,3,2,7,8,2,3,1]` gives duplicates `[2, 3]`.

**Link:** [LeetCode 442 — Find All Duplicates in an Array](https://leetcode.com/problems/find-all-duplicates-in-an-array/)  
**Intuition:** Counting occurrences means aggregating by value, and a hash map from value to count does that in one pass. Duplicates are the entries whose count is greater than 1.

**Approach:**
1. Iterate the array and call `freq.merge(x, 1, Integer::sum)`.
2. Iterate the map entries and keep the keys whose count is greater than 1.

**Pattern:** `Frequency Counting (HashMap)` - if values lie in a small known range such as 1..n, an int array is faster. LeetCode 442 achieves O(1) extra space by negating indices.  
**Complexity:** O(n) average time · O(n) space

```java
static Map<Integer, Integer> frequencies(int[] nums) {
    Map<Integer, Integer> freq = new HashMap<>();
    for (int x : nums) {
        freq.merge(x, 1, Integer::sum);   // cleaner than getOrDefault
    }
    return freq;
}

static List<Integer> duplicates(int[] nums) {
    Map<Integer, Integer> freq = frequencies(nums);
    List<Integer> result = new ArrayList<>();
    for (Map.Entry<Integer, Integer> e : freq.entrySet()) {
        if (e.getValue() > 1) result.add(e.getKey());
    }
    return result;
}
// Input:  [4,3,2,7,8,2,3,1]
// Duplicates: [2, 3]
```

**O(1) space variant (LeetCode 442):** For arrays with values in range [1..n], negate visited index to mark presence.

---

## 2.16 LRU Cache

**Problem:** Design a fixed-capacity cache with `get(key)` and `put(key, value)` in O(1) time. When the cache is full, evict the least recently used entry. Both `get` and `put` count as a use.

**Intuition:** Two requirements: O(1) lookup, which a HashMap provides, and O(1) reordering by recency plus eviction, which a doubly linked list provides. Keep the most recently used node next to the head and the least recently used next to the tail. Given the node from the map, moving it to the front or removing the tail takes only a few pointer updates.

**Approach:**
1. Map each key to its node. The list has dummy `head` and `tail` sentinels.
2. `get`: if the key exists, unlink its node, insert it after `head`, and return its value.
3. `put`: if the key exists, unlink and remove the old node. If the cache is at capacity, remove `tail.prev` from both the list and the map. Insert the new node after `head` and add it to the map.
4. Shortcut in Java: `LinkedHashMap` with `accessOrder = true` and an overridden `removeEldestEntry`.

**Pattern:** `Design + HashMap & Doubly Linked List` - common in design questions, where you combine two structures so that every operation is O(1).

**Link:** [LeetCode 146 — LRU Cache](https://leetcode.com/problems/lru-cache/)  
**Approach A (interview-friendly):** `LinkedHashMap` with access order.  
**Approach B (manual):** `HashMap` + doubly linked list.  
**Complexity:** O(1) average get/put

```java
// Approach A: LinkedHashMap
class LRUCache extends LinkedHashMap<Integer, Integer> {
    private final int capacity;

    LRUCache(int capacity) {
        super(capacity, 0.75f, true);   // true = access order
        this.capacity = capacity;
    }

    public int get(int key)              { return getOrDefault(key, -1); }
    public void put(int key, int value)  { super.put(key, value); }

    @Override
    protected boolean removeEldestEntry(Map.Entry<Integer, Integer> eldest) {
        return size() > capacity;
    }
}

// Approach B: Manual HashMap + Doubly Linked List
class LRUCacheManual {
    private static class Node {
        int key, val;
        Node prev, next;
        Node(int k, int v) { key = k; val = v; }
    }

    private final int capacity;
    private final Map<Integer, Node> map = new HashMap<>();
    private final Node head = new Node(0, 0);  // dummy
    private final Node tail = new Node(0, 0);  // dummy

    LRUCacheManual(int capacity) {
        this.capacity = capacity;
        head.next = tail;
        tail.prev = head;
    }

    public int get(int key) {
        if (!map.containsKey(key)) return -1;
        Node node = map.get(key);
        remove(node);
        insertAtFront(node);
        return node.val;
    }

    public void put(int key, int value) {
        if (map.containsKey(key)) {
            remove(map.get(key));
            map.remove(key);
        }
        if (map.size() == capacity) {
            Node lru = tail.prev;
            remove(lru);
            map.remove(lru.key);
        }
        Node node = new Node(key, value);
        insertAtFront(node);
        map.put(key, node);
    }

    private void remove(Node n) {
        n.prev.next = n.next;
        n.next.prev = n.prev;
    }

    private void insertAtFront(Node n) {
        n.next = head.next;
        n.prev = head;
        head.next.prev = n;
        head.next = n;
    }
}
```

---

## 2.17 Heap Sort

**Problem:** Sort an integer array in O(n log n) time, in place, using a binary heap.

**Link:** [GFG — Heap Sort](https://www.geeksforgeeks.org/heap-sort/)  
**Intuition:** A max-heap keeps the largest element at the root. If you repeatedly move the root to the end of the array and shrink the heap, the largest elements settle into their final positions. This is selection sort where each selection costs O(log n) instead of O(n).

**Approach:**
1. Heapify: for `i` from `n/2 - 1` down to 0, sift down.
2. Repeat: swap `a[0]` with `a[end]`, reduce the heap size by 1, and sift down the new root.

**Pattern:** `Heap / Priority Queue` - also used for top-K problems, where you keep a min-heap of size K.  
**Complexity:** O(n log n) time · O(1) auxiliary space · Not stable

```java
static void heapSort(int[] a) {
    int n = a.length;

    for (int i = n / 2 - 1; i >= 0; i--) siftDown(a, n, i);

    for (int end = n - 1; end > 0; end--) {
        int temp = a[0]; a[0] = a[end]; a[end] = temp;
        siftDown(a, end, 0);
    }
}

static void siftDown(int[] a, int size, int root) {
    while (true) {
        int largest = root;
        int left    = 2 * root + 1;
        int right   = 2 * root + 2;

        if (left  < size && a[left]  > a[largest]) largest = left;
        if (right < size && a[right] > a[largest]) largest = right;
        if (largest == root) return;

        int temp    = a[root]; a[root] = a[largest]; a[largest] = temp;
        root = largest;
    }
}
// Compare with merge sort: heap sort wins on space (O(1) vs O(n)); merge sort wins on cache locality and stability.
```

---

## 2.18 Count Digits and Alphabetic Characters

**Problem:** Count the digit characters and the letter characters in a string. Example: `"a1b2c3!"` gives `[3, 3]`.

**Intuition:** Each character falls into exactly one class, so one pass with a classification check per character is enough.

**Approach:**
1. Keep two counters.
2. For each character, use `Character.isDigit` or `Character.isLetter` to increment the matching counter.
3. Return both counts.

**Pattern:** `Single-pass counting / Character classification`.

**Link:** [GFG — Deutsche Bank FTE On-Campus](https://www.geeksforgeeks.org/interview-experiences/deutsche-bank-interview-experience-on-campus-fte/)  
**Complexity:** O(n) time · O(1) space

```java
static int[] countDigitsAndLetters(String s) {
    int digits = 0, letters = 0;
    for (int i = 0; i < s.length(); i++) {
        char ch = s.charAt(i);
        if (Character.isDigit(ch))       digits++;
        else if (Character.isLetter(ch)) letters++;
    }
    return new int[]{digits, letters};
}
// Input:  "a1b2c3!"
// Output: [3, 3]  (digits=3, letters=3)
```

---

## 2.19 Lexicographically Smallest String of Length n with Character Sum k

**Problem:** Given `n` and `k`, build the lexicographically smallest string of length `n` whose character values sum to `k`, where `a=1` through `z=26`. Example: `n=5, k=42` gives `"aaamz"`.

**Link:** [GFG — Deutsche Bank FTE On-Campus](https://www.geeksforgeeks.org/interview-experiences/deutsche-bank-interview-experience-on-campus-fte/)  
**Intuition:** To make the string lexicographically smallest, the early positions should be as small as possible. Start with all `a` (minimum sum `n`) and spend the remaining budget `k - n` from the right end first, because changes there affect the order least. Each position can absorb at most 25 extra, which turns `a` into `z`.

**Approach:**
1. Validate that `n <= k <= 26n`.
2. Fill the result with `a`, and set `extra = k - n`.
3. For `i` from `n-1` down to 0, add `min(25, extra)` to `result[i]` and subtract it from `extra`.
4. Return the string.

**Pattern:** `Greedy from the end` - give the smallest possible values to the earliest positions while the remaining budget stays feasible.  
**Complexity:** O(n) time · O(n) space

```java
static String smallestString(int n, int k) {
    if (n <= 0 || k < n || k > 26 * n)
        throw new IllegalArgumentException("Impossible n and k");

    char[] result = new char[n];
    Arrays.fill(result, 'a');
    int extra = k - n;   // each 'a' contributes 1; we need to add 'extra' more

    for (int i = n - 1; i >= 0 && extra > 0; i--) {
        int add = Math.min(25, extra);   // 'a'+25 = 'z'
        result[i] += add;
        extra -= add;
    }
    return new String(result);
}
// smallestString(5, 42) → "aaamz"
// smallestString(3, 27) → "aay"
```

---

## 2.20 Bubble Sort

**Problem:** Sort an integer array with bubble sort, stopping early if a pass makes no swaps.

**Link:** [GFG — Bubble Sort](https://www.geeksforgeeks.org/bubble-sort/)  
**Intuition:** After each pass, the largest unsorted element has moved to its final position at the end. If a pass performs no swaps, every adjacent pair is in order and the array is sorted.

**Approach:**
1. The outer loop runs one pass per index `i`.
2. The inner loop compares adjacent elements up to the unsorted boundary `n - i - 1` and swaps when out of order.
3. Track whether a swap happened and break early if not.

**Pattern:** `Adjacent comparison / Early termination` - mainly useful for discussing complexity and stability. In practice use `Arrays.sort`.  
**Complexity:** O(n²) worst/average · O(n) best (with early exit) · O(1) space · Stable

```java
static void bubbleSort(int[] a) {
    int n = a.length;
    for (int i = 0; i < n - 1; i++) {
        boolean swapped = false;
        for (int j = 0; j < n - i - 1; j++) {
            if (a[j] > a[j + 1]) {
                int temp = a[j]; a[j] = a[j + 1]; a[j + 1] = temp;
                swapped = true;
            }
        }
        if (!swapped) break;   // already sorted — O(n) best case
    }
}
```

---

## 2.21 Binary Search

**Problem:** Given a sorted array, return the index of `target` in O(log n) time, or -1 if it is absent.

**Link:** [LeetCode 704 — Binary Search](https://leetcode.com/problems/binary-search/)  
**Intuition:** Comparing the middle element with the target shows which half could contain it. The other half can be discarded, so each step halves the search space.

**Approach:**
1. Set `low = 0` and `high = n - 1`.
2. Compute `mid = low + (high - low) / 2` to avoid overflow.
3. If equal, return `mid`; if smaller, set `low = mid + 1`; if larger, set `high = mid - 1`.

**Pattern:** `Binary Search on a monotonic condition` - the same template solves "search on the answer" problems.  
**Complexity:** O(log n) time · O(1) space

```java
static int binarySearch(int[] nums, int target) {
    int low = 0, high = nums.length - 1;

    while (low <= high) {
        int mid = low + (high - low) / 2;   // avoids overflow vs (low+high)/2

        if (nums[mid] == target) return mid;
        else if (nums[mid] < target) low  = mid + 1;
        else                         high = mid - 1;
    }
    return -1;   // not found
}
// Input:  [1,3,5,7,9,11], target=7
// Output: 3
```

**Variants:** Find first/last occurrence (left/right boundary binary search) · Rotated sorted array (LeetCode 33).

---

## 2.22 Reverse a Linked List

**Problem:** Reverse a singly linked list. Example: `1->2->3->4->5` gives `5->4->3->2->1`.

**Link:** [LeetCode 206 — Reverse Linked List](https://leetcode.com/problems/reverse-linked-list/)  
**Intuition:** Reversing means each node's `next` must point to its predecessor. Walk the list while tracking the previous node, and save the original `next` before breaking the link.

**Approach:**
1. Start with `prev = null` and `curr = head`.
2. Save `next = curr.next`, set `curr.next = prev`, then advance `prev` and `curr`.
3. Return `prev`, which is the new head.

Recursive version: reverse the rest of the list, then make `head.next` point back to `head`.

**Pattern:** `Pointer manipulation (three-pointer walk)` - the building block for list palindromes, reordering lists, and reversing in k-groups.  
**Complexity:** O(n) time · O(1) space (iterative)

```java
static ListNode reverseList(ListNode head) {
    ListNode prev = null, curr = head;

    while (curr != null) {
        ListNode next = curr.next;
        curr.next = prev;
        prev = curr;
        curr = next;
    }
    return prev;
}

// Recursive version
static ListNode reverseListRecursive(ListNode head) {
    if (head == null || head.next == null) return head;

    ListNode newHead = reverseListRecursive(head.next);
    head.next.next = head;
    head.next = null;
    return newHead;
}
// Input:  1->2->3->4->5
// Output: 5->4->3->2->1
```

---

## 2.23 Check Anagram

**Problem:** Determine whether `t` is an anagram of `s`, meaning both contain the same characters with the same frequencies.

**Link:** [LeetCode 242 — Valid Anagram](https://leetcode.com/problems/valid-anagram/)  
**Intuition:** Two strings are anagrams exactly when their character counts match. Instead of building two count tables, add counts for `s` and subtract counts for `t` in the same array. Matching characters cancel to zero.

**Approach:**
1. If the lengths differ, return false.
2. For each index `i`, do `count[s[i]]++` and `count[t[i]]--`.
3. If any count is non-zero, return false; otherwise return true.

**Pattern:** `Frequency counting` - also used in group anagrams. Sorting both strings is an O(n log n) alternative.  
**Complexity:** O(n) time · O(1) space (fixed 26-entry array)

```java
static boolean isAnagram(String s, String t) {
    if (s.length() != t.length()) return false;

    int[] count = new int[26];
    for (int i = 0; i < s.length(); i++) {
        count[s.charAt(i) - 'a']++;
        count[t.charAt(i) - 'a']--;
    }
    for (int c : count) if (c != 0) return false;
    return true;
}
// isAnagram("anagram", "nagaram") → true
// isAnagram("rat", "car")        → false
```

**Unicode variant:** Use `HashMap<Character, Integer>` instead of fixed array.

---

## 2.24 Fibonacci — DP / Memoization

**Problem:** Return the nth Fibonacci number, where `F(0) = 0`, `F(1) = 1`, and `F(n) = F(n-1) + F(n-2)`.

**Link:** [LeetCode 509 — Fibonacci Number](https://leetcode.com/problems/fibonacci-number/)  
**Intuition:** `F(n)` depends only on the previous two values. The naive recursion recomputes the same subproblems and takes exponential time. Either cache each result (memoization) or build up from the bottom while keeping only the last two values.

**Approach:**
1. Handle `n = 0` and `n = 1`.
2. Set `prev = 0` and `curr = 1`; for `i` from 2 to `n`, set `next = prev + curr` and slide both values forward.
3. Return `curr`.

**Pattern:** `Dynamic Programming (overlapping subproblems, optimal substructure)` - top-down memoization, bottom-up tabulation, then space optimization to O(1).  
**Complexity:** O(n) time · O(1) space

```java
// Iterative O(n) time, O(1) space
static long fibonacci(int n) {
    if (n <= 0) return 0;
    if (n == 1) return 1;

    long prev = 0, curr = 1;
    for (int i = 2; i <= n; i++) {
        long next = prev + curr;
        prev = curr;
        curr = next;
    }
    return curr;
}

// Top-down memoization
static long fibMemo(int n, long[] memo) {
    if (n <= 1) return n;
    if (memo[n] != 0) return memo[n];
    memo[n] = fibMemo(n - 1, memo) + fibMemo(n - 2, memo);
    return memo[n];
}
// fibonacci(10) → 55
```

---

## 2.25 Count Elements Greater Than Mean of Neighbours

**Problem:** Count the indices `i` (for `1 <= i <= n-2`) where `a[i]` is greater than the mean of its two neighbours `a[i-1]` and `a[i+1]`. Example: `[1,5,2,8,3]` gives `2`.

**Link:** [GFG — Deutsche Bank FTE On-Campus](https://www.geeksforgeeks.org/interview-experiences/deutsche-bank-interview-experience-on-campus-fte/)  
**Intuition:** The mean of two values is `(x + y) / 2`. Multiply both sides by 2 to avoid division and floating-point error: `2 * a[i] > a[i-1] + a[i+1]`. Each interior index is checked independently, so one pass is enough.

**Approach:**
1. If `n < 3`, return 0 because there are no interior indices.
2. Loop `i` from 1 to `n-2`, test the integer inequality, and count the matches.

**Pattern:** `Local neighbourhood check (window of size 3)` - clarify with the interviewer whether the endpoints can count.  
**Complexity:** O(n) time · O(1) space

```java
static int countGreaterThanNeighbourMean(int[] a) {
    if (a == null || a.length < 3) return 0;

    int count = 0;
    for (int i = 1; i < a.length - 1; i++) {
        // Use integer arithmetic: a[i]*2 > a[i-1]+a[i+1] avoids floating point
        if (2 * a[i] > a[i - 1] + a[i + 1]) count++;
    }
    return count;
}
// Input:  [1, 5, 2, 8, 3]
// i=1: 2*5=10 > 1+2=3  → yes
// i=2: 2*2=4  > 5+8=13 → no
// i=3: 2*8=16 > 2+3=5  → yes
// Output: 2
```

---

## 2.26 Range Updates — Difference Array

**Problem:** Start with an array of `n` zeros. Apply many updates, each adding `val` to every index from `l` to `r` inclusive. Return the final array.

**Link:** [GFG — Deutsche Bank Campus Experience](https://www.geeksforgeeks.org/interview-experiences/deutsche-bank-interview-experience-set-6-campus/)  
**Intuition:** Updating every element in every range costs O(n·q). Instead, record each change only at its boundaries: add `val` at index `l` and subtract it at `r+1`. A single prefix sum then spreads each change across its range, for O(n + q) total.

**Approach:**
1. Create `diff` of size `n + 1`, all zeros.
2. For each update `(l, r, v)`: `diff[l] += v` and `diff[r+1] -= v`.
3. Take a running prefix sum of `diff`; the running value at index `i` is `result[i]`.

**Pattern:** `Difference Array / Prefix Sum` - related to range-sum queries with prefix sums and to booking or flight-count problems.  
**Complexity:** O(n + q) total (q queries) · O(n) space

```java
static int[] rangeUpdate(int n, int[][] updates) {
    int[] diff = new int[n + 1];

    for (int[] u : updates) {
        int l = u[0], r = u[1], val = u[2];
        diff[l]     += val;
        diff[r + 1] -= val;
    }

    int[] result = new int[n];
    int running = 0;
    for (int i = 0; i < n; i++) {
        running   += diff[i];
        result[i]  = running;
    }
    return result;
}
// n=5, updates=[[1,3,2],[2,4,3],[0,2,-1]]
// Result: [-1, 1, 4, 5, 3]
```

---

## Sorting Complexity Quick Reference

| Algorithm  | Best     | Average  | Worst    | Space | Stable |
|------------|----------|----------|----------|-------|--------|
| Bubble     | O(n)     | O(n²)    | O(n²)    | O(1)  | Yes    |
| Merge      | O(n log n)| O(n log n)| O(n log n)| O(n) | Yes   |
| Heap       | O(n log n)| O(n log n)| O(n log n)| O(1) | No    |
| Quick      | O(n log n)| O(n log n)| O(n²)    | O(log n)| No  |

---

## Common Data Structure Complexities

| Structure       | Access | Search | Insert | Delete |
|-----------------|--------|--------|--------|--------|
| Array           | O(1)   | O(n)   | O(n)   | O(n)   |
| LinkedList      | O(n)   | O(n)   | O(1)   | O(1)   |
| HashMap         | —      | O(1)   | O(1)   | O(1)   |
| TreeMap         | —      | O(log n)| O(log n)| O(log n)|
| Min/Max Heap    | O(1) peek | O(n) | O(log n) | O(log n) |
| Stack/Queue     | —      | O(n)   | O(1)   | O(1)   |

---

## Last-Minute Reminders

1. **Clarify constraints first** — array size, value range, null inputs, Unicode vs ASCII.
2. **State brute force → optimisation** — show your thinking process.
3. **Dry-run on a small example** before declaring done.
4. **Always state complexity** — time AND space.
5. **Common edge cases:** empty/null input · single element · all same elements · negative numbers · k = 0 or k > n.
6. **`int` overflow** — use `long` for products/sums, `low + (high - low) / 2` for mid.
7. **Java imports to remember:**
   ```java
   import java.util.*;   // HashMap, ArrayList, PriorityQueue, Deque, ArrayDeque
   import java.util.stream.*;
   import java.util.Arrays;
   ```
