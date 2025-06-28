package BehaviouralPatterns.Observer.prob1;

public class Main {
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
        market.updateStock("AAPL", 192.5);
        market.updateStock("TSLA", 760.0);
        market.updateStock("GOOGL", 2810.0);

        // Unsubscribe
        market.unsubscribe("AAPL", dash2);
        market.updateStock("AAPL", 193.3);
    }
}
