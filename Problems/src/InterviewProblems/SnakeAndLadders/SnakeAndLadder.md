# LLD Practice: Snake and Ladder

## Problem Statement

Design a multiplayer Snake and Ladder game with the following requirements:

- The game is played on a board with numbered cells, typically 100.
- The board has predefined snakes and ladders connecting certain cells.
- The game supports multiple players, each with a unique game piece.
- Players take turns rolling a dice to determine how many cells to move forward.
- Landing on a snake head slides the player down to the snake tail.
- Landing on a ladder base climbs the player up to the ladder top.
- The game continues until a player reaches the final cell.
- The system handles multiple game sessions concurrently, with each group of players playing independently.

## Interview Flow

1. Clarify requirements and scope
2. Identify core entities and relationships
3. Define classes, attributes, and methods
4. Model the core flows (turn, move, snake/ladder resolution, win)
5. Apply design patterns and plan for extensibility
6. Handle concurrency across sessions
7. Write code and cover edge cases

---

## Step 1: Clarify Requirements and Scope

**Status:** Done

**Clue given:** Interviewers leave gaps on purpose. Ask questions and state assumptions before designing anything.

### Candidate Assumptions vs. Interviewer Decisions

| Topic | Candidate Assumption | Decision |
|---|---|---|
| Board size | 100 by default, but configurable (NxN) | Board size is configurable, default 100. Cells are numbered 1 to N. Players start off-board at cell 0. |
| Dice | Single six-sided die, extensible to multiple dice | Single die behind a `Dice` abstraction so multiple dice (or different face counts) can be plugged in later. |
| Exact finish | Unsure | **Recommended:** the player must roll the exact number to land on the final cell. Overshooting leaves the player in place. State this explicitly. Either rule is acceptable if the choice is justified and documented. |
| Extra turn on 6 | One turn per roll, extra turn optional | Not part of the classic rules and not in the spec, so it is out of core scope. Model turn handling so a bonus-turn rule can be added later without changing the core flow. |
| Players | Min and max configurable | Yes, configurable minimum and maximum player count. |
| Shared cells | Configurable | Multiple players can occupy the same cell. There is no capture, which matches the classic rules. |
| Board validation | Reject invalid setups | Yes. Reject: snake or ladder endpoints outside `1..N`; a snake head on the final cell; two snakes sharing a head; two ladders sharing a base; a snake and ladder sharing the same start cell. Additionally, a snake's tail must be below its head, and a ladder's top must be above its base. Cell 0 is not a valid board cell. |
| Winner | Single winner | Yes. The game ends when the first player reaches the final cell. |
| Interface | Console app | A console app is fine for the demo, but the game engine must not depend on I/O. Keep input and output behind an interface so the logic is testable and reusable. |
| Sessions | Separate `SnakeAndLadderGame` instance per session, coordinated by a manager | Yes. Each session gets its own `SnakeAndLadderGame` instance that owns its state. A `GameManager` registers and looks up sessions. Sessions share no mutable state, so they run independently. |

### Key Takeaways

- Stating assumptions explicitly is part of the evaluation.
- Board validation should happen at setup time, so invalid states never reach gameplay.
- Keep the core engine free of I/O, and keep each session's state isolated.

---

## Step 2: Identify Core Entities and Relationships

**Status:** Done

**Clue given:** Look at the nouns in the requirements. Decide which hold state and behavior and which are just values. Then decide who owns whom.

### Candidate Entities

| Entity | Type | Responsibility |
|---|---|---|
| `Snake` | Class (extends `BoardEntity`) | Head and tail. Validates tail < head. |
| `Ladder` | Class (extends `BoardEntity`) | Base and top. Validates top > base. |
| `Player` | Class | Name. Position is owned by `Board`, so no separate piece class or position field is needed. |
| `Dice` | Interface | `roll()`. `StandardDice` implements it. Enables multiple dice later. |
| `Board` | Class | Size N, plus a lookup from cell to jump. Validates the setup. |
| `GameManager` | Class | Creates, registers, and looks up sessions. Uses a concurrent map keyed by game ID. |

### Missing Entities (Added in Review)

| Entity | Type | Why it is needed |
|---|---|---|
| `SnakeAndLadderGame` (originally `Game`) | Class | One session. Owns the board, dice, players, turn index, status, and winner. Without it, `GameManager` has nothing to manage. |
| `GameStatus` | Enum | `NOT_STARTED`, `IN_PROGRESS`, `FINISHED`. Makes the lifecycle explicit. |
| `BoardEntity` (originally `Jump`) | Abstract class | Shared start and end fields for `Snake` and `Ladder`. Lets `Board` store both in one map. |

### Relationships

- `GameManager` 1 to many `SnakeAndLadderGame`
- `SnakeAndLadderGame` 1 to 1 `Board`, 1 to 1 `Dice`, 1 to many `Player` (2 to N players)
- `Board` 1 to many `BoardEntity`, where each `BoardEntity` is a `Snake` or a `Ladder`
- `SnakeAndLadderGame` uses `Board` and `Dice` but does not create them; they are injected at setup
- `Player` holds only its name; `Board` owns positions; `SnakeAndLadderGame` owns movement logic

### Points to Discuss

- **Snake and Ladder as separate classes vs. one `BoardEntity` with a type enum:** Both work. Subclasses put validation in the right place and make later rule changes easier. A single class with an enum is simpler if the interviewer wants less code.
- **Cell as a class:** Not needed. A cell is an integer index. Model it as a class only if cells gain behavior or state.
- **Who validates the board:** `Board` should validate at construction, so invalid states cannot exist.

### Key Takeaways

- Entities should hold state or behavior. Plain values, like cell numbers, should stay primitives.
- Missing an orchestrator entity (`SnakeAndLadderGame`) is a common gap. Session state needs a home that is separate from the manager.

---

## Step 3: Define Classes, Attributes, and Methods

**Status:** Done (reference answer below)

**Clue:** For each class, ask what data it must hold and what operations other classes need from it. Keep `SnakeAndLadderGame` as the only class that changes player positions. Ask yourself which methods belong to `Board` (for example, resolving a landing cell) and which belong to `SnakeAndLadderGame` (for example, playing a turn).

**Clue: walk through one turn in order and assign each step to a class:**

1. Current player's turn starts
2. Roll the dice
3. Compute the tentative cell (current position + roll)
4. If the tentative cell exceeds N, the player stays put (exact finish rule)
5. Otherwise, move the player to the tentative cell
6. If a snake or ladder starts at that cell, move the player to its end
7. If the player's cell is N, mark the game finished and record the winner
8. Advance the turn to the next player

Questions to answer:

- Which steps are data lookups (for example, "is there a jump at cell X?") and which are orchestration?
- What must `Board` store so it can answer the lookup?
- What state must `Game` hold to run the turn loop?
- What must `Game` receive at construction?

### Reference Answer

**Board**

- Attributes: `size: int`, `jumps: Map<Integer, BoardEntity>` (keyed by start cell), `playerPositions: Map<Player, Integer>`
- Methods:

```java
Board(int size, List<Snake> snakes, List<Ladder> ladders, List<Player> players)   // validates the setup and places players at 0
int getSize()
int getNextPosition(int current, int steps)  // returns current on overshoot, otherwise applies any snake or ladder
int getPlayerPosition(Player player)
void setPlayerPosition(Player player, int position)
```

- The spec's `addSnake` and `addLadder` are omitted. Adding jumps after construction would bypass validation.

**BoardEntity** (abstract)

- Attributes: `start: int`, `end: int`
- Methods:

```java
int getStart()
int getEnd()
```

- `Snake(int head, int tail)` validates `tail < head`. `Ladder(int base, int top)` validates `top > base`.

**Player**

- Attributes: `name: String`. Position is owned by `Board`, so `Player` has no position field.
- Methods:

```java
Player(String name)
String getName()
```

**Dice** (interface)

- Methods:

```java
int roll()
```

- `SingleDice` implements `roll()` and returns 1 to 6.

**Game**

- Attributes: `board: Board`, `players: List<Player>`, `dice: Dice`, `currentIndex: int`, `status: GameStatus`, `winner: Player`
- Methods:

```java
Game(Board board, List<Player> players, Dice dice)
void start()                        // validates at least 2 players, sets currentIndex to 0 and status to IN_PROGRESS
void playTurn()                     // runs walkthrough steps 2 to 8
private void movePlayer(Player player, int steps)
private boolean checkWin(Player player)
Player getCurrentPlayer()
Player getWinner()
GameStatus getStatus()
```

**GameStatus** (enum)

```java
NOT_STARTED, IN_PROGRESS, FINISHED
```

**GameManager**

- Attributes: `games: Map<String, Game>` (a `ConcurrentHashMap`)
- Methods:

```java
String createGame(Board board, List<Player> players, Dice dice)   // generates the session ID, creates the game, and registers it
Game getGame(String id)
void removeGame(String id)
```

### Why the Split Works

- **Steps 3 and 6 (snake or ladder):** `Board.getNextPosition` owns them, since they are facts about the board.
- **Step 4 (exact finish):** `Board.getNextPosition` owns it in this design. It's a trade-off, stated in Step 4.
- **Steps 2, 5, 7, 8:** `Game.playTurn`, `movePlayer`, and `checkWin` own them, since they change game state.
- **Board construction:** The `Board` constructor validates the setup, so invalid states cannot exist.
- **Validation of duplicates:** Using `Map<Integer, BoardEntity>` keyed by start cell means a second jump on the same start cell is detected at construction.
- **Session management:** `GameManager` owns the session registry. Each `Game` keeps its own state, so sessions do not interfere with each other.

### Key Takeaways

- Put each rule in the class that owns the data it needs.
- Keep public mutators to a minimum. Only `Game` should move players.

---

## Step 4: Model the Core Flows

**Status:** Done (reference answer below)

**Final design summary:**

- `Game` is the session orchestrator.
- `Snake` and `Ladder` extend `BoardEntity` (abstract). `Board` stores both in `Map<Integer, BoardEntity>` keyed by start cell.
- Positions live in `Board` (`Map<Player, Integer>`). `Player` holds only the name.
- `GameManager` and `GameStatus` are added beyond the spec's entity list. `GameManager` is needed for concurrent sessions. `GameStatus` replaces a raw boolean.

**Open issues to discuss:**

- **Duplicate position state (resolved):** `Board` owns positions, and `Player` keeps only the name.
- **Winner is not stored (resolved):** `Game` has a `winner` field.
- **Exact finish rule location:** `Board.getNextPosition` must return the current cell on overshoot. That puts a rule inside `Board`, which is acceptable for this design, but it should be stated.

### Class Structure Update

**`BoardEntity` (abstract)**

- Fields: `int start`, `int end`
- Methods: `int getStart()`, `int getEnd()`
- Shared base for cell-to-cell jumps. Only this class is abstract.

**`Snake extends BoardEntity`**

- `Snake(int head, int tail)` validates `tail < head`. Concrete class, so `new Snake(14, 7)` works.

**`Ladder extends BoardEntity`**

- `Ladder(int base, int top)` validates `top > base`. Concrete class.

**`Board`**

- `Board(int size, List<Snake> snakes, List<Ladder> ladders, List<Player> players)` keeps the spec's constructor.
- Stores `Map<Integer, BoardEntity> jumps` keyed by start cell, so `getNextPosition` needs one lookup.
- Duplicate starts (two snakes on one head, or a snake and ladder on one cell) are rejected when the map is built.

**Power-ups (future)**

- Not a `BoardEntity`. Power-ups usually affect the player, not a cell-to-cell move (for example, a shield or a double roll).
- Model them as a separate `PowerUp` interface with `apply(Player, Game)`.

**Trade-off**

- An abstract class fits because `Snake` and `Ladder` share fields and constructor validation. An interface would fit if only behavior were shared.

### Reference Flow

**`Game.playTurn()`**

1. If `status` is `FINISHED`, stop.
2. Get the current player.
3. Roll the dice: `int roll = dice.roll()`.
4. Call `movePlayer(player, roll)`.
5. If `checkWin(player)`, set `status` to `FINISHED`, record the winner, and return.
6. Advance `currentIndex` to the next player: `currentIndex = (currentIndex + 1) % players.size()`.

**`Game.movePlayer(player, steps)`**

1. `current = board.getPlayerPosition(player)`
2. `next = board.getNextPosition(current, steps)`, which handles overshoot and snakes or ladders
3. `board.setPlayerPosition(player, next)`

**`Game.checkWin(player)`**

- Return `board.getPlayerPosition(player) == board.getSize()`.

**`Board.getNextPosition(current, steps)`**

1. `target = current + steps`
2. If `target > size`, return `current`
3. If a snake or ladder starts at `target`, return its end
4. Otherwise, return `target`

### Key Takeaways

- Each step in the flow is a single responsibility. `Game.playTurn` orchestrates, `movePlayer` moves, and `Board` answers questions about the board.
- Resolve snakes and ladders in one place (`getNextPosition`) so no caller can skip them.
- Round-robin turns are a single index with modulo, so they are easy to extend.

---

## Step 5: Design Patterns and Extensibility

**Status:** Done (reference answer below)

**Extensions to handle:**
1. Multiple dice
2. Custom board rules (overshoot, chaining, teleport)
3. Game session creation with preset board configurations

### Reference Answer

**1. Multiple Dice → Strategy Pattern (already in place)**

`Dice` is already an interface. `StandardDice` returns 1–6. To support two dice, add a `MultipleDice` class that holds a `List<Dice>` and sums their rolls.

```java
interface Dice {
    int roll();
}

class StandardDice implements Dice {
    public int roll() { return new Random().nextInt(6) + 1; }
}

class MultipleDice implements Dice {
    private final List<Dice> dice;
    public MultipleDice(List<Dice> dice) { this.dice = dice; }
    public int roll() { return dice.stream().mapToInt(Dice::roll).sum(); }
}
```

No existing code changes. `SnakeAndLadderGame` only calls `dice.roll()`, so it works with any implementation. This is the **Strategy** pattern — the algorithm (rolling) is swappable behind an interface.

**2. Custom Board Rules → Strategy Pattern on `MoveStrategy`**

Right now the exact-finish rule and snake-chaining logic live inside `Board.getNextPosition`. If different game modes need different rules, extract the rule into a strategy.

```java
interface MoveStrategy {
    int resolve(int current, int steps, int boardSize, Map<Integer, BoardEntity> jumps);
}

class ClassicMoveStrategy implements MoveStrategy { ... }   // overshoot stays, single jump
class ChainedMoveStrategy implements MoveStrategy { ... }   // keep resolving jumps until stable
```

`Board` is constructed with a `MoveStrategy`, and `getNextPosition` delegates to it. Existing code doesn't change.

**3. Preset Board Configurations → Builder Pattern**

`GameManager.createGame` currently takes a fully built `Board`. To support named presets ("classic", "kids"), use a `BoardBuilder`.

```java
class BoardBuilder {
    private int size = 100;
    private List<Snake> snakes = new ArrayList<>();
    private List<Ladder> ladders = new ArrayList<>();
    private List<Player> players = new ArrayList<>();

    public BoardBuilder size(int size) { this.size = size; return this; }
    public BoardBuilder addSnake(int head, int tail) { snakes.add(new Snake(head, tail)); return this; }
    public BoardBuilder addLadder(int base, int top) { ladders.add(new Ladder(base, top)); return this; }
    public BoardBuilder addPlayer(String name) { players.add(new Player(name)); return this; }
    public Board build() { return new Board(size, snakes, ladders, players); }
}
```

A factory method or static preset can wrap this:

```java
Board classic = BoardBuilder.classic().addPlayer("Alice").addPlayer("Bob").build();
```

**Summary Table**

| Extension | Pattern | Reason |
|---|---|---|
| Multiple dice | Strategy (`Dice` interface) | Swap the roll algorithm without changing the game |
| Custom move rules | Strategy (`MoveStrategy` interface) | Swap overshoot or chaining rules per game mode |
| Preset board configs | Builder (`BoardBuilder`) | Build complex objects step by step, avoid a large constructor |
| Power-ups | Interface (`PowerUp`) with `apply(Player, SnakeAndLadderGame)` | Each power-up is an independent behaviour, no shared state |

### Key Takeaways

- Introducing a new `Dice` implementation or a new board rule requires zero changes to `SnakeAndLadderGame`. This satisfies the Open/Closed Principle.
- The `Builder` pattern removes the need to pass long constructor argument lists and makes preset configurations readable.
- Name the pattern explicitly in the interview, even briefly. "I'd use Strategy here so the rule is swappable" is enough.

---

## Step 6: Concurrency Across Sessions

**Status:** Pending

**Clue:** `GameManager` uses a `ConcurrentHashMap` to register sessions. That handles concurrent session creation safely. But what about two threads playing turns in the same session simultaneously? Think about which field in `SnakeAndLadderGame` is the dangerous one, and what the lightest-weight fix is.

### Reference Answer

**The risk: two threads calling `playTurn()` simultaneously on the same session**

In practice this shouldn't happen — a turn-based game has one active player at a time — but the interviewer will ask if you've thought about it.

The dangerous fields in `SnakeAndLadderGame` are:
- `currentIndex` — two threads could both read the same index, both play, and then both increment it, skipping a player.
- `status` — one thread might check `FINISHED` while another is still inside `playTurn`.
- `winner` — could be written by two threads at once if both reach the win check.

The dangerous field in `Board` is:
- `playerPositions` (`HashMap<Player, Integer>`) — `HashMap` is not thread-safe. Two threads writing positions concurrently can corrupt the map.

**The lightest-weight fix: `synchronized` on `playTurn`**

```java
public synchronized void playTurn() {
    if (status == GameStatus.FINISHED) return;
    // ... rest of the method
}
```

`synchronized` on the method uses `this` as the monitor. Only one thread can be inside `playTurn` at a time per session. Since each session is a separate `SnakeAndLadderGame` instance, sessions don't block each other.

Also change `Board.playerPositions` from `HashMap` to `ConcurrentHashMap` as a safety net:

```java
private final Map<Player, Integer> playerPositions = new ConcurrentHashMap<>();
```

**Why not `ReentrantLock`?**

`synchronized` is sufficient here. `ReentrantLock` adds value when you need `tryLock`, timeout, or fairness. For a simple turn-based game, `synchronized` is the right trade-off and easier to explain in an interview.

**`GameManager` concurrency**

`ConcurrentHashMap` on `games` already handles concurrent session creation and lookup safely. No additional locking is needed there.

### Key Takeaways

- Each session is isolated, so locking is only needed within a single `SnakeAndLadderGame`.
- `synchronized` on `playTurn` is the minimal correct fix.
- Replace `HashMap` with `ConcurrentHashMap` for `playerPositions` as a defensive measure.
- State the trade-off: `synchronized` vs `ReentrantLock`. Always justify your choice.

---

## Step 7: Write the Code

**Status:** Done (reference answer below)

**Classes written in dependency order:**
1. `GameStatus` enum
2. `BoardEntity` abstract class
3. `Snake` and `Ladder`
4. `Dice` interface and `SingleDice`
5. `Player` (Java record)
6. `Board`
7. `Game`
8. `GameManager`
9. `Main` (demo)

### Key Implementation Notes

- `Player` is a Java `record` — gives `equals()`, `hashCode()`, and `name()` for free. Needed for use as a `Map` key in `Board.playerPositions`.
- `BoardEntity.getStart()` and `getEnd()` are concrete (not abstract) since every subclass returns the same inherited fields. No duplication in `Snake` or `Ladder`.
- `Board.jumps` uses `HashMap` (write-once at construction). `Board.playerPositions` uses `ConcurrentHashMap` (written during gameplay).
- `Game.playTurn()` is `synchronized`. `Game.movePlayer()` and `Game.checkWin()` are `private`.
- `GameManager` is a singleton, lazily initialized with `synchronized getInstance()`.
- `Main` uses a turn limit of 1000 to prevent infinite loops on degenerate boards.

---

## Step 8: Testing and Trade-off Discussion

**Status:** Done (reference answer below)

### Edge Cases to Test

| Scenario | Expected behaviour |
|---|---|
| Player overshoots the final cell | Position unchanged, turn passes |
| Player lands exactly on final cell | Game status → `FINISHED`, winner recorded |
| Snake at cell 99 | Player drops to snake tail |
| Ladder base at cell 1 | Player jumps to ladder top immediately |
| Two snakes with same head | `IllegalArgumentException` at `Board` construction |
| Snake head on final cell (100) | `IllegalArgumentException` at `Board` construction |
| `playTurn()` called before `start()` | `IllegalStateException` |
| `playTurn()` called after game finished | Returns immediately, no state change |

### Key Unit Tests

**`Board.getNextPosition`**
```java
// Normal move
assert board.getNextPosition(10, 3) == 13;

// Overshoot
assert board.getNextPosition(98, 5) == 98;

// Exact finish
assert board.getNextPosition(97, 3) == 100;

// Snake
assert board.getNextPosition(14, 3) == tail;   // 17 is a snake head

// Ladder
assert board.getNextPosition(0, 1) == top;     // 1 is a ladder base
```

**`Game` lifecycle**
```java
game.playTurn();   // must throw — not started yet
game.start();
// ... play until finished
game.playTurn();   // must be a no-op
```

### Trade-off Q&A

**"Why `synchronized` instead of `ReentrantLock`?"**
> `synchronized` is simpler and sufficient here. `ReentrantLock` adds value when you need `tryLock`, timeout, or fairness policies. For a turn-based game where only one thread should play at a time, `synchronized` communicates intent clearly and has no meaningful performance difference at this scale.

**"Why abstract class instead of interface for `BoardEntity`?"**
> `Snake` and `Ladder` share two fields (`start`, `end`) and the same getter implementations. An interface cannot hold instance fields, so using one would force each subclass to duplicate the fields and getters. Abstract class eliminates that duplication. If we only needed to share behaviour (no shared state), an interface would be the right choice.

**"What would change if you needed to persist game state to a database?"**
> `Game` and `Board` would need to be serializable or mapped to a persistence model (e.g., JPA entities). `GameManager` would query the database instead of an in-memory map. The `Game` class itself wouldn't change — only the storage layer around it. This is why keeping `GameManager` as a separate registry was the right call.

**"How would you scale to 10,000 concurrent sessions?"**
> The current design already isolates sessions — each `Game` instance has no shared mutable state with other sessions. Scaling horizontally means running multiple JVM instances behind a load balancer, with a shared external store (Redis or a database) replacing the in-memory `ConcurrentHashMap` in `GameManager`. The `Game` class logic stays unchanged.

**"What if a player disconnects mid-game?"**
> Add a timeout or `ABANDONED` status to `GameStatus`. `GameManager` can run a background thread that checks last-activity timestamps and marks idle sessions as abandoned. `playTurn()` would check for `ABANDONED` the same way it checks for `FINISHED`.

### Key Takeaways

- State edge cases before writing code in an interview — it shows you think defensively.
- Name the pattern and justify the choice in one sentence. Interviewers value clarity over length.
- Always distinguish between session-level concurrency (handled by isolation) and within-session concurrency (handled by `synchronized`).

---

## Step 9: Extension — Power-ups

**Status:** Done (design only)

### Why `PowerUp` is NOT a `BoardEntity`

`BoardEntity` models a cell-to-cell move (`start` → `end`). Power-ups don't move a player from one cell to another — they affect a player's turn or state (e.g., skip a turn, roll again, freeze another player). Forcing them into `BoardEntity` would break the abstraction.

**Rule:** if the effect is "move player from A to B", use `BoardEntity`. If the effect is anything else, use `PowerUp`.

### Design

```java
interface PowerUp {
    void apply(Player player, Game game);
}
```

- `apply` receives the current `Player` and the `Game` so it can read or modify any game state.
- Each power-up is a separate class — **Strategy pattern** again.

**Example implementations:**

```java
class SkipTurnPowerUp implements PowerUp {
    public void apply(Player player, Game game) {
        // advance the turn index twice — skips this player's next turn
        game.skipNextTurn(player);
    }
}

class ExtraRollPowerUp implements PowerUp {
    public void apply(Player player, Game game) {
        // flag this player to roll again before advancing the turn index
        game.grantExtraRoll(player);
    }
}

class FreezePowerUp implements PowerUp {
    private final Player target;
    public FreezePowerUp(Player target) { this.target = target; }
    public void apply(Player player, Game game) {
        game.freezePlayer(target, 1); // frozen for 1 turn
    }
}
```

### Where Power-ups Live on the Board

Power-ups are placed on cells, just like snakes and ladders. Two options:

**Option A — Separate map in `Board`**
```java
Map<Integer, PowerUp> powerUps;
```
`getNextPosition` stays clean (only handles movement). `Game.playTurn` calls a second lookup after moving:
```java
PowerUp pu = board.getPowerUp(newPosition);
if (pu != null) pu.apply(currentPlayer, this);
```

**Option B — Extend `BoardEntity`** (not recommended)
Force `PowerUp` to have a `start` cell and inherit from `BoardEntity`, overriding `getEnd()` to return `start` (no movement). This works but is semantically wrong — it abuses the abstraction.

→ **Option A is correct.** Keep concerns separate.

### Changes Needed to `Game` and `Board`

| Class | Change |
|---|---|
| `Board` | Add `Map<Integer, PowerUp> powerUps` field and `getPowerUp(int cell)` method |
| `Game.playTurn` | After `movePlayer`, check `board.getPowerUp(newPosition)` and call `apply` if present |
| `Game` | Add `skipNextTurn(Player)`, `grantExtraRoll(Player)`, `freezePlayer(Player, int)` methods to support power-up effects |

### Updated `Game.playTurn` with Power-up Hook

```java
public synchronized void playTurn() {
    if (status == GameStatus.FINISHED) return;
    if (status == GameStatus.NOT_STARTED) throw new IllegalStateException("Call start() first");

    Player current = players.get(currentIndex);

    if (frozenTurns.getOrDefault(current, 0) > 0) {
        frozenTurns.put(current, frozenTurns.get(current) - 1);
        currentIndex = (currentIndex + 1) % players.size();
        return;
    }

    int roll = dice.roll();
    movePlayer(current, roll);

    if (checkWin(current)) {
        status = GameStatus.FINISHED;
        winner = current;
        return;
    }

    PowerUp pu = board.getPowerUp(board.getPlayerPosition(current));
    if (pu != null) pu.apply(current, this);

    if (!extraRoll) {
        currentIndex = (currentIndex + 1) % players.size();
    }
    extraRoll = false;
}
```

### New Fields Needed in `Game`

```java
private final Map<Player, Integer> frozenTurns = new HashMap<>();  // tracks freeze count per player
private boolean extraRoll = false;                                  // set by ExtraRollPowerUp
```

### Power-up Support Methods in `Game`

```java
public void skipNextTurn(Player player) {
    frozenTurns.put(player, frozenTurns.getOrDefault(player, 0) + 1);
}

public void grantExtraRoll(Player player) {
    extraRoll = true;   // checked in playTurn before advancing currentIndex
}

public void freezePlayer(Player player, int turns) {
    frozenTurns.put(player, frozenTurns.getOrDefault(player, 0) + turns);
}
```

### Updated `Board` for Power-ups

```java
private final Map<Integer, PowerUp> powerUps = new HashMap<>();

public void addPowerUp(int cell, PowerUp powerUp) {
    powerUps.put(cell, powerUp);
}

public PowerUp getPowerUp(int cell) {
    return powerUps.get(cell);   // returns null if no power-up at this cell
}
```

### Key Takeaways

- **Strategy pattern:** each power-up is an independent class implementing the same interface. Adding a new power-up requires zero changes to `Game` or `Board`.
- **Open/Closed Principle:** `Game.playTurn` calls `pu.apply(player, this)` — it never needs to know what kind of power-up it is.
- **Separate maps for separate concerns:** `jumps` (movement) and `powerUps` (effects) are distinct lookups in `Board`. Don't conflate them.
