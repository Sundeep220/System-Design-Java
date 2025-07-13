# **Complete dry run** of the program with this test input:

---

### ✅ Input

```text
Alice paid ₹50 for Bob  
Bob paid ₹20 for Charlie  
Charlie paid ₹30 for Alice
```

---

### Step 1: Setup — Users and Expenses

| Paid By | Paid For | Amount |
| ------- | -------- | ------ |
| Alice   | Bob      | ₹50    |
| Bob     | Charlie  | ₹20    |
| Charlie | Alice    | ₹30    |

So we create three `ExpenseRecord` objects:

```java
[
    ExpenseRecord(Alice, Bob, 50),
    ExpenseRecord(Bob, Charlie, 20),
    ExpenseRecord(Charlie, Alice, 30)
]
```

---

## 🔄 Step 2: Build `netBalances`

We now calculate how much each user has **net paid or owes**, using this logic:

```java
net[paidBy] += amount
net[paidFor] -= amount
```

Let's walk through each transaction:

### 1. Alice paid ₹50 for Bob

* net\[Alice] += 50 → 50
* net\[Bob]   -= 50 → -50

### 2. Bob paid ₹20 for Charlie

* net\[Bob]   += 20 → -30 (since he was -50 before)
* net\[Charlie] -= 20 → -20

### 3. Charlie paid ₹30 for Alice

* net\[Charlie] += 30 → 10 (since he was -20 before)
* net\[Alice]   -= 30 → 20 (since she was 50 before)

---

### 🧮 Final Net Balances:

| User    | Net Balance |
| ------- | ----------- |
| Alice   | +20         |
| Bob     | -30         |
| Charlie | +10         |

💡 Meaning:

* Alice should **get ₹20**
* Charlie should **get ₹10**
* Bob should **pay ₹30**

---

## 🧺 Step 3: Build Min and Max Heaps

* **Debtors Min-Heap (by lowest balance):**

  ```
  Bob → -30
  ```

* **Creditors Max-Heap (by highest balance):**

  ```
  Alice → +20  
  Charlie → +10
  ```

---

## 🔁 Step 4: Greedy Simplification Loop

### 🔁 Iteration 1:

* Pop:

    * Debtor → Bob: -30
    * Creditor → Alice: +20
* Settle ₹20 (minimum of 30 and 20)

✅ Add:

```
Bob pays Alice ₹20
```

* Update balances:

    * Bob → -10
    * Alice → 0

* Push Bob back to debtor heap since he still owes ₹10.

### 🔁 Iteration 2:

* Pop:

    * Debtor → Bob: -10
    * Creditor → Charlie: +10
* Settle ₹10

✅ Add:

```
Bob pays Charlie ₹10
```

* Update balances:

    * Bob → 0
    * Charlie → 0

Heaps now empty → loop ends.

---

## ✅ Final Output

```
Bob pays Alice: ₹20.00  
Bob pays Charlie: ₹10.00
```

---

## 🤔 Why is this "Simplified"?

Instead of 3 expenses and 3 raw transactions:

* We've **compressed the debts** to just **2 direct payments**
* No unnecessary money being passed around (e.g., Alice pays Bob, then Bob pays Charlie, then Charlie pays Alice)

---