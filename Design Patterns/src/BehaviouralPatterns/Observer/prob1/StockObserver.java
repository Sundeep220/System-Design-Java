package BehaviouralPatterns.Observer.prob1;

public interface StockObserver {
    void update(String stockSymbol, double newPrice);
}
