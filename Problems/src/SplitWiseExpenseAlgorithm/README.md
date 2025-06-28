# Problem Understanding

We are designing the **core logic behind Splitwise**, specifically the **Simplify Debt Algorithm** used to minimize the number of transactions between people after group expenses.

### Example:

```
Input:
- A paid B ₹100
- B paid C ₹50
- C paid A ₹30

Goal:
- Determine the minimum number of transactions to settle all debts.
```

---

## ✅ 2. Requirements Gathering

### 📌 Functional Requirements:

* Track users and the net amount each user owes or is owed.
* Minimize the number of transactions to settle debts (simplify the debt graph).
* Support both positive and negative balances (creditors and debtors).
* Return a list of transactions to settle balances.

### ⚙️ Non-Functional Requirements:

* Efficient algorithm (optimize for large number of users).
* Accurate floating point handling.
* Easily extendable for real Splitwise features (groups, categories, partial payments, etc.).

---

## ✅ 3. Entities and Relationships

| Entity             | Description                                                                |
| ------------------ | -------------------------------------------------------------------------- |
| **User**           | Represents a person involved in expense sharing.                           |
| **Transaction**    | Who pays whom and how much.                                                |
| **Expense**        | Original expenses logged, used to calculate net balances.                  |
| **DebtSimplifier** | Core algorithm class that takes balances and outputs minimal transactions. |

---

## ✅ 4. Class Diagram (Textual UML)

```
+---------------------+
|       User          |
+---------------------+
| - id: String        |
| - name: String      |
+---------------------+

+---------------------+
|    ExpenseRecord    |
+---------------------+
| - paidBy: User      |
| - paidFor: User     |
| - amount: double    |
+---------------------+

+--------------------------+
|    TransactionSummary    |
+--------------------------+
| - from: User             |
| - to: User               |
| - amount: double         |
+--------------------------+

+--------------------------+
|     DebtSimplifier       |
+--------------------------+
| + simplify(List<Expense>)|
+--------------------------+
```

---

