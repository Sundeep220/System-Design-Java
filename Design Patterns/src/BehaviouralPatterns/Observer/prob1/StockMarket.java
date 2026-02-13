package BehaviouralPatterns.Observer.prob1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StockMarket implements Subject {
    private final Map<String, List<StockObserver>> observers;
    private final Map<String, Double> stockPrices;

    public StockMarket() {
        observers = new HashMap<>();
        stockPrices = new HashMap<>();
    }

    public void addStock(String stock, double price) {
        stockPrices.put(stock, price);
        observers.putIfAbsent(stock, new ArrayList<>());
    }

    public void updateStock(String stock, double newPrice) {
        if(!stockPrices.containsKey(stock)) {
            System.out.println("Stock not tracked.");
            return;
        }

        stockPrices.put(stock, newPrice);
        notifyObservers(stock, newPrice);
    }

    public void subscribe(String stock, StockObserver observer) {
        // Checks if observer already tracks the stock
        observers.putIfAbsent(stock, new ArrayList<>());

        // Adds observer to the stock observer list
        observers.get(stock).add(observer);
    }

    public void unsubscribe(String stock, StockObserver observer) {
        if (observers.containsKey(stock)) {
            observers.get(stock).remove(observer);
        }
    }

    public void notifyObservers(String stock, double newPrice) {
        // Checks if the stock is being tracked, if not, does nothing
        List<StockObserver> observers = this.observers.getOrDefault(stock, new ArrayList<>()); // <== this.observers.getOrDefault(stock, new ArrayList<>()>
        for (StockObserver observer : observers) {
            observer.update(stock, newPrice);
        }
    }
}
