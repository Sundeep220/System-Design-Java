package ParkingLot.Version2;

import java.util.Map;

public abstract class ParkingSpot {
    private final String id;
    private final SpotType type;
    private boolean isOccupied;
    private final Map<String, Integer> distanceFromEntrances; // entranceId -> distance

    public ParkingSpot(String id, SpotType type, Map<String, Integer> distanceFromEntrances) {
        this.id = id;
        this.type = type;
        this.distanceFromEntrances = distanceFromEntrances;
        this.isOccupied = false;
    }

    public String getId() {
        return id;
    }

    public SpotType getType() {
        return type;
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    public void setOccupied(boolean occupied) {
        this.isOccupied = occupied;
    }

    public int getDistanceFrom(String entranceId) {
        return distanceFromEntrances.getOrDefault(entranceId, Integer.MAX_VALUE);
    }

    public Map<String, Integer> getAllDistances() {
        return distanceFromEntrances;
    }
}
