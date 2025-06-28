# 📄 Problem: **Document Export System**

### 📘 Scenario:

You are building a **document exporter** that converts content into multiple formats like:

* **PDF**
* **DOCX**
* **CSV**

Each export process shares a common workflow:

1. Open the file.
2. Format the content (this differs for each format).
3. Save the file.
4. Optionally, compress the file.

You are to enforce the above flow using the **Template Method Pattern**, while allowing customization in:

* **Formatting**
* **Compression (hook method)**

---

## 🧱 Requirements

### Abstract Base: `DocumentExporter`

* `export(String content)` – the **template method**
* Concrete methods:

    * `openFile()`
    * `saveFile()`
* Abstract method:

    * `formatContent(String content)`
* Hook method (optional):

    * `shouldCompress()` (default: false)
    * `compressFile()` (only called if hook returns true)

---

## 🧑‍💻 Example Output

For PDF:

```
Opening PDF file...
Formatting content as PDF...
Saving file...
```

For CSV (with compression):

```
Opening CSV file...
Formatting content as CSV...
Saving file...
Compressing CSV file...
```

---