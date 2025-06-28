package BehaviouralPatterns.Observer.prob1;


public interface Subject {
    void subscribe(String stockSymbol, StockObserver observer);
    void unsubscribe(String stockSymbol, StockObserver observer);
    void notifyObservers(String symbol, double newPrice);
}
