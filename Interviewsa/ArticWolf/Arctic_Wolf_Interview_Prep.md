# 🐺 Arctic Wolf — Interview Preparation Guide
**Role:** Developer, Data Platform (R26_988)  
**Round 1:** Hands-on Coding + DSA + Tech Stack + Project Discussion  
**Language:** Java  

---

## Table of Contents
1. [Behavioral / HR Questions](#1-behavioral--hr-questions)
2. [DSA Topics & Must-Solve Problems](#2-dsa-topics--must-solve-problems)
3. [Java Core & OOP Questions](#3-java-core--oop-questions)
4. [Spring Boot & Microservices Questions](#4-spring-boot--microservices-questions)
5. [Kafka & Messaging Questions](#5-kafka--messaging-questions)
6. [Kubernetes & Docker Questions](#6-kubernetes--docker-questions)
7. [Database (PostgreSQL, Redis, Elasticsearch)](#7-database-postgresql-redis-elasticsearch)
8. [Distributed Systems & System Design Concepts](#8-distributed-systems--system-design-concepts)
9. [Resume Deep-Dive Questions](#9-resume-deep-dive-questions)
10. [JD-Specific Tech Stack Questions](#10-jd-specific-tech-stack-questions)
11. [Quick Revision Checklist](#11-quick-revision-checklist)

---

## 1. Behavioral / HR Questions

### ❓ "Tell me about yourself."
> **Answer:**
> I'm a backend software engineer with about 3 years of experience, currently working at Mercedes-Benz R&D India. I joined as a backend engineer and my first project was an AI platform — where I built the backend microservices from scratch, handling everything from API design to data flow, integrating LLMs, RAG architectures, and eventually designing agentic workflows where multiple AI agents collaborate across different parts of an engineering workflow. So right from the start I was building the infrastructure that makes AI systems actually work in production, not just plugging into one.
>
> Over time I naturally gravitated toward the deeper backend and infrastructure side — owning microservices that handle high volumes of data, designing event-driven architectures, working with messaging pipelines, Kubernetes, and cloud-native infrastructure on Azure. Java is my primary language, with Python on the backend side as well. So if I had to describe my journey in two words, it's been AI and backend engineering — those two themes have run through everything I've worked on, and at this point they've started to converge in interesting ways.
>
> I'm someone who picks up new technologies fairly quickly, and I've had to do that a lot — each project at work pushed me into a different area I hadn't worked in before. I actually enjoy that. What I don't enjoy is just using something without understanding it properly, so I tend to go fairly deep on the things I work with.
>
> What draws me most is the data engineering and backend infrastructure side — event-driven systems, high-throughput pipelines, making data fast to search and reliable to query. I've worked with Kafka, built search systems, dealt with performance problems at scale, and set up observability so you can actually understand what's happening in production. That's the kind of work I find genuinely interesting and want to keep going deeper on — especially into areas like large-scale search and stream processing which I haven't fully explored yet.
>
> Arctic Wolf is a natural next step for that — the Data Platform team is doing exactly this kind of work, on data that actually matters, at a scale where the engineering decisions have real consequences.

---

### ❓ "Why do you want to join Arctic Wolf?"
> **Answer:**
> Arctic Wolf is one of the few companies working at the intersection of big data, cybersecurity, and distributed systems at serious scale — over 10,000 customers generating massive streams of telemetry data every second.
> The Data Platform role specifically aligns with what I've been building: event-driven systems, Kafka-based pipelines, high-throughput APIs, and now the chance to work with technologies like Elasticsearch, Databricks, and Spark at a scale I haven't touched professionally yet.
> I also deeply respect how Arctic Wolf has positioned itself — it's not just a security product company, it's building an autonomous SOC. That mission matters. I want to be part of a team where the infrastructure I build has a direct impact on protecting organizations.
> Arctic Wolf's consistent recognition as a Top Workplace (2021–2025) across multiple regions also tells me it's a place where engineers are trusted and grown — that's important to me.

---

### ❓ "Why are you looking to switch from Mercedes-Benz?"
> **Answer:**
> My time at Mercedes-Benz R&D has been incredible — I got to own systems at scale, ship AI features to production, and architect end-to-end platforms. But I've been working in the automotive/EV domain and now want to take my distributed systems skills into the data platform and cybersecurity space.
> Arctic Wolf's stack — Kafka, Elasticsearch, Databricks, Kubernetes on AWS — is the natural next step in my journey. I want to work on data at a scale where milliseconds of search latency and event ordering really matter for security outcomes.

---

### ❓ "Tell me about a challenging technical problem you solved."
> **Answer:**
> One of the most impactful was the global search latency issue on the EV charging platform.
> The original implementation fetched charging records and customer data via multiple unbounded JPA queries — latency was averaging 1.2 seconds under load.
> I redesigned the search using JPA Specifications to compose dynamic predicates cleanly, backed by Postgres materialized views that pre-aggregated the most expensive join paths. Added partial indexes on high-cardinality filter fields.
> The result: latency dropped from ~1.2s to ~120ms — roughly a 90% reduction. The key insight was separating "write-time computation" from "query-time computation" using materialized views.

---

### ❓ "Tell me about a time you made an architectural decision you're proud of."
> **Answer:**
> At Mercedes-Benz, I designed the Kubernetes-native job execution framework for Python batch workloads.
> The existing approach required manual intervention — DevOps had to SSH and run scripts for each batch job. I built a framework using Kubernetes CronJobs, adhoc job triggers via REST, and parameterized Helm templates, with secure secrets injection.
> It eliminated ~70% of manual operational overhead. The architectural decision I'm proud of was keeping the framework configuration-driven rather than code-driven — teams onboard new jobs by writing YAML, not code.

---

### ❓ "How do you handle disagreements with teammates on design decisions?"
> **Answer:**
> I try to anchor debates in measurable outcomes rather than opinions. When I proposed moving to Temporal for workflow orchestration, there was pushback about adding a new dependency.
> I made a lightweight proof of concept showing failure recovery behavior vs our existing retry logic, documented the trade-offs, and ran it by senior engineers. Data won the argument, not persuasion.
> I also genuinely listen — sometimes the pushback reveals constraints I hadn't considered.

---

### ❓ "Where do you see yourself in 3–5 years?"
> **Answer:**
> In 3 to 5 years I see myself moving toward a Solution Architect kind of role — someone who sits at the intersection of backend engineering and AI, and can make the right technical decisions across both. Not just writing code but owning the design of systems end-to-end — understanding the data layer, the infrastructure, and where AI fits into it in a way that actually adds value rather than just bolting it on.
> I've already been building in both spaces, and I want to get to a point where I can look at a complex engineering problem and architect a solution that's solid on the backend side and intelligent on the AI side. That's the direction I'm heading, and a role like this — where the backend and data challenges are real and serious — is exactly the kind of foundation I need to get there.

---

### ❓ "What do you know about Arctic Wolf's product?"
> **Answer:**
> Arctic Wolf provides a managed Security Operations Center (SOC) powered by its Aurora Superintelligence Platform. It ingests endpoint, network, and cloud telemetry from customers, analyzes it for threats, and surfaces actionable alerts — essentially removing the need for every company to build their own SOC team.
> The Data Platform team specifically handles the storage and rapid search of that telemetry data — the layer between raw ingestion and the detection/response layer. That's what I'd be building on.

---

## 2. DSA Topics & Must-Solve Problems

> **Strategy:** Focus on Medium-level problems. Arctic Wolf's coding round tests problem-solving and clean code, not competitive programming tricks.

### 📌 Pattern 1: Arrays & Two Pointers
**Core Concepts:** Sliding window (fixed/variable), prefix sums, Kadane's, difference arrays

| Problem | Difficulty | Link |
|---|---|---|
| Two Sum | Easy | https://leetcode.com/problems/two-sum/ |
| Best Time to Buy and Sell Stock | Easy | https://leetcode.com/problems/best-time-to-buy-and-sell-stock/ |
| Maximum Subarray (Kadane's) | Medium | https://leetcode.com/problems/maximum-subarray/ |
| Longest Subarray with Sum ≤ K | Medium | https://leetcode.com/problems/longest-subarray-of-1s-after-deleting-one-element/ |
| Trapping Rain Water | Hard | https://leetcode.com/problems/trapping-rain-water/ |
| Minimum Window Substring | Hard | https://leetcode.com/problems/minimum-window-substring/ |
| Rotate Array | Medium | https://leetcode.com/problems/rotate-array/ |
| Subarray Sum Equals K | Medium | https://leetcode.com/problems/subarray-sum-equals-k/ |
| Container With Most Water | Medium | https://leetcode.com/problems/container-with-most-water/ |
| 3Sum | Medium | https://leetcode.com/problems/3sum/ |

---

### 📌 Pattern 2: Hashing & Frequency Maps
**Core Concepts:** HashMap, HashSet, frequency count, prefix hash

| Problem | Difficulty | Link |
|---|---|---|
| Group Anagrams | Medium | https://leetcode.com/problems/group-anagrams/ |
| Top K Frequent Elements | Medium | https://leetcode.com/problems/top-k-frequent-elements/ |
| Longest Consecutive Sequence | Medium | https://leetcode.com/problems/longest-consecutive/ |
| Valid Anagram | Easy | https://leetcode.com/problems/valid-anagram/ |
| Two Sum (HashMap) | Easy | https://leetcode.com/problems/two-sum/ |
| Find All Anagrams in a String | Medium | https://leetcode.com/problems/find-all-anagrams-in-a-string/ |

---

### 📌 Pattern 3: Strings
**Core Concepts:** Sliding window on strings, palindrome expansion

| Problem | Difficulty | Link |
|---|---|---|
| Longest Substring Without Repeating Characters | Medium | https://leetcode.com/problems/longest-substring-without-repeating-characters/ |
| Longest Palindromic Substring | Medium | https://leetcode.com/problems/longest-palindromic-substring/ |
| Valid Parentheses | Easy | https://leetcode.com/problems/valid-parentheses/ |
| Decode String | Medium | https://leetcode.com/problems/decode-string/ |
| String Compression | Medium | https://leetcode.com/problems/string-compression/ |
| Minimum Window Substring | Hard | https://leetcode.com/problems/minimum-window-substring/ |

---

### 📌 Pattern 4: Stacks & Monotonic Stack
**Core Concepts:** Next greater element, histogram area, balanced brackets

| Problem | Difficulty | Link |
|---|---|---|
| Valid Parentheses | Easy | https://leetcode.com/problems/valid-parentheses/ |
| Daily Temperatures | Medium | https://leetcode.com/problems/daily-temperatures/ |
| Largest Rectangle in Histogram | Hard | https://leetcode.com/problems/largest-rectangle-in-histogram/ |
| Next Greater Element I | Easy | https://leetcode.com/problems/next-greater-element-i/ |
| Decode String | Medium | https://leetcode.com/problems/decode-string/ |
| Min Stack | Medium | https://leetcode.com/problems/min-stack/ |

---

### 📌 Pattern 5: Linked Lists
**Core Concepts:** Fast/slow pointer, reversal, cycle detection

| Problem | Difficulty | Link |
|---|---|---|
| Reverse Linked List | Easy | https://leetcode.com/problems/reverse-linked-list/ |
| Detect Cycle in Linked List | Easy | https://leetcode.com/problems/linked-list-cycle/ |
| Merge Two Sorted Lists | Easy | https://leetcode.com/problems/merge-two-sorted-lists/ |
| Merge K Sorted Lists | Hard | https://leetcode.com/problems/merge-k-sorted-lists/ |
| LRU Cache | Medium | https://leetcode.com/problems/lru-cache/ |
| Reorder List | Medium | https://leetcode.com/problems/reorder-list/ |
| Find Middle of Linked List | Easy | https://leetcode.com/problems/middle-of-the-linked-list/ |

> **💡 LRU Cache is extremely high-value** — it tests HashMap + DoublyLinkedList together and is directly relevant to your Redis experience.

---

### 📌 Pattern 6: Trees & BST
**Core Concepts:** DFS/BFS traversals, LCA, path sums, diameter

| Problem | Difficulty | Link |
|---|---|---|
| Binary Tree Level Order Traversal (BFS) | Medium | https://leetcode.com/problems/binary-tree-level-order-traversal/ |
| Maximum Depth of Binary Tree | Easy | https://leetcode.com/problems/maximum-depth-of-binary-tree/ |
| Lowest Common Ancestor of BST | Medium | https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-search-tree/ |
| Validate Binary Search Tree | Medium | https://leetcode.com/problems/validate-binary-search-tree/ |
| Kth Smallest Element in BST | Medium | https://leetcode.com/problems/kth-smallest-element-in-a-bst/ |
| Serialize and Deserialize Binary Tree | Hard | https://leetcode.com/problems/serialize-and-deserialize-binary-tree/ |
| Binary Tree Right Side View | Medium | https://leetcode.com/problems/binary-tree-right-side-view/ |
| Path Sum II | Medium | https://leetcode.com/problems/path-sum-ii/ |
| Diameter of Binary Tree | Easy | https://leetcode.com/problems/diameter-of-binary-tree/ |

---

### 📌 Pattern 7: Graphs (BFS/DFS)
**Core Concepts:** Grid traversal, connected components, topo sort, Dijkstra

| Problem | Difficulty | Link |
|---|---|---|
| Number of Islands | Medium | https://leetcode.com/problems/number-of-islands/ |
| Clone Graph | Medium | https://leetcode.com/problems/clone-graph/ |
| Course Schedule (Topo Sort) | Medium | https://leetcode.com/problems/course-schedule/ |
| Shortest Path in Binary Matrix | Medium | https://leetcode.com/problems/shortest-path-in-binary-matrix/ |
| Word Ladder | Hard | https://leetcode.com/problems/word-ladder/ |
| Pacific Atlantic Water Flow | Medium | https://leetcode.com/problems/pacific-atlantic-water-flow/ |
| Rotting Oranges (Multi-source BFS) | Medium | https://leetcode.com/problems/rotting-oranges/ |
| Network Delay Time (Dijkstra) | Medium | https://leetcode.com/problems/network-delay-time/ |

---

### 📌 Pattern 8: Heaps & Priority Queues
**Core Concepts:** Min-heap, max-heap, K-th largest/smallest, merge K sorted

| Problem | Difficulty | Link |
|---|---|---|
| Kth Largest Element in Array | Medium | https://leetcode.com/problems/kth-largest-element-in-an-array/ |
| Top K Frequent Elements | Medium | https://leetcode.com/problems/top-k-frequent-elements/ |
| Merge K Sorted Lists | Hard | https://leetcode.com/problems/merge-k-sorted-lists/ |
| Find Median from Data Stream | Hard | https://leetcode.com/problems/find-median-from-data-stream/ |
| Task Scheduler | Medium | https://leetcode.com/problems/task-scheduler/ |

---

### 📌 Pattern 9: Binary Search
**Core Concepts:** Binary search on answer, rotated arrays

| Problem | Difficulty | Link |
|---|---|---|
| Binary Search | Easy | https://leetcode.com/problems/binary-search/ |
| Search in Rotated Sorted Array | Medium | https://leetcode.com/problems/search-in-rotated-sorted-array/ |
| Find Minimum in Rotated Sorted Array | Medium | https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/ |
| Capacity to Ship Packages (BS on answer) | Medium | https://leetcode.com/problems/capacity-to-ship-packages-within-d-days/ |
| Median of Two Sorted Arrays | Hard | https://leetcode.com/problems/median-of-two-sorted-arrays/ |
| Koko Eating Bananas | Medium | https://leetcode.com/problems/koko-eating-bananas/ |

---

### 📌 Pattern 10: Dynamic Programming
**Core Concepts:** 1D DP, 2D DP, memoization, tabulation

| Problem | Difficulty | Link |
|---|---|---|
| Climbing Stairs | Easy | https://leetcode.com/problems/climbing-stairs/ |
| House Robber | Medium | https://leetcode.com/problems/house-robber/ |
| Coin Change | Medium | https://leetcode.com/problems/coin-change/ |
| Longest Increasing Subsequence | Medium | https://leetcode.com/problems/longest-increasing-subsequence/ |
| Longest Common Subsequence | Medium | https://leetcode.com/problems/longest-common-subsequence/ |
| 0/1 Knapsack | Medium | https://www.geeksforgeeks.org/0-1-knapsack-problem-dp-10/ |
| Edit Distance | Hard | https://leetcode.com/problems/edit-distance/ |
| Word Break | Medium | https://leetcode.com/problems/word-break/ |
| Unique Paths | Medium | https://leetcode.com/problems/unique-paths/ |
| Maximum Product Subarray | Medium | https://leetcode.com/problems/maximum-product-subarray/ |

---

### 📌 Pattern 11: Backtracking
**Core Concepts:** Subsets, permutations, combinations

| Problem | Difficulty | Link |
|---|---|---|
| Subsets | Medium | https://leetcode.com/problems/subsets/ |
| Permutations | Medium | https://leetcode.com/problems/permutations/ |
| Combination Sum | Medium | https://leetcode.com/problems/combination-sum/ |
| N-Queens | Hard | https://leetcode.com/problems/n-queens/ |
| Word Search | Medium | https://leetcode.com/problems/word-search/ |

---

### 📌 Pattern 12: Tries
**Core Concepts:** Prefix tree for string problems

| Problem | Difficulty | Link |
|---|---|---|
| Implement Trie | Medium | https://leetcode.com/problems/implement-trie-prefix-tree/ |
| Word Search II | Hard | https://leetcode.com/problems/word-search-ii/ |
| Design Add and Search Words Data Structure | Medium | https://leetcode.com/problems/design-add-and-search-words-data-structure/ |

---

### 🎯 Priority Order for 2 Days
```
Day 1 (Core Patterns):
  Arrays + Sliding Window → Hashing → Stacks → Linked Lists → Trees (BFS/DFS)

Day 2 (Advanced + Weak Areas):
  Graphs → Binary Search → Heaps → DP (1D) → Backtracking basics
```

---

## 3. Java Core & OOP Questions

### Topics to Revise
- **Collections Framework:** HashMap, TreeMap, LinkedHashMap, PriorityQueue, Deque, ArrayDeque
- **Generics & Comparable/Comparator**
- **Exception handling:** checked vs unchecked, custom exceptions
- **Functional interfaces:** `Function`, `Predicate`, `Consumer`, `Supplier`
- **Streams API:** `filter`, `map`, `flatMap`, `reduce`, `collect`, `groupingBy`, `sorted`
- **Optional**
- **Concurrency:** `synchronized`, `ReentrantLock`, `ExecutorService`, `CompletableFuture`, `CountDownLatch`, `Semaphore`
- **Java Memory Model:** heap vs stack, GC basics
- **Java 17+ features:** Records, Sealed classes, Pattern matching `instanceof`
- **Java 21:** Virtual threads (Project Loom), structured concurrency

### Expected Questions & Answers

**Q: What is the difference between `HashMap` and `ConcurrentHashMap`?**
> **Answer:**
> - `HashMap` is not thread-safe; multiple threads can corrupt it
> - `ConcurrentHashMap` uses **segment-based locking** (Java 7) or **bucket-level locking** (Java 8+) — only locks the segment being modified, not the entire map
> - `ConcurrentHashMap` allows concurrent reads without locking; only writes lock
> - Use `ConcurrentHashMap` in multi-threaded scenarios (e.g., your rate limiter with Redis, or shared caches in Spring)

---

**Q: How does `HashMap` work internally? What happens on hash collision?**
> **Answer:**
> `HashMap` uses an **array of buckets** (default size 16). For each key:
> 1. `hashCode()` determines the bucket index: `index = hash(key) % buckets.length`
> 2. If bucket is empty, insert the entry
> 3. If bucket has entries (collision), use **chaining** — store entries in a linked list (Java 7) or **balanced tree** (Java 8+, if chain length > 8)
> 4. On retrieval, hash to bucket, then iterate the chain/tree to find the matching key
>
> **Load factor (default 0.75):** When `size > capacity * 0.75`, the map **resizes** (doubles capacity) and rehashes all entries.
>
> **Why Java 8 switched to trees:** If many collisions, chain lookup becomes O(n). Trees are O(log n).

---

**Q: Difference between `Comparable` and `Comparator`?**
> **Answer:**
>
> | | Comparable | Comparator |
> |---|---|---|
> | Interface | `implements Comparable<T>` | `implements Comparator<T>` |
> | Method | `compareTo(T other)` | `compare(T a, T b)` |
> | When | Natural order (built-in) | Custom order (external) |
> | Example | `class User implements Comparable` | `new Comparator<User>() { compare(...) }` |
>
> ```java
> // Comparable — natural order
> class User implements Comparable<User> {
>     public int compareTo(User other) {
>         return this.age - other.age;  // ascending by age
>     }
> }
>
> // Comparator — custom order
> List<User> users = ...;
> users.sort(Comparator.comparing(User::getAge).reversed());  // descending
> ```

---

**Q: What is a `CompletableFuture`? How is it different from `Future`?**
> **Answer:**
> - `Future` is a placeholder for a result that will be available later. You can only **block and wait** for it: `result = future.get()`
> - `CompletableFuture` is a `Future` you can **complete manually** and **chain operations** on
>
> ```java
> // Future — blocking
> Future<String> future = executor.submit(() -> fetchData());
> String result = future.get();  // blocks until done
>
> // CompletableFuture — non-blocking, chainable
> CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> fetchData())
>     .thenApply(data -> transform(data))
>     .thenAccept(result -> System.out.println(result));
> // No blocking — continues immediately
> ```
>
> Use `CompletableFuture` for async workflows, combining multiple async operations, and non-blocking I/O.

---

**Q: What is the difference between `synchronized` method and `synchronized` block?**
> **Answer:**
> - `synchronized` method locks the **entire method** — coarse-grained
> - `synchronized` block locks only the **critical section** — fine-grained, better performance
>
> ```java
> // Method — locks entire method
> synchronized void updateUser(User u) {
>     db.save(u);
>     cache.put(u.id, u);
> }
>
> // Block — locks only the critical part
> void updateUser(User u) {
>     db.save(u);  // no lock
>     synchronized(cache) {
>         cache.put(u.id, u);  // only this is locked
>     }
> }
> ```

---

**Q: What are virtual threads (Java 21)? How do they differ from platform threads?**
> **Answer:**
> Virtual threads (Project Loom) are **lightweight threads** that run on a small pool of platform threads (OS threads).
>
> | | Platform Thread | Virtual Thread |
> |---|---|---|
> | Cost | Heavy (1-2MB memory, OS context switch) | Lightweight (few KB) |
> | Count | ~1000s per JVM | Millions per JVM |
> | Blocking | Blocks the OS thread | Scheduler parks it, reuses platform thread |
> | Use | CPU-bound, limited concurrency | I/O-bound, massive concurrency |
>
> ```java
> // Virtual threads — can spawn millions
> try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
>     for (int i = 0; i < 1_000_000; i++) {
>         executor.submit(() -> blockingIO());  // no problem
>     }
> }
> ```

---

**Q: Explain the difference between `==` and `.equals()` for Strings.**
> **Answer:**
> - `==` compares **object identity** (same memory address)
> - `.equals()` compares **content**
>
> ```java
> String a = new String("hello");
> String b = new String("hello");
> String c = "hello";
>
> a == b  // false (different objects)
> a.equals(b)  // true (same content)
> c == "hello"  // true (string literal pool)
> ```

---

**Q: What is the `volatile` keyword? When would you use it?**
> **Answer:**
> `volatile` ensures that **reads and writes to a variable are visible across threads** — no caching in CPU registers.
>
> Use when:
> - A variable is shared between threads
> - You need visibility without full synchronization (lighter than `synchronized`)
> - Example: flags, counters that don't need atomic operations
>
> ```java
> volatile boolean shutdown = false;
>
> // Thread 1
> while (!shutdown) {
>     process();
> }
>
> // Thread 2
> shutdown = true;  // visible to Thread 1 immediately
> ```

---

**Q: Explain `CopyOnWriteArrayList`. When is it preferred?**
> **Answer:**
> `CopyOnWriteArrayList` creates a **new copy of the array on every write** — reads are lock-free and fast.
>
> Use when:
> - Reads >> writes (e.g., event listeners, observer lists)
> - Small lists (copying is expensive for large lists)
>
> ```java
> CopyOnWriteArrayList<EventListener> listeners = new CopyOnWriteArrayList<>();
> // Reads (no lock)
> for (EventListener l : listeners) l.onEvent(e);
> // Write (copies entire array)
> listeners.add(newListener);
> ```

---

**Q: What are functional interfaces? Name 5 from `java.util.function`.**
> **Answer:**
> A functional interface has **exactly one abstract method**. Can be implemented with lambda expressions.
>
> ```java
> // 5 common functional interfaces
> Function<T, R>        // T → R (transform)
> Predicate<T>          // T → boolean (test)
> Consumer<T>           // T → void (side effect)
> Supplier<T>           // () → T (produce)
> BiFunction<T, U, R>   // (T, U) → R (two args)
>
> // Examples
> Function<String, Integer> length = s -> s.length();
> Predicate<Integer> isEven = n -> n % 2 == 0;
> Consumer<String> print = s -> System.out.println(s);
> Supplier<LocalDateTime> now = () -> LocalDateTime.now();
> ```

---

## 4. Spring Boot & Microservices Questions

### Topics to Revise
- **Spring IoC container:** BeanFactory vs ApplicationContext, bean scopes
- **Spring annotations:** `@Component`, `@Service`, `@Repository`, `@Controller`, `@RestController`, `@Bean`, `@Configuration`
- **Dependency Injection:** constructor vs field vs setter injection
- **Spring Boot auto-configuration:** how it works under the hood
- **Spring Data JPA:** repositories, `@Query`, `Specification`, transactions, lazy vs eager loading
- **Spring Security:** filter chain, JWT authentication, OAuth2
- **Spring Cloud:** Gateway, Config Server, Eureka
- **REST API design:** idempotency, status codes, versioning
- **Transaction management:** `@Transactional`, propagation levels, isolation levels

### Expected Questions & Answers

**Q: How does Spring Boot auto-configuration work?**
> **Answer:**
> Spring Boot uses `@SpringBootApplication` which includes `@EnableAutoConfiguration`. This scans the classpath for `spring.factories` files in JAR META-INF directories and loads configuration classes conditionally.
>
> ```
> @EnableAutoConfiguration
>     ↓
> Scans META-INF/spring.factories in all JARs
>     ↓
> Loads @Configuration classes conditionally (@ConditionalOnClass, @ConditionalOnProperty)
>     ↓
> Example: If Kafka is on classpath → KafkaAutoConfiguration loads
> ```
>
> You can override with `application.yml` or `@Bean` methods in your `@Configuration` class.

---

**Q: What is the difference between `@Component`, `@Service`, `@Repository`?**
> **Answer:**
> All are `@Component` stereotypes (Spring marks them for auto-wiring). Semantic difference:
>
> - `@Component` — generic, any Spring-managed bean
> - `@Service` — business logic layer (semantic clarity)
> - `@Repository` — data access layer (Spring adds exception translation — DB exceptions → `DataAccessException`)
>
> ```java
> @Repository
> public class UserRepository {
>     // Spring catches SQLException and wraps as DataAccessException
> }
>
> @Service
> public class UserService {
>     @Autowired UserRepository repo;
> }
> ```

---

**Q: Explain bean lifecycle in Spring.**
> **Answer:**
> 1. **Instantiation** — constructor called
> 2. **Populate properties** — `@Autowired` fields injected
> 3. **BeanNameAware / BeanFactoryAware** — if bean implements these, callbacks invoked
> 4. **@PostConstruct** — custom init method runs
> 5. **Bean ready** — available for use
> 6. **@PreDestroy** — cleanup when context closes
>
> ```java
> @Component
> public class MyBean {
>     @PostConstruct
>     void init() {
>         System.out.println("Bean initialized");
>     }
>
>     @PreDestroy
>     void cleanup() {
>         System.out.println("Bean destroyed");
>     }
> }
> ```

---

**Q: What is `@Transactional` propagation `REQUIRED` vs `REQUIRES_NEW`?**
> **Answer:**
>
> | | REQUIRED | REQUIRES_NEW |
> |---|---|---|
> | Behavior | Uses existing transaction if present, else creates new | Always creates new transaction |
> | Rollback | Shares transaction — if inner fails, outer rolls back | Independent — inner can rollback without affecting outer |
> | Use case | Default, most common | Logging, audit trails that should persist even if main fails |
>
> ```java
> @Transactional(propagation = Propagation.REQUIRED)
> void updateUser(User u) {
>     userRepo.save(u);
>     logEvent("User updated");  // same transaction
> }
>
> @Transactional(propagation = Propagation.REQUIRES_NEW)
> void logEvent(String msg) {
>     // separate transaction — commits even if updateUser rolls back
> }
> ```

---

**Q: What is the N+1 problem in JPA? How do you solve it?**
> **Answer:**
> N+1 = 1 query to fetch N parent objects, then N queries to fetch their children.
>
> ```java
> // BAD — N+1 queries
> List<User> users = userRepo.findAll();  // 1 query
> for (User u : users) {
>     u.getOrders();  // N queries (lazy load)
> }
>
> // GOOD — 1 query with JOIN
> List<User> users = userRepo.findAll(Specification.where(
>     (root, query, cb) -> {
>         root.fetch("orders", JoinType.LEFT);
>         return null;
>     }
> ));
> ```
>
> Or use `@Query` with `JOIN FETCH`:
> ```java
> @Query("SELECT u FROM User u JOIN FETCH u.orders")
> List<User> findAllWithOrders();
> ```

---

**Q: What is the difference between `EAGER` and `LAZY` fetching?**
> **Answer:**
> - `EAGER` — fetch related data immediately (JOIN in SQL)
> - `LAZY` — fetch only when accessed (separate query on access)
>
> ```java
> @Entity
> public class User {
>     @OneToMany(fetch = FetchType.LAZY)  // default
>     List<Order> orders;  // loaded only when accessed
>
>     @ManyToOne(fetch = FetchType.EAGER)
>     Company company;  // loaded immediately
> }
> ```
>
> **Best practice:** Use LAZY by default (avoids N+1), explicitly JOIN FETCH when needed.

---

**Q: What are JPA Specifications? When would you use them? *(Your real experience!)*
> **Answer:**
> `Specification` allows **dynamic, type-safe query building** — perfect for complex filters.
>
> ```java
> public class EventSpecifications {
>     public static Specification<Event> bySeverity(String severity) {
>         return (root, query, cb) -> cb.equal(root.get("severity"), severity);
>     }
>
>     public static Specification<Event> byTenantId(String tenantId) {
>         return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
>     }
>
>     public static Specification<Event> byTimeRange(LocalDateTime start, LocalDateTime end) {
>         return (root, query, cb) -> cb.between(root.get("timestamp"), start, end);
>     }
> }
>
> // Compose dynamically
> Specification<Event> spec = Specification.where(null);
> if (severity != null) spec = spec.and(bySeverity(severity));
> if (tenantId != null) spec = spec.and(byTenantId(tenantId));
> if (startTime != null) spec = spec.and(byTimeRange(startTime, endTime));
>
> List<Event> events = eventRepo.findAll(spec);
> ```
>
> **Your use case:** Global search on EV charging platform — composed filters for severity, tenant, time range, source IP. Specifications made this clean and maintainable.

---

**Q: How do you implement circuit breaker in Spring Boot? *(Resilience4j)*
> **Answer:**
> Circuit breaker prevents cascading failures — if downstream service is down, fail fast instead of retrying.
>
> ```java
> @Service
> public class PaymentService {
>     @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
>     public String processPayment(String orderId) {
>         return restTemplate.getForObject("http://payment-service/pay/" + orderId, String.class);
>     }
>
>     public String paymentFallback(String orderId, Exception e) {
>         return "Payment processing delayed, will retry later";
>     }
> }
> ```
>
> **States:** CLOSED (normal) → OPEN (fail fast) → HALF_OPEN (test recovery) → CLOSED
>
> Config in `application.yml`:
> ```yaml
> resilience4j:
>   circuitbreaker:
>     instances:
>       paymentService:
>         failure-rate-threshold: 50
>         wait-duration-in-open-state: 10000
> ```

---

**Q: Explain Spring Cloud Gateway — how does routing and filter chain work?**
> **Answer:**
> Spring Cloud Gateway is an API Gateway that routes requests to microservices and applies filters.
>
> ```yaml
> spring:
>   cloud:
>     gateway:
>       routes:
>         - id: user-service
>           uri: lb://USER-SERVICE
>           predicates:
>             - Path=/api/users/**
>           filters:
>             - name: CircuitBreaker
>               args:
>                 name: userServiceCB
>             - name: RateLimiter
>               args:
>                 redis-rate-limiter.replenishRate: 10
>                 redis-rate-limiter.burstCapacity: 20
> ```
>
> **Filter chain execution:**
> ```
> Request → Pre-filters (auth, logging) → Route to service → Post-filters (response transform) → Response
> ```

---

**Q: What is the difference between `@RestController` and `@Controller`?**
> **Answer:**
> - `@Controller` — returns view names (HTML templates)
> - `@RestController` — returns JSON/XML directly (equivalent to `@Controller + @ResponseBody`)
>
> ```java
> @Controller
> public class WebController {
>     @GetMapping("/users")
>     public String getUsers(Model model) {
>         model.addAttribute("users", userService.findAll());
>         return "users";  // returns view name
>     }
> }
>
> @RestController
> public class ApiController {
>     @GetMapping("/api/users")
>     public List<User> getUsers() {
>         return userService.findAll();  // returns JSON
>     }
> }
> ```

---

### 🔥 Deep Dive — From Your Resume
> **"Architected global search using JPA Specifications and materialized views, cutting latency from 1.2s to ~120ms"**

**Q: Walk through how `JPA Specification` composes dynamic predicates**
> See answer above — Specifications allow combining filters with `.and()` and `.or()` for flexible query building.

**Q: Why materialized views rather than regular views?**
> - Regular view: computed on every query (slow if complex joins)
> - Materialized view: pre-computed, stored as physical table (fast reads, stale data)
> - Your case: EV charging search required joining 3-4 tables with aggregations. Materialized view pre-computed the expensive joins, queries hit the materialized table instead.

**Q: How do you refresh a materialized view without downtime?**
> ```sql
> -- Postgres: concurrent refresh (doesn't lock)
> REFRESH MATERIALIZED VIEW CONCURRENTLY mv_charging_search;
> ```
> Requires a unique index on the view. Refreshes in background, old data available until refresh completes.

**Q: What indexes did you add and why?**
> Partial indexes on high-cardinality filter columns:
> ```sql
> CREATE INDEX idx_charging_severity ON mv_charging_search(severity) WHERE severity = 'HIGH';
> CREATE INDEX idx_charging_tenant ON mv_charging_search(tenant_id);
> ```
> Reduces index size, speeds up common filters (severity, tenant).

---

## 5. Kafka & Messaging Questions

### Topics to Revise
- **Core concepts:** producer, consumer, topic, partition, offset, consumer group
- **Replication:** leader, follower, ISR (In-Sync Replicas)
- **Delivery semantics:** at-most-once, at-least-once, exactly-once
- **Kafka transactions and idempotent producers**
- **Kafka Saga pattern** — your CodeForgeUI experience
- **Consumer lag and rebalancing**
- **Kafka Streams vs Kafka Consumer API**
- **Dead Letter Queue (DLQ) pattern**
- **Kafka vs SQS vs RabbitMQ** trade-offs

### Expected Questions
- What is the role of a partition in Kafka? How does Kafka achieve parallelism?
- What does "consumer group" mean? What happens when a new consumer joins a group?
- Explain "exactly-once semantics" in Kafka. How do you achieve it?
- What is the difference between `acks=0`, `acks=1`, and `acks=all`?
- How do you handle message ordering in Kafka?
- What is a Dead Letter Queue? How would you implement one in Kafka?
- How does Kafka compare to SQS for a high-throughput pipeline?
- What is Kafka Saga orchestration? *(From CodeForgeUI — own this answer!)*
- What is consumer lag? How would you monitor and fix it?
- How do you handle exactly-once idempotent file persistence with Kafka? *(CodeForgeUI!)*

### 🔥 Deep Dive — From Your Resume
> **"Kafka Saga orchestration for exactly-once idempotent file persistence"**
- Explain the Saga pattern: choreography vs orchestration
- How does idempotency key work in your implementation?
- What happens if the Kafka consumer crashes mid-saga?

---

## 6. Kubernetes & Docker Questions

### Topics to Revise
- **Core objects:** Pod, Deployment, Service, Ingress, ConfigMap, Secret, PersistentVolume
- **Scheduling:** resource requests/limits, node affinity, taints/tolerations
- **CronJobs and Jobs** *(your direct experience!)*
- **Helm charts and parameterized templates** *(your direct experience!)*
- **Liveness vs Readiness vs Startup probes**
- **HPA (Horizontal Pod Autoscaler)**
- **Kubernetes networking:** ClusterIP, NodePort, LoadBalancer, Ingress
- **Secrets management:** environment variables vs mounted secrets
- **Amazon EKS vs ECS** — differences

### Expected Questions
- What is the difference between a `Pod` and a `Deployment`?
- What are liveness and readiness probes? Why are both needed?
- How does Kubernetes handle rolling updates?
- What is Helm? What problem does it solve?
- What is a Kubernetes CronJob? How does it differ from a regular Job?
- How do you manage secrets securely in Kubernetes?
- What is HPA? How does it scale based on custom metrics?
- How would you debug a pod that is in `CrashLoopBackOff`?
- Explain your Kubernetes-native job execution framework. *(Your real experience!)*

### 🔥 Deep Dive — From Your Resume
> **"Kubernetes-native job execution framework for Python workloads with secure CronJobs, adhoc execution, and parameterized Helm templates"**
- How did you handle parameterization across environments (dev/staging/prod)?
- How did you implement adhoc execution on demand?
- How did you secure secrets in the CronJob manifests?

---

## 7. Database (PostgreSQL, Redis, Elasticsearch)

### PostgreSQL
**Topics:** Indexes (B-tree, GIN, partial), query planning (`EXPLAIN ANALYZE`), CTEs, window functions, materialized views, connection pooling (PgBouncer), transactions, MVCC

**Expected Questions:**
- What is the difference between a clustered and non-clustered index?
- What is a materialized view? When would you use it over a regular view?
- How does PostgreSQL MVCC work?
- What is a partial index? When is it useful?
- Explain `EXPLAIN ANALYZE` output — what are "Seq Scan" vs "Index Scan"?
- What are window functions? Give an example of `ROW_NUMBER()` vs `RANK()`.
- What is a CTE? When would you use recursive CTEs?
- What is connection pooling? Why does it matter for high-throughput services?

### Redis
**Topics:** Data structures (String, Hash, List, Set, SortedSet, Stream), TTL, pub/sub, distributed locks (SETNX/Redlock), token bucket rate limiting

**Expected Questions:**
- What Redis data structure would you use for a leaderboard? *(SortedSet)*
- How does Redis implement distributed locking?
- How did you implement token bucket rate limiting with Redis? *(Your real experience!)*
- What is the difference between Redis `EXPIRE` and `PERSIST`?
- What is Redis pub/sub? How does it differ from Kafka?

### Elasticsearch
**Topics:** Inverted index, shards/replicas, index mapping, query DSL (`match`, `term`, `range`, `bool`, `must/should/filter`), aggregations, relevance scoring (TF-IDF, BM25)

**Expected Questions:**
- What is an inverted index? How does Elasticsearch use it for fast full-text search?
- What is the difference between a `match` query and a `term` query?
- What are shards and replicas in Elasticsearch?
- How would you design an Elasticsearch index for cybersecurity event logs?
- What is a `bool` query? Explain `must`, `should`, and `filter` clauses.
- How does Elasticsearch handle relevance scoring?
- How would you sync data from PostgreSQL into Elasticsearch? *(CDC + Kafka + ES consumer)*
- What is Elasticsearch DSL? Write a query to filter events by timestamp and severity.

---

## 8. Distributed Systems & System Design Concepts

### Core Concepts to Revise
- **CAP Theorem:** Consistency, Availability, Partition Tolerance — trade-offs
- **Event-driven architecture:** event sourcing, CQRS
- **Idempotency:** why it matters, implementation patterns
- **Distributed transactions:** Saga pattern, 2-phase commit
- **Rate limiting:** token bucket, leaky bucket, sliding window
- **Circuit breaker pattern**
- **Sidecar pattern** in Kubernetes
- **Observability:** metrics (Prometheus/Micrometer), tracing (OpenTelemetry/Zipkin), logging (Datadog)
- **Data lakehouse:** Delta Lake, Apache Iceberg, Databricks, Parquet
- **Stream processing:** Kafka Streams, Spark Streaming

### System Design Questions — Full Solutions

---

#### 🏗️ SD-1: Design a Real-Time Security Telemetry Ingestion Pipeline
> *Kafka → Processing → Elasticsearch (most likely asked at Arctic Wolf)*

**Requirements:**
- Ingest endpoint/network/cloud telemetry events from 10,000+ customers
- Store events durably and make them searchable within seconds
- Handle ~millions of events/min at peak; tolerate bursts
- No data loss; at-least-once delivery with idempotent writes

**Architecture:**

```
Agents (Endpoint/Network/Cloud)
        │
        ▼
  [API Ingest Layer]          ← Spring Boot / Go service
  (Auth, validation,          ← Validates schema, stamps tenantId + timestamp
   schema normalization)
        │
        ▼
  [Apache Kafka]              ← Topic: raw-telemetry (partitioned by tenantId)
  (Durable log buffer)        ← replication-factor=3, acks=all, retention=24h
        │
        ├──────────────────────────────────────────┐
        ▼                                          ▼
  [Stream Processor]                     [Raw S3/Delta Lake]
  (Kafka Streams / Flink)                ← long-term cold storage
  - Enrich (GeoIP, asset lookup)         ← Parquet + Delta for time travel
  - Filter noise / dedup
  - Normalize to common schema
        │
        ▼
  [Elasticsearch Index]       ← Index: telemetry-{YYYY.MM.dd}
  - Index per day (ILM policy)
  - Shards partitioned by tenantId routing
  - Hot → Warm → Cold → Delete tier lifecycle
        │
        ▼
  [Detection Engine]          ← Reads from ES for rule matching
  [Customer Search UI]        ← Kibana-style search over their own events
```

**Key Design Decisions to Discuss:**
- **Partitioning by `tenantId`**: ensures all events for a tenant go to the same partition → ordered per tenant, and consumer parallelism maps to tenant shards
- **`acks=all` + `min.insync.replicas=2`**: guarantees no data loss even if a broker dies mid-write
- **Idempotent consumer → ES**: use `eventId` as the Elasticsearch document `_id`. ES upserts are idempotent — same event written twice = no duplicate
- **Index per day + ILM (Index Lifecycle Management)**: hot index for writes/searches today, warm (read-only, compressed) for last 7 days, cold for 30 days, delete after 90 days
- **Backpressure**: if ES falls behind, Kafka consumer pauses via `pause()`/`resume()` on partitions; Kafka retains events up to retention period
- **Dead Letter Queue**: malformed events → `telemetry-dlq` topic → alert + manual inspection

**Follow-up: No data loss guarantee**
> Producer: `acks=all`, idempotent producer (`enable.idempotence=true`).
> Consumer: commit offset **after** successful ES write, not before (at-least-once).
> ES: use `_id = eventId` for idempotent upserts.

---

#### 🏗️ SD-2: Design a Distributed Rate Limiter
> *Token bucket with Redis — you built this in CodeForgeUI (1,200+ req/s)*

**Requirements:**
- Limit each user/API key to N requests per second
- Must work across multiple API gateway instances (distributed)
- Low latency — must not add >5ms to request path
- Handle bursts gracefully (token bucket, not hard cutoff)

**Algorithm — Token Bucket:**
```
- Each user has a "bucket" with capacity = MAX_TOKENS
- Tokens refill at rate R per second
- Each request consumes 1 token
- If bucket empty → reject with HTTP 429
```

**Architecture:**

```
Client Request
      │
      ▼
[API Gateway Instance 1..N]   ← Spring Cloud Gateway / custom filter
      │
      ▼ (before routing)
[Rate Limit Filter]
      │
      ▼
[Redis]  ← single source of truth for token counts
      │
      └─ key: "rl:{userId}" → {tokens, lastRefillTime}
```

**Redis Lua Script (atomic — no race conditions):**
```lua
local key = KEYS[1]
local max_tokens = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])   -- tokens per second
local now = tonumber(ARGV[3])           -- current epoch ms

local data = redis.call("HMGET", key, "tokens", "last_refill")
local tokens = tonumber(data[1]) or max_tokens
local last_refill = tonumber(data[2]) or now

-- Calculate new tokens based on elapsed time
local elapsed = (now - last_refill) / 1000.0
local new_tokens = math.min(max_tokens, tokens + elapsed * refill_rate)

if new_tokens >= 1 then
    new_tokens = new_tokens - 1
    redis.call("HMSET", key, "tokens", new_tokens, "last_refill", now)
    redis.call("EXPIRE", key, 3600)
    return 1   -- allowed
else
    return 0   -- rejected
end
```

**Why Lua script?** Redis executes Lua atomically — no race condition between read-modify-write across distributed gateway instances.

**Spring Cloud Gateway Filter (your real code pattern):**
```java
@Component
public class RateLimitGatewayFilter implements GatewayFilter {
    private final ReactiveRedisTemplate<String, String> redis;
    private final RedisScript<Long> rateLimitScript;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        String key = "rl:" + userId;
        return redis.execute(rateLimitScript, List.of(key), MAX_TOKENS, REFILL_RATE, currentTimeMs())
            .flatMap(allowed -> {
                if (allowed == 1L) return chain.filter(exchange);
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                return exchange.getResponse().setComplete();
            });
    }
}
```

**Scale to 1,200+ req/s:** Redis single-threaded but handles ~100k ops/sec. Lua script = 1 round trip per request. Redis Cluster for further horizontal scaling.

---

#### 🏗️ SD-3: Design an API Gateway
> *You built this in CodeForgeUI — Spring Cloud Gateway + Redis + Eureka + JWT*

**Requirements:**
- Single entry point for all microservices
- JWT authentication (stateless)
- Rate limiting per user
- Dynamic routing + service discovery
- Centralized config

**Architecture:**

```
Client
  │
  ▼
[Spring Cloud Gateway]
  │
  ├─── Filter 1: JWT Auth Filter
  │       └─ Validate JWT signature (HMAC/RSA)
  │          Extract userId, roles → propagate via headers
  │
  ├─── Filter 2: Rate Limit Filter
  │       └─ Redis token bucket (see SD-2)
  │
  ├─── Filter 3: Request Logging / Tracing
  │       └─ Inject traceId header (OpenTelemetry)
  │
  ├─── Route Table (from Spring Cloud Config)
  │       /api/users/**   → user-service
  │       /api/orders/**  → order-service
  │       /api/ai/**      → ai-service
  │
  └─── Eureka Client
          └─ Resolve service-name → actual pod IPs
             Client-side load balancing via Spring Cloud LoadBalancer
```

**JWT Auth Filter logic:**
```
1. Extract "Authorization: Bearer <token>" header
2. Validate signature using secret / public key
3. Check expiry claim
4. Extract userId and roles
5. Forward as X-User-Id / X-Roles headers to downstream service
6. Downstream trusts gateway — no re-validation needed
```

**Config-driven routing (application.yml):**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://USER-SERVICE        # lb:// = Eureka load-balanced
          predicates:
            - Path=/api/users/**
          filters:
            - JwtAuthFilter
            - RateLimitFilter
            - name: CircuitBreaker
              args:
                name: userServiceCB
                fallbackUri: forward:/fallback/users
```

**Resilience patterns:**
- Circuit breaker via Resilience4j on each route
- Retry with exponential backoff for idempotent GETs
- Timeout per route (prevents slow upstream from cascading)

---

#### 🏗️ SD-4: Design a Search System over Large Security Event Logs
> *Core to Arctic Wolf's product — Elasticsearch at scale*

**Requirements:**
- Search billions of security events per customer
- Queries: filter by time range, severity, event type, source IP, hostname
- Full-text search on event message fields
- Sub-second response for common queries
- Data retention: 90 days hot, 1 year cold

**Architecture:**

```
[Ingestion Pipeline]          (see SD-1)
        │
        ▼
[Elasticsearch Cluster]
  ├─ Hot nodes (SSD)          ← current day + last 7 days
  ├─ Warm nodes (HDD)         ← 8–30 days
  └─ Cold / Frozen (S3)       ← 31–365 days (searchable but slow)
```

**Index Design:**
```json
PUT /telemetry-2026.10.03
{
  "settings": {
    "number_of_shards": 6,
    "number_of_replicas": 1,
    "routing.allocation.require.box_type": "hot"
  },
  "mappings": {
    "properties": {
      "tenantId":    { "type": "keyword" },
      "timestamp":   { "type": "date" },
      "severity":    { "type": "keyword" },
      "eventType":   { "type": "keyword" },
      "sourceIp":    { "type": "ip" },
      "hostname":    { "type": "keyword" },
      "message":     { "type": "text", "analyzer": "standard" },
      "rawEvent":    { "type": "object", "enabled": false }
    }
  }
}
```

**Key decisions:**
- `keyword` for exact-match fields (tenantId, severity, eventType) — no tokenization → fast filter
- `text` for message — full-text analyzed → inverted index for relevance search
- `ip` type for sourceIp — enables CIDR range queries natively
- `rawEvent` disabled — stored but not indexed (saves index size)
- **Routing by `tenantId`**: `_routing=tenantId` → all tenant docs in same shard → query hits 1 shard instead of all 6

**Sample Query — "Show me HIGH severity events from sourceIp 192.168.1.0/24 in last 1 hour":**
```json
GET /telemetry-*/_search
{
  "query": {
    "bool": {
      "filter": [
        { "term":  { "tenantId": "tenant-abc" }},
        { "term":  { "severity": "HIGH" }},
        { "range": { "timestamp": { "gte": "now-1h" }}},
        { "term":  { "sourceIp": "192.168.1.0/24" }}
      ]
    }
  },
  "sort": [{ "timestamp": "desc" }],
  "size": 100
}
```
> Use `filter` (not `must`) for all non-relevance conditions → results are cached, faster.

**ILM Policy:**
```
Hot (0–7 days):  rollover at 50GB or 1 day → active writes + fast searches
Warm (8–30 days): readonly, force-merge to 1 segment, move to HDD nodes
Cold (31–90 days): readonly, compress, move to frozen / S3-backed
Delete: after 365 days
```

---

#### 🏗️ SD-5: Design a Distributed Job Scheduler
> *You built this at Mercedes-Benz — K8s CronJobs + adhoc execution + Helm*

**Requirements:**
- Schedule recurring batch jobs (cron-based)
- Support adhoc/on-demand job triggers via REST API
- Parameterized jobs (different config per environment/customer)
- Secure secret injection (no secrets in job args)
- Observability: job success/failure tracking

**Architecture:**

```
[REST API / Scheduler Service]   ← Spring Boot — job control plane
        │
        ├─── GET  /jobs              → list registered jobs
        ├─── POST /jobs/{id}/trigger → trigger adhoc execution
        └─── GET  /jobs/{id}/runs    → run history
        │
        ▼
[Kubernetes API Server]
  ├─ CronJob manifests (Helm-deployed)
  │     └─ schedule: "0 2 * * *"
  │        image: our-python-runner:v1.2
  │        envFrom: secretRef → K8s Secret (not plain text)
  │
  └─ Adhoc Job (created dynamically by Scheduler Service)
        └─ Scheduler calls k8s Java client: batchV1().createNamespacedJob(...)
           with user-provided params as env vars
```

**Helm Template (parameterized):**
```yaml
# templates/cronjob.yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: {{ .Values.job.name }}
spec:
  schedule: {{ .Values.job.schedule | quote }}
  jobTemplate:
    spec:
      template:
        spec:
          containers:
            - name: runner
              image: {{ .Values.image.repository }}:{{ .Values.image.tag }}
              args: ["--mode={{ .Values.job.mode }}", "--tenant={{ .Values.job.tenant }}"]
              envFrom:
                - secretRef:
                    name: {{ .Values.job.secretName }}   # injected from K8s Secret
          restartPolicy: OnFailure
```

**Adhoc execution via Kubernetes Java Client:**
```java
@PostMapping("/jobs/{jobId}/trigger")
public ResponseEntity<String> triggerJob(@PathVariable String jobId,
                                          @RequestBody Map<String, String> params) {
    V1Job job = buildJobFromTemplate(jobId, params);  // copy from CronJob template
    batchV1Api.createNamespacedJob("default", job, null, null, null, null);
    return ResponseEntity.accepted().body("Job submitted: " + job.getMetadata().getName());
}
```

**Observability:**
- Each Job pod emits structured logs → Datadog picks up exit code
- Scheduler service watches Job status via K8s `watch` API → updates run history in DB
- Alert on `job.status.failed > 0` via Prometheus alert rule

**Secret injection:**
- Secrets stored in K8s Secrets (base64, etcd-encrypted at rest)
- Mounted as env vars via `envFrom.secretRef` — never passed as CLI args
- Rotation: update K8s Secret → pods pick up on next run

---

### Expected Discussion Points — Answers

**Q: How would you ensure no data loss in a Kafka consumer pipeline?**
> 1. Producer: `acks=all`, `enable.idempotence=true`, `retries=Integer.MAX_VALUE`
> 2. Consumer: manual offset commit (`enable.auto.commit=false`) — commit **after** successful processing
> 3. Downstream write: use idempotency key (e.g., `eventId` as ES `_id`) so retry = no duplicate
> 4. DLQ: after N retries, send to dead-letter topic for manual replay instead of discarding

**Q: How do you handle backpressure in a high-throughput stream?**
> Kafka consumer: call `consumer.pause(partitions)` when downstream (ES / DB) is slow → stop fetching new messages → Kafka buffers them.
> Resume with `consumer.resume(partitions)` when downstream recovers.
> Upstream producers don't slow down — Kafka absorbs the burst.
> For Kafka Streams: use `maxBytes` fetch config and internal buffer tuning.

**Q: What is the difference between event sourcing and CQRS?**
> **Event Sourcing:** State is derived from a sequence of immutable events. Instead of storing current state in DB, you store every state change as an event. Current state = replay of all events. Kafka is a natural event store.
> **CQRS (Command Query Responsibility Segregation):** Separate the write model (commands, events) from the read model (query-optimized views). Often combined with event sourcing — events update both the write store and a read-optimized view (Elasticsearch, materialized view).
> *Example:* EV charging platform — charging events written to Kafka (event source) → consumer updates PostgreSQL materialized view (CQRS read model) → API queries the view.

**Q: How would you implement idempotent message processing?**
> 1. Each message carries a unique `messageId` / `eventId`
> 2. Consumer checks a dedup store (Redis SET or DB table) before processing: `SETNX dedup:{messageId} 1 EX 86400`
> 3. If key already exists → skip (already processed)
> 4. If not → process → mark as processed
> 5. Alternatively: use the `messageId` as the primary key / `_id` in the write target — duplicate writes become no-ops (upsert semantics)

**Q: What is partition pruning in Spark/Databricks? Why does it matter?**
> Partition pruning = Spark skips reading entire data partitions that cannot match a query predicate, based on the partition column.
> Example: data on S3 partitioned as `year=2026/month=10/day=03/`. Query `WHERE date = '2026-10-03'` → Spark reads only `year=2026/month=10/day=03/` directory, skipping all others.
> Without pruning, Spark reads all partitions → massive unnecessary I/O on petabyte datasets.
> In Delta Lake: `ZORDER BY` further colocates related data within files → file skipping on non-partition columns.

---

## 9. Resume Deep-Dive Questions

> These are questions the interviewer will likely ask based specifically on your resume. Own every answer.

### Mercedes-Benz — EV Charging Platform

**Q: "Walk me through the architecture of your EV charging platform."**
> Answer should cover: Spring Boot microservices → Kafka event bus → PostgreSQL for persistence → Redis caching → Kubernetes deployment → Datadog monitoring. Mention EMEA/AMAP/China multi-region scope and 5M+ events/day scale.

**Q: "How did you design the Temporal-based workflow orchestration?"**
> Cover: Why Temporal over custom retry logic? How does Temporal handle durability (event sourcing of workflow state)? What is mTLS and why did you need it? What was your workflow staging framework?

**Q: "What is mTLS and why did you implement it for Temporal?"**
> mTLS = mutual TLS — both client and server present certificates. Used for service-to-service auth in zero-trust internal networks. Especially important for financial/automotive data.

**Q: "You overhauled Python batch jobs — what was wrong with the original implementation?"**
> Original loaded entire dataset into memory. You replaced it with cursor-based pagination (process N rows at a time), streaming output, and selective field parsing — reducing memory by ~75%.

**Q: "Tell me about your AI Agent for log summarization."**
> OpenAI Agents SDK → LLM receives structured log snippets → returns human-readable summary → reduced manual log analysis by ~70%. Mention prompt engineering, tool use, and how you integrated it into existing logging infrastructure.

---

### Mercedes-Benz — NeoAssist Platform

**Q: "What is a RAG architecture and how did you implement it?"**
> RAG = Retrieval-Augmented Generation. User query → embed query → vector search in Azure AI Search → retrieve top-K relevant documents → append to LLM prompt as context → LLM generates grounded response.
> Your stack: LangChain + Azure AI Search + OpenAI → served via Django REST.

**Q: "How did you integrate PingID MFA?"**
> PingID = enterprise identity provider. Implemented OAuth2 PKCE flow for user auth + MFA challenge. Built middleware to validate tokens and enrich request context with user identity.

**Q: "You onboarded 100+ teams. What were the main challenges?"**
> Reliability at scale, rate limiting per team (token bucket), onboarding documentation, middleware for Datadog usage metrics per team, handling varied use cases (code gen vs test gen).

---

### CodeForgeUI Project

**Q: "Explain the Kafka Saga orchestration for file persistence."**
> Saga choreography: each step in AI code generation emits an event → next step consumes it → on failure, compensating transaction fires. Idempotency key = requestId + stepId, deduplicated via Redis before writing to PostgreSQL.

**Q: "How does your SSE streaming work with OpenAI?"**
> Server-Sent Events: Spring Boot `SseEmitter` → calls OpenAI streaming API → pushes token chunks to client as they arrive. Frontend renders tokens in real time. Stateless — each stream is tied to a session token.

**Q: "How did you achieve <30 second Kubernetes live preview?"**
> Pre-warmed pod pool: maintain a pool of idle pods ready to serve. On request → Redis distributed lock to claim a pod → mount user files via MinIO sync → patch DNS for dynamic subdomain routing. No cold-start penalty.

**Q: "What is token bucket rate limiting and how did you implement it with Redis?"**
> Token bucket: bucket holds N tokens, refilled at rate R/second. Each request consumes 1 token. If empty → 429.
> Redis implementation: `INCR key` + `EXPIRE key 1s` atomically (Lua script) or using Redis sorted sets for sliding window variant.
> Achieved 1,200+ req/s throughput.

**Q: "Why did you choose Eureka for service discovery over Kubernetes-native DNS?"**
> Eureka gives client-side load balancing (Ribbon/Spring Cloud LoadBalancer) and health-check registration. K8s DNS is simpler but doesn't give you fine-grained metadata about service instances. In a Spring Cloud ecosystem, Eureka integrates tightly with Feign clients.

---

## 10. JD-Specific Tech Stack Questions

### Databricks & Spark *(Nice to Have — Study Basics)*

**Topics:**
- Spark RDD vs DataFrame vs Dataset
- Transformations (lazy) vs Actions (eager)
- Spark partitioning, shuffling
- Delta Lake: ACID transactions on data lakes, time travel, schema evolution
- Apache Iceberg: table format, hidden partitioning, snapshot isolation
- Databricks notebooks, clusters, jobs

**Quick Q&A:**
- What is Delta Lake? How does it differ from a regular Parquet-based data lake?
  > Delta Lake adds ACID transactions, versioned snapshots ("time travel"), and schema enforcement on top of Parquet files stored in object storage (S3/ADLS).
- What is partition pruning in Spark?
  > When Spark can skip reading entire partitions of data based on query predicates — massively reduces I/O. Requires data to be partitioned on the filter column.
- What is shuffle in Spark? Why is it expensive?
  > Shuffle = redistributing data across executors (network + disk I/O). Expensive because it requires serialization, network transfer, and re-partitioning. Triggered by `groupBy`, `join`, `distinct`.

---

### AWS Cloud Native *(Exposure Expected)*

**Topics:** S3, EC2, EKS, ECS, SQS, Lambda basics, IAM roles, VPC

**Expected Questions:**
- What is the difference between Amazon EKS and ECS?
  > EKS = managed Kubernetes. ECS = AWS-native container orchestration (simpler, less portable). EKS gives full K8s API; ECS is proprietary.
- What is Amazon SQS? How does it compare to Kafka?
  > SQS = managed message queue, at-least-once delivery, no consumer groups, messages deleted on consume. Kafka = distributed log, consumer groups, message replay possible, better for high-throughput pipelines.
- What is S3 and how is it used in data platforms?
  > Object storage. Used as the backing store for data lakes (Delta Lake/Iceberg store Parquet files on S3).

---

### Observability Stack *(You Have This!)*

**Topics:** Prometheus + Micrometer, Grafana, OpenTelemetry, Zipkin, Datadog

**Expected Questions:**
- What is distributed tracing? How does OpenTelemetry work?
  > Each service propagates a `traceId` and `spanId` in headers. Each span represents a unit of work. Zipkin/Jaeger collect and visualize trace trees.
- What is the difference between metrics and traces?
  > Metrics = aggregated numerical data over time (latency p99, error rate). Traces = individual request journeys across services.
- What are p50/p95/p99 latency percentiles?
  > p99 = 99% of requests complete within this latency. Critical for SLA definitions. Your Micrometer AI latency metrics (CodeForgeUI) demonstrate this!

---

## 11. Quick Revision Checklist

### Before Monday ✅

**DSA (Priority)**
- [ ] NeetCode 75 — scan all patterns, solve weak areas
- [ ] LRU Cache (LeetCode 146) — HashMap + DoublyLinkedList
- [ ] Course Schedule (topo sort) — LeetCode 207
- [ ] Merge K Sorted Lists — LeetCode 23
- [ ] Minimum Window Substring — LeetCode 76
- [ ] LIS (Longest Increasing Subsequence) — LeetCode 300

**Java**
- [ ] Streams API — `groupingBy`, `flatMap`, `collect`
- [ ] `CompletableFuture` chaining
- [ ] `ConcurrentHashMap` vs `HashMap`
- [ ] Java 21 virtual threads concept

**Spring Boot**
- [ ] JPA Specifications (own it from your resume)
- [ ] `@Transactional` propagation types
- [ ] N+1 problem + solutions
- [ ] Spring Cloud Gateway filter chain

**Kafka**
- [ ] Exactly-once semantics
- [ ] Consumer group rebalancing
- [ ] Saga pattern (choreography vs orchestration)

**System Design**
- [ ] Kafka → Elasticsearch pipeline design
- [ ] Rate limiter design (token bucket)
- [ ] Kubernetes job execution framework (your own!)

**Behavioral (Practice Out Loud)**
- [ ] "Tell me about yourself" — 2 min pitch
- [ ] "Why Arctic Wolf" — data platform mission
- [ ] 2x STAR stories ready (latency fix + workflow orchestration)

---

## 🎯 Final Tip

> Arctic Wolf's Data Platform is a **big data + cybersecurity** intersection. In every technical answer, connect your experience to **scale, reliability, and observability**. You have real production scale (5M+ events/day). Use numbers. They matter.

---

*Last updated: October 2026 | Good luck! 🐺*
