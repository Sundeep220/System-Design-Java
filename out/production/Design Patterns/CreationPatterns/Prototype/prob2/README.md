# 🚀 Prototype Pattern Assignment – Document Template System

## 📘 Problem Statement

You are designing a **Document Management System** for a legal firm that frequently generates different types of documents like **Contracts**, **NDAs**, and **Reports** from master templates.  
Each document contains nested components like **Header**, **Footer**, **Body**, and **MetaData** objects.

Creating these documents from scratch is costly, especially when many documents share a common structure. So you must implement the **Prototype Pattern** to:

- Define base prototypes for each document type
- Clone and customize them on demand
- Ensure deep copy of all nested components

---

## 🎯 Goals

1. Implement the Prototype pattern with complex, nested composition.
2. Ensure deep cloning of nested fields like `Header`, `Body`, `Footer`, `MetaData`.
3. Demonstrate independence of clones by modifying components post-cloning.

---

## 📦 Classes to Design

### Interface: `Document`
```java
- Document clone();
- void printContent();
```

### Concrete Classes:
- `ContractDocument`
- `NDADocument`
- `ReportDocument`

Each document contains:

- `Header` (title, dateCreated)
- `Body` (content)
- `Footer` (author, signature)
- `MetaData` (createdBy, department, version)

---

## 🔁 Clone Requirements

- Implement **deep copy** in the `clone()` method for all classes.
- Clones must be **independent** (i.e., modifying one shouldn't affect others).
- Use **copy constructors** or serialization if needed for deep copying.

---

## 📌 Sample Usage

```java
Document contract = new ContractDocument("Employee Agreement");
Document contractCopy = contract.clone();

contractCopy.getHeader().setTitle("Consulting Agreement");
contractCopy.getBody().setContent("This is a consulting contract...");

contract.printContent();
contractCopy.printContent();
```

---

## ✅ Expected Output

```
=== Original Contract ===
Title: Employee Agreement
Body: Standard employee contract...

=== Cloned Contract ===
Title: Consulting Agreement
Body: This is a consulting contract...
```

---

## 🚨 Bonus Challenges

- Add a `version` field in `MetaData` and update it when a document is cloned.
- Maintain a `DocumentRegistry` that stores and retrieves prototypes by type (`contract`, `nda`, `report`).
- Track cloning logs using a `CloneLogger` utility.
