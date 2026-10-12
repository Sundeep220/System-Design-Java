# Deutsche Bank SDE Interview Question Bank

## Candidate-reported questions, technical answers, DSA links, and Java solutions

**Prepared:** October 2026\
**Focus:** Software Engineer / SDE / Java Developer interviews, with
Java as the primary DSA language.

> **Important scope note:** No public source contains every question
> Deutsche Bank has ever asked. This handbook consolidates the
> candidate-reported questions found in the linked public interview
> experiences and adds closely related practice questions. Reports vary
> by year, country, campus vs experienced hiring, role, and interviewer.
> "Reported" means a candidate or public interview-prep source described
> it; it does not guarantee the question will recur.

## Contents

1.  How to use this question bank
2.  Candidate-reported DSA questions, with links and Java solutions
3.  Other reported coding questions and solution approaches
4.  Core Java and OOP --- questions and answers
5.  Java 8, collections, concurrency --- questions and answers
6.  Spring Boot, REST, microservices, Kafka --- questions and answers
7.  SQL and DBMS --- questions, answers, and queries
8.  Operating systems, networking, and CS fundamentals
9.  Projects, behavioural, and techno-managerial questions
10. Mock interview checklist
11. Public sources and links

------------------------------------------------------------------------

## 1. How to use this question bank

Suggested order:

1.  **DSA:** solve the linked questions in Java without looking at the
    solution first.
2.  **Core Java:** prepare concise explanations and examples for OOP,
    collections, immutability, Java 8, and concurrency.
3.  **Backend:** prepare Spring Boot layers, REST, transactions,
    microservices resilience, and Kafka basics.
4.  **SQL/DBMS:** practise joins, grouping, indexes, second-highest
    salary, DDL/DML, and transactions.
5.  **Resume/projects:** know every technology, design choice,
    trade-off, and metric on your resume.

For each coding question: - Restate the problem and confirm
assumptions. - Explain a brute-force approach if applicable. - Explain
the optimal approach and data structure. - Write readable Java code. -
Test a normal case and edge cases. - State time and space complexity.

------------------------------------------------------------------------

# 2. Candidate-reported DSA questions --- links and Java solutions

The links below lead to the original problem or the interview experience
that reported it. Some reports describe a problem in prose rather than
giving an exact LeetCode ID.

## 2.1 Remove duplicates from a string

**Reported in:** Deutsche Bank SDE on-campus technical interview.\
**Problem link:** [GeeksforGeeks --- Remove duplicates from a given
string](https://www.geeksforgeeks.org/dsa/remove-duplicates-from-a-given-string/)

**Approach:** Track characters already seen. Append only the first
occurrence. This version preserves the order of first appearance and
assumes ASCII characters.

``` java
static String removeDuplicates(String s) {
    boolean[] seen = new boolean[256];
    StringBuilder result = new StringBuilder();

    for (int i = 0; i < s.length(); i++) {
        char ch = s.charAt(i);
        if (ch < 256 && !seen[ch]) {
            seen[ch] = true;
            result.append(ch);
        } else if (ch >= 256) {
            // For general Unicode, use a Set<Character> or code points.
            result.append(ch);
        }
    }
    return result.toString();
}
```

**Complexity:** O(n) time, O(1) extra space for the fixed 256-character
table (excluding output). For general Unicode input, prefer a
`HashSet<Character>` or code-point-aware solution.

**Follow-ups:** What if the string contains Unicode? Should duplicates
be case-sensitive? Should the first or last occurrence be kept?

## 2.2 Merge sort

**Reported in:** A candidate described being asked to explain sorting
algorithms and implement merge sort.\
**Practice link:** [GeeksforGeeks --- Merge
Sort](https://www.geeksforgeeks.org/merge-sort/)

``` java
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
        else temp[k++] = a[j++];
    }
    while (i <= mid) temp[k++] = a[i++];
    while (j <= right) temp[k++] = a[j++];

    for (int x = 0; x < temp.length; x++) {
        a[left + x] = temp[x];
    }
}
```

**Complexity:** O(n log n) time; O(n) auxiliary space for the standard
array implementation. It is stable when equal elements from the left
half are chosen first.

## 2.3 Maximum subarray sum --- Kadane's algorithm

**Reported in:** Deutsche Bank interview experience where the
interviewer asked for the largest sum of a contiguous subarray.\
**Problem link:** [GeeksforGeeks --- Largest Sum Contiguous
Subarray](https://www.geeksforgeeks.org/dsa/largest-sum-contiguous-subarray/)

``` java
static int maxSubArray(int[] nums) {
    if (nums == null || nums.length == 0) {
        throw new IllegalArgumentException("Array must not be empty");
    }

    int current = nums[0];
    int best = nums[0];

    for (int i = 1; i < nums.length; i++) {
        current = Math.max(nums[i], current + nums[i]);
        best = Math.max(best, current);
    }
    return best;
}
```

**Complexity:** O(n) time, O(1) extra space. Initialising from the first
element handles arrays containing only negative numbers.

## 2.4 Connect N ropes with minimum cost

**Reported in:** Deutsche Bank online coding round.\
**Problem link:** [GeeksforGeeks --- Connect N Ropes with Minimum
Cost](https://www.geeksforgeeks.org/dsa/connect-n-ropes-minimum-cost/)

``` java
static long minCostToConnectRopes(int[] ropes) {
    PriorityQueue<Long> minHeap = new PriorityQueue<>();
    for (int rope : ropes) minHeap.offer((long) rope);

    long totalCost = 0;
    while (minHeap.size() > 1) {
        long first = minHeap.poll();
        long second = minHeap.poll();
        long combined = first + second;
        totalCost += combined;
        minHeap.offer(combined);
    }
    return totalCost;
}
```

**Complexity:** O(n log n) time, O(n) space. Always combine the two
shortest ropes first.

## 2.5 Count paths in an N × M grid

**Reported in:** Deutsche Bank online coding round. The report describes
moving from top-left to bottom-right. Confirm allowed moves and whether
obstacles exist.\
**Problem link:** [GeeksforGeeks --- Count Possible Paths in an N×M
Matrix](https://www.geeksforgeeks.org/dsa/count-possible-paths-top-left-bottom-right-nxm-matrix/)

Assumption: only right/down moves, no obstacles.

``` java
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
```

**Complexity:** O(rows × cols) time, O(cols) space. Use `BigInteger` or
modulo arithmetic if constraints make the answer exceed `long`.

## 2.6 Longest common substring

**Reported in:** Deutsche Bank online coding round. A substring must be
contiguous; do not confuse it with longest common subsequence.\
**Problem link:** [GeeksforGeeks --- Longest Common
Substring](https://www.geeksforgeeks.org/dsa/longest-common-substring-dp-29/)

``` java
static int longestCommonSubstring(String a, String b) {
    int[] previous = new int[b.length() + 1];
    int best = 0;

    for (int i = 1; i <= a.length(); i++) {
        int[] current = new int[b.length() + 1];

        for (int j = 1; j <= b.length(); j++) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) {
                current[j] = previous[j - 1] + 1;
                best = Math.max(best, current[j]);
            }
            // Nonmatching cells remain zero: substring must be contiguous.
        }
        previous = current;
    }
    return best;
}
```

**Complexity:** O(mn) time, O(n) auxiliary space.

## 2.7 Rotate a singly linked list to the right by k

**Reported in:** Deutsche Bank Java Developer interview report.\
**Practice link:** [GeeksforGeeks --- Rotate a Linked
List](https://www.geeksforgeeks.org/rotate-a-linked-list/)

``` java
static class ListNode {
    int val;
    ListNode next;
    ListNode(int val) { this.val = val; }
}

static ListNode rotateRight(ListNode head, int k) {
    if (head == null || head.next == null || k == 0) return head;

    int length = 1;
    ListNode tail = head;
    while (tail.next != null) {
        tail = tail.next;
        length++;
    }

    k %= length;
    if (k == 0) return head;

    tail.next = head; // temporarily make a ring
    int stepsToNewTail = length - k - 1;
    ListNode newTail = head;

    for (int i = 0; i < stepsToNewTail; i++) {
        newTail = newTail.next;
    }

    ListNode newHead = newTail.next;
    newTail.next = null;
    return newHead;
}
```

**Complexity:** O(n) time, O(1) extra space. Clarify left vs right
rotation.

## 2.8 Detect a linked-list cycle and return its start

**Reported in:** Deutsche Bank interview experiences mention cycle
detection, including a twist on finding the loop.\
**Practice link:** [LeetCode 142 --- Linked List Cycle
II](https://leetcode.com/problems/linked-list-cycle-ii/)

``` java
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
            return fromHead;
        }
    }
    return null;
}
```

**Complexity:** O(n) time, O(1) extra space.

## 2.9 Two Sum

**Reported in:** Deutsche Bank candidate reports.\
**Problem link:** [LeetCode 1 --- Two
Sum](https://leetcode.com/problems/two-sum/)

``` java
static int[] twoSum(int[] nums, int target) {
    Map<Integer, Integer> indexByValue = new HashMap<>();

    for (int i = 0; i < nums.length; i++) {
        int needed = target - nums[i];

        if (indexByValue.containsKey(needed)) {
            return new int[]{indexByValue.get(needed), i};
        }
        indexByValue.put(nums[i], i);
    }
    return new int[]{-1, -1};
}
```

**Complexity:** Average O(n) time, O(n) space. Check the complement
before inserting the current number so the same array index is not used
twice.

## 2.10 Swap two strings without a third variable

**Reported in:** Deutsche Bank Software Engineer candidate report.\
**Reference:** [TryExponent --- Deutsche Bank Software Engineer
interview
experience](https://www.tryexponent.com/experiences/deutsche-bank-software-engineer-interview-ea98bb)

Java strings are immutable. The clean Java approach uses a temporary
variable, or a mutable character buffer if the problem specifically
requires swapping characters. Avoid arithmetic/XOR tricks for swapping
string variables; they do not apply to Java `String` references in the
intended way.

``` java
static String[] swapStrings(String a, String b) {
    String temp = a;
    a = b;
    b = temp;
    return new String[]{a, b};
}
```

**Interview point:** If the prompt literally prohibits a third variable,
clarify whether it means swapping primitive variables, references, or
the characters inside two strings. In Java, swapping local references
without a temporary variable is not a meaningful requirement in the same
way as swapping primitive values.

## 2.11 Remove all zero digits from a number and replace them with 8

**Reported in:** Deutsche Bank FTE interview experience. The report
explicitly says not to convert the number to a string.\
**Reference:** [GeeksforGeeks --- Deutsche Bank FTE interview
experience](https://www.geeksforgeeks.org/interview-experiences/deutsche-bank-interview-experience-for-fte-on-campus-virtual-2020/)

``` java
static long replaceZeroWithEight(long number) {
    if (number == 0) return 8;

    boolean negative = number < 0;
    long n = Math.abs(number);
    long result = 0;
    long place = 1;

    while (n > 0) {
        long digit = n % 10;
        if (digit == 0) digit = 8;
        result += digit * place;
        place *= 10;
        n /= 10;
    }

    return negative ? -result : result;
}
```

**Caution:** `Math.abs(Long.MIN_VALUE)` overflows. If the input
type/range can include that value, use a safer strategy or explicitly
discuss the constraint. The straightforward solution is fine for normal
non-extreme inputs.

## 2.12 Armstrong number

**Reported in:** Deutsche Bank FTE interview experience.\
**Reference:** [GeeksforGeeks --- Deutsche Bank FTE interview
experience](https://www.geeksforgeeks.org/interview-experiences/deutsche-bank-interview-experience-for-fte-on-campus-virtual-2020/)

``` java
static boolean isArmstrong(int n) {
    if (n < 0) return false;

    int digits = String.valueOf(n).length();
    int value = n;
    long sum = 0;

    while (value > 0) {
        int digit = value % 10;
        sum += intPow(digit, digits);
        value /= 10;
    }
    return sum == n;
}

static long intPow(int base, int exp) {
    long result = 1;
    for (int i = 0; i < exp; i++) result *= base;
    return result;
}
```

**Note:** The code uses string conversion only to count digits. If the
interviewer forbids string conversion entirely, count digits with a loop
first. Handle zero explicitly: zero is an Armstrong number under the
usual definition.

## 2.13 Palindrome and string reversal variants

**Reported in:** Deutsche Bank Technology Analyst / Graduate Analyst
reports.\
**Practice links:** [LeetCode 125 --- Valid
Palindrome](https://leetcode.com/problems/valid-palindrome/), [LeetCode
344 --- Reverse String](https://leetcode.com/problems/reverse-string/)

``` java
static boolean isPalindrome(String s) {
    int left = 0, right = s.length() - 1;
    while (left < right) {
        if (s.charAt(left) != s.charAt(right)) return false;
        left++;
        right--;
    }
    return true;
}

static String reverseString(String s) {
    char[] chars = s.toCharArray();
    int left = 0, right = chars.length - 1;
    while (left < right) {
        char temp = chars[left];
        chars[left++] = chars[right];
        chars[right--] = temp;
    }
    return new String(chars);
}
```

For "reverse string with vowels" or "reverse only vowels," clarify
whether only vowel positions change or the whole string reverses.
Related practice: [LeetCode 345 --- Reverse Vowels of a
String](https://leetcode.com/problems/reverse-vowels-of-a-string/).

## 2.14 Queue using two stacks

**Reported in:** Deutsche Bank Technology Analyst interview experience.\
**Practice link:** [GeeksforGeeks --- Implement Queue Using
Stacks](https://www.geeksforgeeks.org/queue-using-stacks/)

``` java
class QueueUsingStacks {
    private final Deque<Integer> in = new ArrayDeque<>();
    private final Deque<Integer> out = new ArrayDeque<>();

    void offer(int x) {
        in.push(x);
    }

    int poll() {
        moveIfNeeded();
        if (out.isEmpty()) throw new NoSuchElementException();
        return out.pop();
    }

    int peek() {
        moveIfNeeded();
        if (out.isEmpty()) throw new NoSuchElementException();
        return out.peek();
    }

    private void moveIfNeeded() {
        if (out.isEmpty()) {
            while (!in.isEmpty()) out.push(in.pop());
        }
    }
}
```

**Complexity:** amortised O(1) per enqueue/dequeue; a single dequeue can
take O(n) when elements transfer.

## 2.15 Find duplicate elements and their frequencies

**Reported in:** Deutsche Bank Technology Analyst interview experience.\
**Practice link:** [LeetCode 442 --- Find All Duplicates in an
Array](https://leetcode.com/problems/find-all-duplicates-in-an-array/)

``` java
static Map<Integer, Integer> frequencies(int[] nums) {
    Map<Integer, Integer> freq = new HashMap<>();
    for (int x : nums) {
        freq.put(x, freq.getOrDefault(x, 0) + 1);
    }
    return freq;
}

static Set<Integer> duplicates(int[] nums) {
    Map<Integer, Integer> freq = frequencies(nums);
    Set<Integer> result = new HashSet<>();

    for (Map.Entry<Integer, Integer> entry : freq.entrySet()) {
        if (entry.getValue() > 1) result.add(entry.getKey());
    }
    return result;
}
```

**Complexity:** O(n) average time, O(n) space.

## 2.16 LRU cache

**Reported in:** Deutsche Bank Graduate Analyst FTE interview
experience.\
**Practice link:** [LeetCode 146 --- LRU
Cache](https://leetcode.com/problems/lru-cache/)

Java's `LinkedHashMap` provides an interview-friendly implementation. If
asked to implement the data structure manually, use a HashMap plus a
doubly linked list.

``` java
class LRUCache extends LinkedHashMap<Integer, Integer> {
    private final int capacity;

    LRUCache(int capacity) {
        super(capacity, 0.75f, true); // access order
        this.capacity = capacity;
    }

    public int getValue(int key) {
        return getOrDefault(key, -1);
    }

    public void putValue(int key, int value) {
        put(key, value);
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<Integer, Integer> eldest) {
        return size() > capacity;
    }
}
```

**Complexity:** expected O(1) for get/put. Explain why the
`accessOrder=true` constructor argument matters. If the interviewer
prohibits built-in linked structures, implement the doubly linked list
explicitly.

## 2.17 Heap sort

**Reported in:** Deutsche Bank interview experience where heap sort and
its complexity/comparison with other sorting algorithms were discussed.\
**Practice link:** [GeeksforGeeks --- Heap
Sort](https://www.geeksforgeeks.org/heap-sort/)

``` java
static void heapSort(int[] a) {
    int n = a.length;

    for (int i = n / 2 - 1; i >= 0; i--) {
        siftDown(a, n, i);
    }

    for (int end = n - 1; end > 0; end--) {
        int temp = a[0];
        a[0] = a[end];
        a[end] = temp;
        siftDown(a, end, 0);
    }
}

static void siftDown(int[] a, int size, int root) {
    while (true) {
        int largest = root;
        int left = 2 * root + 1;
        int right = 2 * root + 2;

        if (left < size && a[left] > a[largest]) largest = left;
        if (right < size && a[right] > a[largest]) largest = right;
        if (largest == root) return;

        int temp = a[root];
        a[root] = a[largest];
        a[largest] = temp;
        root = largest;
    }
}
```

**Complexity:** O(n log n) time, O(1) auxiliary space; not stable.

## 2.18 Count digits and alphabetic characters

**Reported in:** Deutsche Bank FTE on-campus coding round.\
**Reference:** [GeeksforGeeks --- Deutsche Bank FTE on-campus
experience](https://www.geeksforgeeks.org/interview-experiences/deutsche-bank-interview-experience-on-campus-fte/)

``` java
static int[] countDigitsAndLetters(String s) {
    int digits = 0, letters = 0;
    for (int i = 0; i < s.length(); i++) {
        char ch = s.charAt(i);
        if (Character.isDigit(ch)) digits++;
        else if (Character.isLetter(ch)) letters++;
    }
    return new int[]{digits, letters};
}
```

**Complexity:** O(n) time, O(1) extra space.

## 2.19 Lexicographically smallest string of length n with character sum k

**Reported in:** Deutsche Bank FTE on-campus coding round. The report
uses `a=1, b=2, …, z=26`.\
**Reference:** [GeeksforGeeks --- Deutsche Bank FTE on-campus
experience](https://www.geeksforgeeks.org/interview-experiences/deutsche-bank-interview-experience-on-campus-fte/)

Goal: create the lexicographically smallest string of length `n` whose
values sum to `k`. Start with all `a` (sum n), then distribute the extra
value from the rightmost position backwards so earlier characters stay
as small as possible.

``` java
static String smallestString(int n, int k) {
    if (n <= 0 || k < n || k > 26 * n) {
        throw new IllegalArgumentException("Impossible n and k");
    }

    char[] result = new char[n];
    Arrays.fill(result, 'a');
    int extra = k - n;

    for (int i = n - 1; i >= 0 && extra > 0; i--) {
        int add = Math.min(25, extra);
        result[i] += add;
        extra -= add;
    }
    return new String(result);
}
// n=5, k=42 -> "aaamz"
```

**Complexity:** O(n) time, O(n) space.

## 2.20 Other reported or adjacent coding prompts

These are described in candidate reports; some reports do not include
enough constraints to provide a single correct implementation. Use the
linked source to read the full statement before coding.

  ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  Reported question       Practice link                                                                                                                                          Key technique
  ----------------------- ------------------------------------------------------------------------------------------------------------------------------------------------------ -----------------------
  Count elements greater  [Deutsche Bank FTE experience](https://www.geeksforgeeks.org/interview-experiences/deutsche-bank-interview-experience-on-campus-fte/)                  Array traversal;
  than the mean of                                                                                                                                                               clarify endpoint
  previous and next                                                                                                                                                              handling and
  elements                                                                                                                                                                       integer/decimal mean

  Difference between      [Deutsche Bank FTE experience](https://www.geeksforgeeks.org/interview-experiences/deutsche-bank-interview-experience-on-campus-fte/)                  Prefix/suffix counts;
  smaller elements to the                                                                                                                                                        careful inequalities
  left and greater                                                                                                                                                               
  elements to the right;                                                                                                                                                         
  maximise absolute                                                                                                                                                              
  difference                                                                                                                                                                     

  Minimum travel cost     [Deutsche Bank Summer Internship                                                                                                                       Prefix sums; compare
  between stations on a   experience](https://www.geeksforgeeks.org/interview-experiences/deutsche-bank-interview-experience-for-summer-internship-2024/)                        clockwise and
  circular route                                                                                                                                                                 counter-clockwise
                                                                                                                                                                                 routes

  Tree diameter from node [Deutsche Bank FTE 2020                                                                                                                                Reconstruct
  counts at each level    experience](https://www.geeksforgeeks.org/interview-experiences/deutsche-bank-interview-experience-for-fte-on-campus-virtual-2020/)                    constraints; tree
                                                                                                                                                                                 reasoning / greedy
                                                                                                                                                                                 proof

  String conversion using [Deutsche Bank FTE 2020 experience](https://www.geeksforgeeks.org/interview-experiences/deutsche-bank-interview-experience-for-fte2020/)               Greedy / character
  a sequence of character                                                                                                                                                        arithmetic; exact
  shifts                                                                                                                                                                         operation rules matter

  Range updates and       [Deutsche Bank campus experience](https://www.geeksforgeeks.org/interview-experiences/deutsche-bank-interview-experience-set-6-campus/)                Difference array or
  queries on an array                                                                                                                                                            lazy propagation
                                                                                                                                                                                 depending on operations

  Calculator designed     [Deutsche Bank full-time on-campus                                                                                                                     Interfaces/strategy or
  with OOP; follow-up to  experience](https://www.geeksforgeeks.org/interview-experiences/deutsche-bank-interview-experiance-for-sde-full-time-on-campus/)                       polymorphism;
  add percentage and                                                                                                                                                             division-by-zero
  modulo                                                                                                                                                                         handling

  Bubble sort and sorting [Graduate Analyst/Technology Analyst 2021                                                                                                              Sorting algorithms and
  comparison              experience](https://www.geeksforgeeks.org/interview-experiences/deutsche-bank-interview-experience-for-graduate-analyst-or-technology-analyst-2021/)   complexity
  ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

------------------------------------------------------------------------

# 3. Core Java and OOP --- questions and answers

## What are the four pillars of OOP?

-   **Encapsulation:** keep state and behaviour together and control
    access to state.
-   **Abstraction:** expose essential behaviour while hiding
    implementation details.
-   **Inheritance:** derive a class from another class to reuse or
    specialise behaviour.
-   **Polymorphism:** use a common interface/type with different
    implementations.

## Abstraction vs encapsulation?

Abstraction is about what an object exposes; encapsulation is about
controlling access to its state and implementation. An interface is a
common abstraction mechanism; private fields with controlled methods are
an encapsulation mechanism.

## Overloading vs overriding?

-   **Overloading:** same method name with different parameter lists;
    selected at compile time.
-   **Overriding:** subclass provides an implementation of an inherited
    instance method; dynamic dispatch selects the implementation at
    runtime.

## Why is `public static void main(String[] args)` written this way?

-   `public`: JVM must be able to access the entry point.
-   `static`: the JVM can invoke it without creating an instance.
-   `void`: it returns no value.
-   `main`: recognised entry-point method name.
-   `String[] args`: command-line arguments.

## Can `String[] args` be replaced with `Integer[] args`?

No, not as the standard Java launcher entry-point signature. The JVM
looks for the supported `main` signature; an `Integer[]` parameter is
not a substitute.

## What is immutability? How do you make a class immutable?

Make the class final where appropriate; make fields private and final;
initialise them in the constructor; provide no setters; defensively copy
mutable inputs and outputs; do not expose internal mutable state.
Immutable objects are easier to share safely across threads.

## How does `HashMap` work internally?

It hashes a key to choose a bucket. Collisions are handled within the
bucket; Java implementations use nodes and may treeify heavily-collided
buckets under conditions. `equals()` distinguishes keys that land in the
same bucket. Resizing occurs when size crosses a capacity/load-factor
threshold. Exact implementation details can vary by Java version.

## What is the `equals()` / `hashCode()` contract?

If two objects are equal according to `equals`, they must return the
same hash code. Unequal objects can have the same hash code. If an
object is used as a key, its equality/hash fields should not change
while it is in the map.

## `HashMap` vs `ConcurrentHashMap`?

`HashMap` is not safe for unsynchronised concurrent mutation.
`ConcurrentHashMap` supports concurrent operations, but multiple
separate calls do not automatically form one atomic transaction. Use
atomic methods such as `computeIfAbsent` where appropriate.

## `ArrayList` vs `LinkedList`?

`ArrayList` offers O(1) indexed access and good memory locality;
insertions/removals in the middle can shift elements. `LinkedList` has
O(n) indexed access; insertion/removal is efficient only when the
relevant position/node is already available. `ArrayList` is usually the
default choice.

## What is a singleton? What are its risks?

A singleton restricts construction to one instance per intended scope.
Risks include hidden global state, testing difficulty, concurrency
issues if implemented poorly, and confusion between one instance per JVM
and one instance per application/container.

## What is a functional interface?

An interface with exactly one abstract method, suitable as a lambda
target. Default and static methods do not count toward the single
abstract method requirement. `@FunctionalInterface` lets the compiler
validate the intent.

## `map()` vs `flatMap()` in streams?

`map` transforms each input into one output. `flatMap` transforms each
input into a stream/optional-like container and flattens the nested
results into one pipeline.

## What is `Optional`?

A container that represents a present or absent value. It can make
absence explicit in return values. Avoid using it indiscriminately for
fields, parameters, or to replace all null checks.

## Checked vs unchecked exceptions?

Checked exceptions must be caught or declared. Unchecked exceptions are
subclasses of `RuntimeException` (or `Error`) and do not require
declaration. Choose handling based on whether the caller can reasonably
recover.

------------------------------------------------------------------------

# 4. Java 8, collections, and concurrency

## What are Java Streams?

A declarative pipeline over data, typically using intermediate
operations (`filter`, `map`, `sorted`) followed by a terminal operation
(`collect`, `count`, `reduce`, `forEach`). Intermediate operations are
lazy until a terminal operation runs. Avoid shared mutable state and
unintended side effects.

## What is a race condition?

The outcome depends on the timing/interleaving of concurrent operations
on shared state. Avoid it with immutability, thread confinement,
synchronisation, locks, atomic variables, or concurrent data structures.

## What is a deadlock?

Two or more threads wait for resources held by one another, preventing
progress. Reduce risk with consistent lock ordering, small critical
sections, avoiding unnecessary nested locks, and carefully designed
timeouts.

## Process vs thread?

A process has its own address space and resources. Threads within a
process share memory/resources but have their own execution stack and
thread state. Threads are lighter to create but shared memory requires
coordination.

## How can deadlock be prevented?

A common strategy is to impose a global lock order and always acquire
locks in that order. Other strategies include reducing nested locking,
using timed lock attempts, or redesigning to avoid holding multiple
locks simultaneously.

## What is a Red-Black Tree?

A self-balancing binary search tree whose colouring/structural rules
keep height logarithmic, enabling O(log n) search, insertion, and
deletion. Java's sorted maps/sets use balanced-tree structures.

## How do you implement a queue using two stacks?

Use an input stack for enqueue and an output stack for dequeue. When the
output stack is empty, transfer all input elements to it. Each element
moves at most once between stacks, giving amortised O(1) operations.

------------------------------------------------------------------------

# 5. Spring Boot, REST, microservices, and Kafka

## What are IoC and dependency injection in Spring?

The IoC container creates and wires beans. Dependency injection supplies
a class with its collaborators rather than requiring it to construct
them directly. Constructor injection makes required dependencies
explicit and improves testability.

## Controller vs service vs repository?

-   **Controller:** HTTP concerns, request/response mapping.
-   **Service:** business rules and orchestration.
-   **Repository:** persistence/data-access abstraction.

## What is Spring Boot auto-configuration?

It configures beans based on classpath dependencies, existing beans, and
configuration properties to reduce setup. You can inspect or override
auto-configuration when needed.

## What does `@Transactional` do?

It defines a transaction boundary through Spring's transaction
infrastructure. Understand propagation, isolation, rollback rules, and
proxy/self-invocation caveats. By default, unchecked exceptions
generally trigger rollback; checked-exception behaviour may need
configuration.

## JPA vs Hibernate vs Spring Data JPA?

JPA is the persistence specification; Hibernate is a common
implementation; Spring Data JPA adds repository abstractions that reduce
repetitive data-access code.

## REST vs SOAP?

REST is an architectural style commonly used over HTTP with JSON; SOAP
is a protocol with a formal XML message envelope and established
standards. Selection depends on interoperability, contracts, and system
requirements.

## Explain HTTP methods and idempotency.

-   `GET`: read.
-   `POST`: create or trigger an operation.
-   `PUT`: replace a resource.
-   `PATCH`: partially update.
-   `DELETE`: delete. Idempotency means repeated execution has the same
    intended effect as one execution. GET, PUT, and DELETE are intended
    to be idempotent; POST usually is not unless an idempotency
    mechanism is implemented.

## What are timeout, retry, and circuit breaker?

A timeout limits waiting. A retry repeats a possibly transient failure,
usually with bounded attempts and backoff. A circuit breaker temporarily
stops calls to an unhealthy dependency. Retries can amplify outages and
can duplicate side effects unless operations are safe/idempotent.

## What is Kafka consumer offset?

An offset identifies a consumer's position within a partition. Commit
timing determines where consumption resumes. Processing before
committing can cause reprocessing; committing before processing
completes can risk losing work, depending on the design.

## How do you handle duplicate events?

Use stable event/business identifiers, idempotent handlers, unique
database constraints, deduplication records, and carefully chosen
offset/transaction semantics. Assume repeated delivery unless the
system's guarantees say otherwise.

## Offset vs cursor pagination?

Offset pagination is simple and supports page numbers but can be
expensive and unstable for large/changing datasets. Cursor/keyset
pagination uses a stable ordering key and is often more efficient for
large datasets.

## How do you investigate a slow API?

Measure latency by stage; inspect traces/logs with request IDs; examine
query plans, N+1 queries, indexes, connection pools, downstream calls,
CPU/memory, and recent deployments. Make evidence-based changes and
measure again.

------------------------------------------------------------------------

# 6. SQL and DBMS questions

## DDL vs DML?

-   **DDL:** defines/changes schema, e.g. `CREATE`, `ALTER`, `DROP`,
    `TRUNCATE` (transaction semantics vary by database).
-   **DML:** changes data, e.g. `INSERT`, `UPDATE`, `DELETE`. `SELECT`
    is often categorised as DQL, though classifications vary.

## Find the second-highest distinct salary

``` sql
SELECT MAX(salary) AS second_highest
FROM employees
WHERE salary < (SELECT MAX(salary) FROM employees);
```

Returns `NULL` if there is no second distinct salary. Know
`DENSE_RANK()` as an alternative.

## What is a database index?

An additional data structure that speeds up selected lookups, joins,
filters, and ordering. It uses storage and adds write overhead. Index
columns based on real query patterns and inspect query plans.

``` sql
CREATE INDEX idx_employee_department
ON employees(department_id);
```

## `INNER JOIN` vs `LEFT JOIN`?

`INNER JOIN` returns matching rows only. `LEFT JOIN` keeps all
left-table rows and fills unmatched right-table columns with `NULL`.

## `WHERE` vs `HAVING`?

``` sql
SELECT department_id, COUNT(*) AS total
FROM employees
WHERE salary IS NOT NULL
GROUP BY department_id
HAVING COUNT(*) > 5;
```

`WHERE` filters rows before grouping; `HAVING` filters grouped results
after aggregation.

## What is ACID?

-   **Atomicity:** all-or-nothing.
-   **Consistency:** constraints/invariants remain valid.
-   **Isolation:** concurrent transactions follow the configured
    isolation guarantees.
-   **Durability:** committed changes persist according to database
    guarantees.

## What is normalisation?

Structuring relational data to reduce redundancy and update anomalies.
Denormalisation deliberately duplicates or precomputes data for selected
read performance, adding consistency complexity.

## What is a deadlock in a database?

Transactions wait on locks held by each other. Reduce risk with
consistent lock ordering, short transactions, appropriate indexes, and
safe retry handling.

------------------------------------------------------------------------

# 7. Operating systems, networking, and CS fundamentals

Candidate reports mention OS scheduling, process/thread concepts,
virtual vs physical memory, compiler vs interpreter, and basic
networking/HTTP questions.

## What are process scheduling algorithms?

Examples include FCFS, Shortest Job First, Priority Scheduling, and
Round Robin. Be able to explain one algorithm, its trade-offs, and
whether it can cause starvation.

## FCFS vs SJF vs Round Robin?

-   **FCFS:** simple first-come-first-served; can cause a convoy effect.
-   **SJF:** chooses the shortest estimated job; good average waiting
    time when job lengths are known/estimated, but long jobs can starve.
-   **Round Robin:** each process receives a time slice; responsive for
    time-sharing, but too-small slices add context-switch overhead.

## Virtual memory vs physical memory?

Physical memory is RAM. Virtual memory gives each process an address
space that the operating system and hardware map to physical memory,
with paging and protection mechanisms. It is not simply "extra RAM";
paging to disk is much slower than RAM.

## Compiler vs interpreter?

A compiler translates source or intermediate code before execution; an
interpreter executes instructions through a runtime. Many modern systems
use a combination, such as bytecode plus JIT compilation.

## HTTP vs HTTPS?

HTTPS is HTTP protected by TLS, providing encryption in transit,
integrity, and server authentication when certificates are validated
correctly.

## How do frontend and backend communicate?

Typically through HTTP requests to REST/GraphQL endpoints, with
structured payloads such as JSON. The backend validates input,
authorises the caller, performs business/data operations, and returns
status codes and a response body.

------------------------------------------------------------------------

# 8. Projects, behavioural, and techno-managerial questions

Candidate reports repeatedly emphasise resume and project deep dives.
Prepare your actual projects rather than memorising generic answers.

## Common reported or likely questions

1.  Tell me about yourself.
2.  Which technical area are you strongest in?
3.  Explain a project from your resume end to end.
4.  Why did you choose that framework/library?
5.  How did your frontend or another service call your API?
6.  What was a difficult bug? How did you identify the root cause?
7.  What did you personally implement, versus what the team implemented?
8.  What tests did you write? What was your testing strategy?
9.  What would you change if you redesigned the project today?
10. How did you measure a performance improvement?
11. Tell me about working in a team or handling disagreement.
12. Why Deutsche Bank and why financial-services technology?
13. How do you handle an unclear requirement?
14. What would you do if two teammates advocated different designs?
15. How do you learn a new technology?

## Project answer structure

Use:

**Problem → architecture → your contribution → design choice/trade-off →
testing → outcome → improvement.**

For every metric on your resume, know: - The baseline and measurement
method. - The exact change you made. - How you ruled out other causes. -
Any trade-off or limitation. - How you would verify the result again.

## Scenario: two teammates disagree on a design

Clarify requirements and constraints, compare options against measurable
criteria (correctness, latency, maintainability, risk, delivery time),
prototype or benchmark if needed, record the decision, and commit to the
agreed direction.

## Scenario: production API is failing

Check scope and impact, recent releases, metrics, traces, logs,
dependency health, and database state. Mitigate safely (rollback/feature
flag where appropriate), communicate status, verify recovery, and follow
up with root-cause analysis and prevention.

------------------------------------------------------------------------

# 9. Mock interview checklist

## DSA

-   [ ] Remove duplicates from a string --- [GFG
    link](https://www.geeksforgeeks.org/dsa/remove-duplicates-from-a-given-string/)
-   [ ] Merge sort --- [GFG
    link](https://www.geeksforgeeks.org/merge-sort/)
-   [ ] Maximum subarray sum --- [GFG
    link](https://www.geeksforgeeks.org/dsa/largest-sum-contiguous-subarray/)
-   [ ] Connect N ropes --- [GFG
    link](https://www.geeksforgeeks.org/dsa/connect-n-ropes-minimum-cost/)
-   [ ] Count grid paths --- [GFG
    link](https://www.geeksforgeeks.org/dsa/count-possible-paths-top-left-bottom-right-nxm-matrix/)
-   [ ] Longest common substring --- [GFG
    link](https://www.geeksforgeeks.org/dsa/longest-common-substring-dp-29/)
-   [ ] Rotate linked list --- [GFG
    link](https://www.geeksforgeeks.org/rotate-a-linked-list/)
-   [ ] Linked-list cycle start --- [LeetCode
    142](https://leetcode.com/problems/linked-list-cycle-ii/)
-   [ ] Two Sum --- [LeetCode 1](https://leetcode.com/problems/two-sum/)
-   [ ] Queue using two stacks --- [GFG
    link](https://www.geeksforgeeks.org/queue-using-stacks/)
-   [ ] LRU Cache --- [LeetCode
    146](https://leetcode.com/problems/lru-cache/)
-   [ ] Heap sort --- [GFG
    link](https://www.geeksforgeeks.org/heap-sort/)
-   [ ] Reverse string / palindrome --- [LeetCode
    344](https://leetcode.com/problems/reverse-string/), [LeetCode
    125](https://leetcode.com/problems/valid-palindrome/)
-   [ ] Armstrong number, replace zero with eight, count digits/letters,
    lexicographically smallest string --- see sections above.

## Technical

-   [ ] OOP, immutability, `equals`/`hashCode`, HashMap internals.
-   [ ] Java 8 lambdas, streams, functional interfaces, Optional.
-   [ ] Collections, multithreading, race conditions, deadlock
    prevention.
-   [ ] Spring DI, controller/service/repository, transactions,
    JPA/Hibernate.
-   [ ] REST methods/status codes, pagination, retries, circuit
    breakers, Kafka offsets.
-   [ ] SQL joins, DDL/DML, second-highest salary, indexes, ACID.
-   [ ] OS scheduling, process vs thread, virtual memory, HTTPS.
-   [ ] Two detailed project walkthroughs and one production-debugging
    story.

------------------------------------------------------------------------

# 10. Public candidate-report sources

These are public interview-experience pages and guides. Some reports may
be old, anecdotal, edited, incomplete, or gated. Treat them as practice
evidence, not official hiring guidance.

1.  [GeeksforGeeks --- Deutsche Bank Interview Experience for SDE
    (On-Campus)](https://www.geeksforgeeks.org/interview-experiences/deutsche-bank-interview-experience-for-sde-on-campus/)
    --- merge sort, remove duplicates, OS scheduling, DDL/DML, project
    discussion.
2.  [GeeksforGeeks --- Deutsche Bank SDE Full-Time
    On-Campus](https://www.geeksforgeeks.org/interview-experiences/deutsche-bank-interview-experiance-for-sde-full-time-on-campus/)
    --- calculator using OOP and project/technical discussions.
3.  [GeeksforGeeks --- Deutsche Bank
    On-Campus](https://www.geeksforgeeks.org/interview-experiences/deutsche-bank-interview-experience-on-campus-3/)
    --- connect ropes, grid paths, longest common substring, maximum
    subarray.
4.  [GeeksforGeeks --- Deutsche Bank Technology Analyst,
    Pune](https://www.geeksforgeeks.org/interview-experiences/deutsche-bank-interview-experience-technology-analyst-pune/)
    --- queue using two stacks, duplicate frequencies, HTTP/HTTPS, data
    structures.
5.  [GeeksforGeeks --- Deutsche Bank FTE
    On-Campus](https://www.geeksforgeeks.org/interview-experiences/deutsche-bank-interview-experience-on-campus-fte/)
    --- digit/letter count, array problem, lexicographically smallest
    string.
6.  [GeeksforGeeks --- Deutsche Bank FTE Virtual
    2020](https://www.geeksforgeeks.org/interview-experiences/deutsche-bank-interview-experience-for-fte-on-campus-virtual-2020/)
    --- replace zero digits, Armstrong number, OS/CS fundamentals.
7.  [GeeksforGeeks --- Deutsche Bank Graduate Analyst / Technology
    Analyst
    2021](https://www.geeksforgeeks.org/interview-experiences/deutsche-bank-interview-experience-for-graduate-analyst-or-technology-analyst-2021/)
    --- SQL joins, bubble sort, palindrome, string reversal.
8.  [Glassdoor --- Java Developer interview report: coding, SQL, Java 8,
    linked-list rotation, indexes,
    Agile](https://www.glassdoor.com/Interview/Coding-Test-1-coding-problem-one-SQL-query-MCQ-s-1st-Technical-round-all-java-basics-questions-java-8-features-wh-QTN_4685908.htm)
9.  [Glassdoor --- Java Developer: collections, OOP, concurrency, Java
    8, Spring, REST/SOAP,
    SQL](https://www.glassdoor.co.in/Interview/The-questions-were-from-the-Java-Collection-API-OOPS-Multithreading-Race-condition-and-Deadlock-Java-8-Functional-Inte-QTN_7391475.htm)
10. [TryExponent --- Deutsche Bank Software Engineer interview
    report](https://www.tryexponent.com/experiences/deutsche-bank-software-engineer-interview-ea98bb)
    --- swap two strings, Two Sum, HashMap internals, behavioural
    questions.
11. [InterviewQuery --- Deutsche Bank Software Engineer interview
    guide](https://www.interviewquery.com/guides/deutsche-bank-software-engineer)
    --- broader candidate-reported themes and process variation.
12. [Glassdoor --- Deutsche Bank interview questions and recent
    reports](https://www.glassdoor.com/Interview/Deutsche-Bank-Interview-Questions-E3150.htm)
    --- recent reports may be gated and can change.

------------------------------------------------------------------------

## Final advice

The most repeatable themes across the reports are **DSA fundamentals,
Core Java, collections, project depth, SQL/DBMS, and practical
problem-solving**. Some roles add Spring Boot, Kafka, concurrency, OS,
or scenario-based questions.

For a Python/Java backend interview, keep Python framework preparation
in your plan as well. The Deutsche Bank question bank is an additional
source of practice, not a substitute for the specific job description
you are interviewing against.
