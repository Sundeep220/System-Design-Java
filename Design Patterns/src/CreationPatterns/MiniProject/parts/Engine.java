package CreationPatterns.MiniProject.parts;
/**
 * @Purpose: This class represents the engine of a vehicle.
 */
public class Engine {
    private String engineType;  //Ex: Petrol, Diesel, Electric

    public Engine(String engineType) {
        this.engineType = engineType;
    }

    public Engine(Engine engine) {
        this.engineType = engine.engineType;
    }

    public String getEngineType() {
        return engineType;
    }

    public void setEngineType(String engineType) {
        this.engineType = engineType;
    }
}
