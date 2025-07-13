package ParkingLot.Version1;

/**
 * @Purpose: This class represents a spot in a parking lot.
 */
public class Spot {
    private String id;
    private String type;
    private boolean occupied;
    public Spot(String id, String typeName) {
        this.id = id;
        this.type = typeName;
        this.occupied = false;
    }

    public boolean canFitVehicle(Vehicle vehicle) {
        return this.type.equals(vehicle.getType().getTypeName()) && !occupied;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }
}
