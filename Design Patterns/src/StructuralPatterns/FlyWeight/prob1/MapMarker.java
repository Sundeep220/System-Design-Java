package StructuralPatterns.FlyWeight.prob1;

// Flyweight Context
public class MapMarker {
    private final String name;
    private final double latitude;
    private final double longitude;
    private final MarkerStyle style;

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

