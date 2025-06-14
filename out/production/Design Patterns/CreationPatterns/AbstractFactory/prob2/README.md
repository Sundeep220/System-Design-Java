# ☁️ Abstract Factory Pattern Assignment 2

## 🎯 Problem: Cloud Service Provisioning System

You are building a **cloud provisioning engine** that works with multiple cloud providers — AWS and Azure.

Each cloud provider offers services like:

- **Compute Service** (EC2 for AWS, VM for Azure)
- **Storage Service** (S3 for AWS, Blob Storage for Azure)

You need to design this system using the **Abstract Factory Pattern** so that client code can request compute and storage services **without knowing which cloud provider is being used**.

---

## 📦 Requirements

### 1. Define Service Interfaces:

```java
public interface ComputeService {
    void startInstance();
}

public interface StorageService {
    void storeFile(String filename);
}
```

---

### 2. Implement Concrete Services:

#### For AWS:
- `AWSComputeService implements ComputeService` → `startInstance()` → "Starting EC2 instance on AWS"
- `AWSStorageService implements StorageService` → `storeFile()` → "Storing file in AWS S3"

#### For Azure:
- `AzureComputeService implements ComputeService` → `startInstance()` → "Starting VM on Azure"
- `AzureStorageService implements StorageService` → `storeFile()` → "Storing file in Azure Blob Storage"

---

### 3. Create CloudServiceFactory Interface:

```java
public interface CloudServiceFactory {
    ComputeService createComputeService();
    StorageService createStorageService();
}
```

---

### 4. Implement Concrete Factories:

- `AWSCloudServiceFactory implements CloudServiceFactory`
- `AzureCloudServiceFactory implements CloudServiceFactory`

Each factory returns the correct provider-specific compute and storage objects.

---

### 5. Create a ProvisioningClient class

- Takes `CloudServiceFactory` in its constructor
- Has a method `provision()`:
    - Starts a compute instance
    - Stores a file named `"log.txt"`

---

## 🧪 Testing

In your `Main` class:

1. Instantiate `AWSCloudServiceFactory` and `AzureCloudServiceFactory`
2. Pass them to `ProvisioningClient` and call `provision()`

---

## ✅ Sample Output

```
Starting EC2 instance on AWS
Storing file in AWS S3

Starting VM on Azure
Storing file in Azure Blob Storage
```

---

## 🧠 Bonus (Optional)

- Add GCP support with `GCPComputeService`, `GCPStorageService`, and `GCPCloudServiceFactory`
- Use an `enum CloudProvider { AWS, AZURE, GCP }` + `CloudFactoryProvider.getFactory(...)`
