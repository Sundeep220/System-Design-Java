package CreationPatterns.Singleton.prob2;

import java.util.HashMap;

public class ApplicationConfig {
    private static ApplicationConfig config;
    private final HashMap<String, String> properties = new HashMap<>(); // <key, value>

    private ApplicationConfig() {}

    public static synchronized ApplicationConfig getInstance() {
        if(config == null) {
            config = new ApplicationConfig();
        }
        return config;
    }

    public void set(String key, String value) {
        properties.put(key, value);
    }

    public String get(String key) {
        return properties.get(key);
    }

}
