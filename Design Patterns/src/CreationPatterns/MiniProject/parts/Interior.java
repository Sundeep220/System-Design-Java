package CreationPatterns.MiniProject.parts;

/**
 * @Purpose: This class represents the interior of a vehicle.
 */
public class Interior {
    private String material; // e.g., Leather, Fabric
    private boolean infotainment;

    public Interior(String material, boolean infotainment) {
        this.material = material;
        this.infotainment = infotainment;
    }

    public Interior(Interior original) {
        this.material = original.material;
        this.infotainment = original.infotainment;
    }

    public String getMaterial() { return material; }
    public boolean hasInfotainment() { return infotainment; }
}
