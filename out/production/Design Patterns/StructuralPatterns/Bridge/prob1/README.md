# 💼 Bridge Pattern Problem: **Report Generator**

---

### 🧩 Problem Statement:

You’re building a system that generates reports. Reports can vary along **two independent dimensions**:

1. **Report Type** (Abstraction):

    * Summary Report
    * Detailed Report

2. **Output Format** (Implementation):

    * PDF
    * HTML
    * CSV

We want to **avoid creating a separate class for every combination** (e.g., `SummaryPDFReport`, `DetailedCSVReport`, etc.)

---

### 🎯 Objective:

Use the **Bridge Pattern** to decouple `Report` from `ReportFormatter`.

---

### ✅ Client Code

```java
public class Main {
    public static void main(String[] args) {
        ReportFormatter pdf = new PDFReportFormatter();
        ReportFormatter html = new HTMLReportFormatter();
        ReportFormatter csv = new CSVReportFormatter();

        Report summaryPdf = new SummaryReport(pdf);
        Report detailedHtml = new DetailedReport(html);
        Report summaryCsv = new SummaryReport(csv);

        System.out.println("---- Summary PDF ----");
        summaryPdf.display();

        System.out.println("\n---- Detailed HTML ----");
        detailedHtml.display();

        System.out.println("\n---- Summary CSV ----");
        summaryCsv.display();
    }
}
```

---

### ✅ Output

```
---- Summary PDF ----
[PDF Report]
Title: Monthly Sales Summary
Content: Total: $50,000

---- Detailed HTML ----
<html><body>
<h1>Monthly Sales - Detailed</h1>
<p>Item1: $10,000
Item2: $15,000
Item3: $25,000</p>
</body></html>

---- Summary CSV ----
Title,Content
Monthly Sales Summary,Total: $50,000
```

---