# 🧱 **Proxy Pattern**

---

### 📖 **Intent:**

Provide a **placeholder (surrogate)** or substitute for another object to **control access** to it.

---

## 🎯 Why Use Proxy?

1. To **delay object creation** (lazy loading).
2. To add **security checks or validation**.
3. To **log** access or calls.
4. To **control access** to remote or resource-intensive objects.
5. To add caching, smart access, and protection.

---

## 💡 Real-World Analogy

### 🔐 **Security Guard (Proxy) for a Vault:**

You can’t access the vault directly. The **guard controls who can enter**, **logs access**, and **calls the real vault** only if the visitor is authorized. The **vault is the real object**, and the **guard is the proxy**.

---

## 👨‍💻 Java Code Example – Virtual Proxy (Lazy Loading)

Let’s simulate a system where loading a **Heavy Report** is expensive. You want to defer its creation **until it’s really needed**.

---

### ✅ Step 1: Create the Interface

```java
public interface Report {
    void display();
}
```

---

### ✅ Step 2: Real Subject (Expensive to Create)

```java
public class RealReport implements Report {
    public RealReport() {
        loadFromDisk();  // Simulate expensive operation
    }

    private void loadFromDisk() {
        System.out.println("Loading report from disk...");
    }

    @Override
    public void display() {
        System.out.println("Displaying the full report");
    }
}
```

---

### ✅ Step 3: Proxy Class

```java
public class ReportProxy implements Report {
    private RealReport realReport;

    @Override
    public void display() {
        if (realReport == null) {
            realReport = new RealReport();  // Lazy initialization
        }
        realReport.display();
    }
}
```

---

### ✅ Step 4: Client Code

```java
public class Main {
    public static void main(String[] args) {
        Report report = new ReportProxy();

        System.out.println("Proxy created.");
        // Real report is not yet loaded

        System.out.println("User wants to view the report...");
        report.display(); // Triggers real object creation

        System.out.println("User views again...");
        report.display(); // Uses cached object
    }
}
```

---

### 🧾 Output:

```
Proxy created.
User wants to view the report...
Loading report from disk...
Displaying the full report
User views again...
Displaying the full report
```

---

## 🔄 Types of Proxy Patterns

| Type                         | Purpose                                                       | Example                            |
| ---------------------------- | ------------------------------------------------------------- | ---------------------------------- |
| **Virtual Proxy**            | Lazy loading, deferring object creation                       | Load images or reports on-demand   |
| **Protection Proxy**         | Controls access (e.g., based on user roles)                   | Admin vs. user access to resources |
| **Remote Proxy**             | Represents an object in a different address space (network)   | Java RMI, API gateways             |
| **Caching Proxy**            | Returns cached data instead of calling real object every time | CDN, memoization                   |
| **Logging/Monitoring Proxy** | Adds logging before/after calling real object                 | Audit trails                       |

---

## 🔐 Example: **Protection Proxy – Secure File Access**

```java
public interface FileAccess {
    void openFile();
}

public class RealFileAccess implements FileAccess {
    private String filename;

    public RealFileAccess(String filename) {
        this.filename = filename;
    }

    @Override
    public void openFile() {
        System.out.println("Opening file: " + filename);
    }
}
```

### Proxy to restrict based on user role:

```java
public class FileAccessProxy implements FileAccess {
    private RealFileAccess fileAccess;
    private String username;
    private String role;

    public FileAccessProxy(String username, String role, String filename) {
        this.username = username;
        this.role = role;
        this.fileAccess = new RealFileAccess(filename);
    }

    @Override
    public void openFile() {
        if ("ADMIN".equals(role)) {
            fileAccess.openFile();
        } else {
            System.out.println("Access denied for user: " + username);
        }
    }
}
```

### Client:

```java
public class Main {
    public static void main(String[] args) {
        FileAccess userAccess = new FileAccessProxy("john", "USER", "secret.txt");
        userAccess.openFile(); // Denied

        FileAccess adminAccess = new FileAccessProxy("alice", "ADMIN", "secret.txt");
        adminAccess.openFile(); // Allowed
    }
}
```

---

## ✅ Summary

| Pattern     | Role Played                        |
| ----------- | ---------------------------------- |
| Interface   | Common access                      |
| Real Object | Actual object with heavy logic     |
| Proxy       | Controlled access + extra behavior |

---

## Key Benefits

* **Flexibility**: You can add new features to the real object without modifying the proxy.
* **Separation of Concerns**: The proxy controls access, while the real object provides the heavy logic.
* **Dynamic Behavior**: You can add new features to the proxy without modifying the real object.    
* **Performance**: The proxy can control access and caching, reducing overhead.
* **Lazy Initialization**: You can defer object creation until it's needed, reducing memory usage.
* **Access Control**: You can control access to the real object based on user roles or permissions.
* **Additional Behavior**: You can add logging, monitoring, or other features to the proxy.
* **Security**: You can add security checks to the proxy, ensuring that only authorized users can access the real object.