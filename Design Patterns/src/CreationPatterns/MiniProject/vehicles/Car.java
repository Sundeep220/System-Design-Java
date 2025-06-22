package CreationPatterns.MiniProject.vehicles;

import CreationPatterns.MiniProject.Vehicle;
import CreationPatterns.MiniProject.parts.Engine;
import CreationPatterns.MiniProject.parts.Interior;
import CreationPatterns.MiniProject.parts.WheelConfig;

public class Car implements Vehicle {
    private String model;
    private String color;

    private Engine engine;
    private WheelConfig wheelConfig;
    private Interior interior;

    public Car(String model, String color, Engine engine, Interior interior, WheelConfig wheelConfig) {
        this.model = model;
        this.color = color;
        this.engine = engine;
        this.wheelConfig = wheelConfig;
        this.interior = interior;
    }
    // Deep copy
    public Car(Car original) {
        this.model = original.model;
        this.color = original.color;
        this.engine = new Engine(original.engine);
        this.wheelConfig = new WheelConfig(original.wheelConfig);
        this.interior = new Interior(original.interior);
    }

    @Override
    public String toString() {
        return "Car{" +
                "engine=" + engine +
                ", wheelConfig=" + wheelConfig +
                ", interior=" + interior +
                '}';
    }

    @Override
    public Car clone() {
        return new Car(this);
    }

    @Override
    public void drive() {
        System.out.println("🚗 Driving " + model + " (" + color + ") with " +
                engine.getEngineType() + " engine and " + wheelConfig.getCount() + " wheels.");
    }

    public String getModel() { return model; }
    public String getColor() { return color; }
    @Override
    public Engine getEngine() { return engine; }
    @Override
    public WheelConfig getWheelConfig() { return wheelConfig; }
    @Override
    public Interior getInterior() { return interior; }

    public void setModel(String model) { this.model = model; }
    public void setColor(String color) { this.color = color; }
    // We can't(or shouldn't be able to) change the parts of the car once it's built
//    public void setEngine(Engine engine) { this.engine = engine; }
//    public void setWheelConfig(WheelConfig wheelConfig) { this.wheelConfig = wheelConfig; }
//    public void setInterior(Interior interior) { this.interior = interior; }
}
