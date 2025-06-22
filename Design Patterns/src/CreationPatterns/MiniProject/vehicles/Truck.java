package CreationPatterns.MiniProject.vehicles;

import CreationPatterns.MiniProject.Vehicle;
import CreationPatterns.MiniProject.parts.Engine;
import CreationPatterns.MiniProject.parts.Interior;
import CreationPatterns.MiniProject.parts.WheelConfig;

public class Truck implements Vehicle {
    private String model;
    private String color;

    private Engine engine;
    private Interior interior;
    private WheelConfig wheelConfig;


    public Truck(String model, String color, Engine engine, Interior interior, WheelConfig wheelConfig) {
        this.model = model;
        this.color = color;
        this.engine = engine;
        this.interior = interior;
        this.wheelConfig = wheelConfig;
    }

    // Deep copy constructor
    public Truck(Truck original) {
        this.model = original.model;
        this.color = original.color;
        this.engine = new Engine(original.engine);
        this.interior = new Interior(original.interior);
        this.wheelConfig = new WheelConfig(original.wheelConfig);
    }

    @Override
    public void drive() {
        System.out.println("🚚 Driving Truck " + model + " (" + color + ") with " +
                engine.getEngineType() + " engine and " + wheelConfig.getCount() + " wheels.");
    }

    @Override
    public Vehicle clone() {
        return new Truck(this);
    }

    // Getters
    public String getModel() { return model; }
    public String getColor() { return color; }
    @Override
    public Engine getEngine() { return engine; }
    @Override
    public Interior getInterior() { return interior; }
    @Override
    public WheelConfig getWheelConfig() { return wheelConfig; }

    // Setters
    public void setModel(String model) { this.model = model; }
    public void setColor(String color) { this.color = color; }
}

