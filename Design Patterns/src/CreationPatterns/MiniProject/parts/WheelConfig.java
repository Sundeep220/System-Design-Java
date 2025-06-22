package CreationPatterns.MiniProject.parts;

/**
 * @Purpose: This class represents the configuration of a vehicle's wheels.
 */
public class WheelConfig {
    private int count; // 2 for bike, 4 for a car, etc.

    public WheelConfig(int count) {
        this.count = count;
    }

    public WheelConfig(WheelConfig original) {
        this.count = original.count;
    }

    public int getCount() { return count; }
}
