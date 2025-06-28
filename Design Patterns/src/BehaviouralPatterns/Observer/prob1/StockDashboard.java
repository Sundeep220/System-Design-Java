package BehaviouralPatterns.Observer.prob1;

public class StockDashboard implements StockObserver {
    private final String dashboardId;

    public StockDashboard(String id) {
        this.dashboardId = id;
    }

    public void update(String stockSymbol, double newPrice) {
        System.out.println(dashboardId + ": " + stockSymbol + " price updated to $" + newPrice);
    }
}
