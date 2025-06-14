package CreationPatterns.Singleton.prob2;

public class Main {
    public static void main(String[] args) {
        ApplicationConfig config1 = ApplicationConfig.getInstance();
        config1.set("db_url", "jdbc:mysql://localhost");
        config1.set("timeout", "30s");

        ApplicationConfig config2 = ApplicationConfig.getInstance();
        System.out.println("db_url from config2: " + config1.get("db_url"));
        System.out.println("timeout from config2: " + config2.get("timeout"));

        System.out.println("config1 == config2: " + (config1 == config2));
    }
}

