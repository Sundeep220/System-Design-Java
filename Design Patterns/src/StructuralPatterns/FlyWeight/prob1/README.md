# 🧠 Advanced Flyweight Problem: **Online Map System – Marker Rendering**

---

### 📍 **Problem Statement**

You are building a **map visualization system** (like Google Maps). The map displays thousands of markers for:

* **Hospitals**
* **Restaurants**
* **Gas Stations**

Each marker type has:

* Common visual style: icon, color → ✅ **Shared (Flyweight)**
* Unique data: location (latitude/longitude), name → ❌ **Not shared**

You need to **optimize memory** by sharing marker styles instead of creating a new object for each marker.

---

## 🎯 Objectives

1. Create **MarkerStyle (Flyweight)** that stores icon & color.
2. Build a **MarkerStyleFactory** to manage and reuse styles.
3. Create a **MapMarker (Context)** that includes location and marker style.
4. Client should render thousands of markers using **just 3 shared styles**.

---

## 🔧 Implementation

---

### ✅ Step 1: Flyweight – `MarkerStyle`

```java
public class MarkerStyle {
    private String icon;
    private String color;

    public MarkerStyle(String icon, String color) {
        this.icon = icon;
        this.color = color;
    }

    public void draw(String name, double latitude, double longitude) {
        System.out.println("Rendering [" + name + "] at (" + latitude + ", " + longitude +
            ") using icon '" + icon + "' with color " + color);
    }
}
```

---

### ✅ Step 2: Flyweight Factory – `MarkerStyleFactory`

```java
import java.util.HashMap;
import java.util.Map;

public class MarkerStyleFactory {
    private static final Map<String, MarkerStyle> stylePool = new HashMap<>();

    public static MarkerStyle getStyle(String type) {
        if (!stylePool.containsKey(type)) {
            switch (type) {
                case "hospital":
                    stylePool.put(type, new MarkerStyle("🏥", "Red"));
                    break;
                case "restaurant":
                    stylePool.put(type, new MarkerStyle("🍽️", "Blue"));
                    break;
                case "gas":
                    stylePool.put(type, new MarkerStyle("⛽", "Green"));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown marker type: " + type);
            }
            System.out.println("Created new style for type: " + type);
        }
        return stylePool.get(type);
    }
}
```

---

### ✅ Step 3: Context – `MapMarker`

```java
public class MapMarker {
    private String name;
    private double latitude;
    private double longitude;
    private MarkerStyle style;

    public MapMarker(String name, double latitude, double longitude, MarkerStyle style) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.style = style;
    }

    public void render() {
        style.draw(name, latitude, longitude);
    }
}
```

---

### ✅ Step 4: Client Code (Test)

```java
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        List<MapMarker> markers = new ArrayList<>();
        Random rand = new Random();

        // Simulate adding 1000 markers for each type
        for (int i = 0; i < 1000; i++) {
            markers.add(new MapMarker("Hospital_" + i,
                    rand.nextDouble() * 100,
                    rand.nextDouble() * 100,
                    MarkerStyleFactory.getStyle("hospital")));

            markers.add(new MapMarker("Restaurant_" + i,
                    rand.nextDouble() * 100,
                    rand.nextDouble() * 100,
                    MarkerStyleFactory.getStyle("restaurant")));

            markers.add(new MapMarker("GasStation_" + i,
                    rand.nextDouble() * 100,
                    rand.nextDouble() * 100,
                    MarkerStyleFactory.getStyle("gas")));
        }

        // Render a few sample markers
        System.out.println("\nRendering sample markers:\n");
        for (int i = 0; i < 5; i++) {
            markers.get(i).render();
        }

        System.out.println("\nTotal markers created: " + markers.size());
        System.out.println("Unique MarkerStyles used: 3");
    }
}
```

---

### ✅ Sample Output

```
Created new style for type: hospital
Created new style for type: restaurant
Created new style for type: gas

Rendering sample markers:

Rendering [Hospital_0] at (18.5, 92.3) using icon '🏥' with color Red
Rendering [Restaurant_0] at (36.7, 22.1) using icon '🍽️' with color Blue
Rendering [GasStation_0] at (80.9, 61.5) using icon '⛽' with color Green
Rendering [Hospital_1] at (52.2, 11.8) using icon '🏥' with color Red
Rendering [Restaurant_1] at (24.6, 85.9) using icon '🍽️' with color Blue

Total markers created: 3000  
Unique MarkerStyles used: 3
```

---

## ✅ What You Practiced

| Concept               | Applied As                                |
| --------------------- | ----------------------------------------- |
| Flyweight             | Shared `MarkerStyle` (icon + color)       |
| Extrinsic Data        | Each `MapMarker` has unique position/name |
| Factory Pattern Combo | Used `MarkerStyleFactory` to reuse styles |
| Memory Optimization   | Saved memory using only 3 shared objects  |

---
