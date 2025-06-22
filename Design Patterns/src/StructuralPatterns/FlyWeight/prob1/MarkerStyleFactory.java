package StructuralPatterns.FlyWeight.prob1;

import java.util.HashMap;
import java.util.Map;

// Flyweight Factory
public class MarkerStyleFactory {
    public static final Map<String, MarkerStyle> stylePool = new HashMap<>();  // should make private, I did it to test the see number of distinct objects

    public static MarkerStyle getStyle(String type) {
        if (!stylePool.containsKey(type)) {
            switch (type){
                case "hospital" -> stylePool.put(type, new MarkerStyle("🏥", "Red"));
                case "restaurant" -> stylePool.put(type, new MarkerStyle("🍽️", "Blue"));
                case "gas" -> stylePool.put(type, new MarkerStyle("⛽", "Green"));
                default -> throw new IllegalArgumentException("Unknown marker type: " + type);
            }
            System.out.println("Created new style for type: " + type);
        }
        return stylePool.get(type);
    }
}

