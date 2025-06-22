package CreationPatterns.MiniProject;

import CreationPatterns.MiniProject.parts.Engine;
import CreationPatterns.MiniProject.parts.Interior;
import CreationPatterns.MiniProject.parts.WheelConfig;

public interface Vehicle extends Cloneable{
    void drive();
    Vehicle clone();

    Engine getEngine();
    Interior getInterior();
    WheelConfig getWheelConfig();
    String getModel();
    String getColor();
}
