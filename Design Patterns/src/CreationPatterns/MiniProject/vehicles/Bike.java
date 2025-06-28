package CreationPatterns.MiniProject.vehicles;

import CreationPatterns.MiniProject.Vehicle;
import CreationPatterns.MiniProject.parts.Engine;
import CreationPatterns.MiniProject.parts.Interior;
import CreationPatterns.MiniProject.parts.WheelConfig;

public class Bike implements Vehicle {
    private String model;
    private String color;

    private final Engine engine;
    private final Interior interior;
    private final WheelConfig wheelConfig;

    public Bike(String model, String color, Engine engine, Interior interior, WheelConfig wheelConfig) {
        this.model = model;
        this.color = color;
        this.engine = engine;
        this.interior = interior;
        this.wheelConfig = wheelConfig;
    }

    // Deep copy constructor
    public Bike(Bike original) {
        this.model = original.model;
        this.color = original.color;
        this.engine = new Engine(original.engine);
        this.interior = new Interior(original.interior);
        this.wheelConfig = new WheelConfig(original.wheelConfig);
    }

    @Override
    public void drive() {
        System.out.println("🏍️ Riding " + model + " (" + color + ") with " +
                engine.getEngineType() + " engine and " + wheelConfig.getCount() + " wheels.");
    }

    @Override
    public Vehicle clone() {
        return new Bike(this);
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
//    We Shouldn't be able to set the engine, interior, and wheelConfig after bike is built
//    public void setEngine(Engine engine) { this.engine = engine; }
//    public void setInterior(Interior interior) { this.interior = interior; }
//    public void setWheelConfig(WheelConfig wheelConfig) { this.wheelConfig = wheelConfig; }
}
