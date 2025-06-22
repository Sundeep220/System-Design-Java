package StructuralPatterns.FlyWeight.prob1;

// Flyweight
public class MarkerStyle {
    private final String icon;
    private final String color;

    public MarkerStyle(String icon, String color) {
        this.icon = icon;
        this.color = color;
    }

    public void draw(String name, double latitude, double longitude) {
        System.out.println("Rendering [" + name + "] at (" + latitude + ", " + longitude +
                ") using icon '" + icon + "' with color " + color);
    }
}
