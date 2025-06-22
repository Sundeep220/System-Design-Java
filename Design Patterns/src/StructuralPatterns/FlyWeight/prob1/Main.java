package StructuralPatterns.FlyWeight.prob1;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        // Create a list of markers
        List<MapMarker> markers = new ArrayList<>();

        // Create a random number generator
        Random rand = new Random();

        // Simulate adding 1000 markers for each type
        for (int i = 0; i < 1000; i++) {
            markers.add(new MapMarker("Hospital_" + i,
                    rand.nextDouble() * 100,
                    rand.nextDouble() * 100,
                    MarkerStyleFactory.getStyle("hospital"))); // At first, it will create the style, then use it from next iterations

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
        System.out.println("Unique MarkerStyles used: " + MarkerStyleFactory.stylePool.size());
    }
}
