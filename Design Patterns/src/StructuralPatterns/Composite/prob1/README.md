# 💼 Problem: **Organization Hierarchy System**

---

### 🧩 Problem Statement:

You're building an **Organization Hierarchy Viewer** for a company.

The organization has:

* Individual employees (developers, designers, etc.)
* Managers who manage **both employees and other managers** (nested)

You need to design a system where:

1. Every **Employee and Manager** implements a common interface.
2. You can **print the entire hierarchy** with indentation.
3. Managers can add or remove employees/managers they manage.

---

## ✅ Requirements:

### Interface: `EmployeeComponent`

```java
public interface EmployeeComponent {
    void showDetails(String indent);
}
```

---

### Concrete Leaf: `Employee`

```java
public class Employee implements EmployeeComponent {
    private String name;
    private String role;

    public Employee(String name, String role) {
        this.name = name;
        this.role = role;
    }

    @Override
    public void showDetails(String indent) {
        System.out.println(indent + "- " + role + ": " + name);
    }
}
```

---

### Composite: `Manager`

```java
import java.util.ArrayList;
import java.util.List;

public class Manager implements EmployeeComponent {
    private String name;
    private String role;
    private List<EmployeeComponent> team = new ArrayList<>();

    public Manager(String name, String role) {
        this.name = name;
        this.role = role;
    }

    public void add(EmployeeComponent member) {
        team.add(member);
    }

    public void remove(EmployeeComponent member) {
        team.remove(member);
    }

    @Override
    public void showDetails(String indent) {
        System.out.println(indent + "+ " + role + ": " + name);
        for (EmployeeComponent member : team) {
            member.showDetails(indent + "  ");
        }
    }
}
```

---

### ✅ Client Code

```java
public class Main {
    public static void main(String[] args) {
        // Leaf employees
        EmployeeComponent dev1 = new Employee("Alice", "Developer");
        EmployeeComponent dev2 = new Employee("Bob", "Developer");
        EmployeeComponent designer = new Employee("Charlie", "Designer");

        // Manager 1
        Manager teamLead = new Manager("Eve", "Team Lead");
        teamLead.add(dev1);
        teamLead.add(dev2);

        // Manager 2
        Manager designHead = new Manager("Grace", "Design Head");
        designHead.add(designer);

        // Top-level Manager (CTO)
        Manager cto = new Manager("Mallory", "CTO");
        cto.add(teamLead);
        cto.add(designHead);

        // Show full organization hierarchy
        cto.showDetails("");
    }
}
```

---

### ✅ Expected Output

```
+ CTO: Mallory
  + Team Lead: Eve
    - Developer: Alice
    - Developer: Bob
  + Design Head: Grace
    - Designer: Charlie
```

