package CreationPatterns.Prototype.prob2.registry;

import CreationPatterns.Prototype.prob2.Document;

import java.util.HashMap;
import java.util.Map;

public class DocumentRegistry {
    private final Map<String, Document> registry = new HashMap<>();

    public void register(String key, Document prototype) {
        registry.put(key, prototype);
    }

    public Document getClone(String key) {
        Document prototype = registry.get(key);
        if (prototype == null) {
            throw new IllegalArgumentException("No prototype registered for key: " + key);
        }
        return prototype.clone();  // Clone on access
    }
}

