# 💼 Problem: **Stock Market Ticker & Dynamic Dashboards**

### 📘 Scenario:

You're building a **Stock Market Ticker System**.

* A `StockMarket` (subject) publishes real-time updates about stock prices.
* Multiple `StockDashboard` instances (observers) are subscribed to **specific stock symbols**.
* Each dashboard wants updates **only for stocks it is subscribed to**.
* When a stock price changes, only relevant dashboards should be notified.
* Dashboards should be able to **subscribe/unsubscribe** from stocks at runtime.

---

## 🧱 Requirements

### Subject: `StockMarket`

* Allows dashboards to subscribe/unsubscribe for a specific stock symbol.
* Maintains a mapping: `stockSymbol -> List of Observers`
* On price change, **only notifies observers of that stock**

### Observer: `StockDashboard`

* Subscribes to **one or more stock symbols**
* Prints updates like: `Dashboard1: AAPL price updated to $192.5`

---

## 🧑‍💻 Ready? Let’s Implement!

---

### ✅ 1. Observer Interface

```java
interface StockObserver {
    void update(String stockSymbol, double newPrice);
}
```

---

### ✅ 2. Subject (StockMarket)

```java
class StockMarket {
    private Map<String, List<StockObserver>> observers = new HashMap<>();
    private Map<String, Double> stockPrices = new HashMap<>();

    public void addStock(String symbol, double initialPrice) {
        stockPrices.put(symbol, initialPrice);
        observers.putIfAbsent(symbol, new ArrayList<>());
    }

    public void subscribe(String stockSymbol, StockObserver observer) {
        observers.putIfAbsent(stockSymbol, new ArrayList<>());
        observers.get(stockSymbol).add(observer);
    }

    public void unsubscribe(String stockSymbol, StockObserver observer) {
        if (observers.containsKey(stockSymbol)) {
            observers.get(stockSymbol).remove(observer);
        }
    }

    public void updateStockPrice(String symbol, double newPrice) {
        if (!stockPrices.containsKey(symbol)) {
            System.out.println("Stock not tracked.");
            return;
        }
        stockPrices.put(symbol, newPrice);
        notifyObservers(symbol, newPrice);
    }

    private void notifyObservers(String stockSymbol, double newPrice) {
        List<StockObserver> observerList = observers.get(stockSymbol);
        for (StockObserver observer : observerList) {
            observer.update(stockSymbol, newPrice);
        }
    }
}
```

---

### ✅ 3. Concrete Observer (StockDashboard)

```java
class StockDashboard implements StockObserver {
    private String dashboardId;

    public StockDashboard(String id) {
        this.dashboardId = id;
    }

    public void update(String stockSymbol, double newPrice) {
        System.out.println(dashboardId + ": " + stockSymbol + " price updated to $" + newPrice);
    }
}
```

---

### ✅ 4. Demo

```java
public class AdvancedObserverDemo {
    public static void main(String[] args) {
        StockMarket market = new StockMarket();

        // Add some stocks
        market.addStock("AAPL", 190.0);
        market.addStock("GOOGL", 2800.0);
        market.addStock("TSLA", 750.0);

        // Create dashboards
        StockDashboard dash1 = new StockDashboard("Dashboard1");
        StockDashboard dash2 = new StockDashboard("Dashboard2");
        StockDashboard dash3 = new StockDashboard("Dashboard3");

        // Subscribe dashboards
        market.subscribe("AAPL", dash1);
        market.subscribe("GOOGL", dash1);

        market.subscribe("AAPL", dash2);
        market.subscribe("TSLA", dash2);

        market.subscribe("TSLA", dash3);

        // Updates
        market.updateStockPrice("AAPL", 192.5);
        market.updateStockPrice("TSLA", 760.0);
        market.updateStockPrice("GOOGL", 2810.0);

        // Unsubscribe
        market.unsubscribe("AAPL", dash2);
        market.updateStockPrice("AAPL", 193.3);
    }
}
```

---

### ✅ Output:

```
Dashboard1: AAPL price updated to $192.5
Dashboard2: AAPL price updated to $192.5
Dashboard2: TSLA price updated to $760.0
Dashboard3: TSLA price updated to $760.0
Dashboard1: GOOGL price updated to $2810.0
Dashboard1: AAPL price updated to $193.3
```

---

## ✅ Concepts Practiced

* Multi-key dynamic observer mapping (`Map<String, List<Observer>>`)
* Dynamic subscription/unsubscription
* Notifying only relevant observers (filtering)
* Loose coupling and real-time event-based updates

---