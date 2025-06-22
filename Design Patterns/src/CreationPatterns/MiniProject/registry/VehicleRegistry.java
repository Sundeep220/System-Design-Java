package CreationPatterns.MiniProject.registry;

import java.util.HashMap;
import java.util.Map;
import CreationPatterns.MiniProject.Vehicle;
import CreationPatterns.MiniProject.logging.LogService;

public class VehicleRegistry {
    private static VehicleRegistry instance;
    private Map<String, Vehicle> prototypes = new HashMap<>();
    private Map<String, Vehicle> cache = new HashMap<>();

    private VehicleRegistry() {} // Private constructor
    // return singleton instance
    public static VehicleRegistry getInstance() {
        if (instance == null) {
            instance = new VehicleRegistry();
        }
        return instance;
    }

    public void register(String key, Vehicle prototype) {
        prototypes.put(key.toLowerCase(), prototype);
    }

    public Vehicle getClone(String key) {
        String lowerKey = key.toLowerCase();
        if (cache.containsKey(lowerKey)) {
            LogService.log("[Cache Hit] Returning cached prototype for key: " + key);
            return cache.get(lowerKey).clone();
        }
        Vehicle prototype = prototypes.get(lowerKey);
        if (prototype == null) {
            throw new IllegalArgumentException("No prototype registered for key: " + key);
        }
        LogService.log("[Cache Miss] Cloning prototype for key: " + key);
        cache.put(lowerKey, prototype);
        return prototype.clone();
    }

    public boolean contains(String key) {
        return prototypes.containsKey(key.toLowerCase());
    }

    public void clearCache() {
        cache.clear();
    }
}


