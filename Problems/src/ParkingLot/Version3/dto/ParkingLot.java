package ParkingLot.Version3.dto;

import ParkingLot.Version3.dto.ParkingSpot.ParkingSpot;
import ParkingLot.Version3.enums.ParkingSpotEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParkingLot {
    private String name;
    private List<EntrancePanel> entrancePanels;
    private List<ExitPanel> exitPanels;
    private DisplayBoard displayBoard;
    private Map<ParkingSpotEnum, List<ParkingSpot>> freeparkingSpots;
    private Map<ParkingSpotEnum, List<ParkingSpot>> occupiedParkingSpots;

    private static ParkingLot parkingLot;

    public synchronized static ParkingLot getInstance() {
        if (parkingLot == null) {
            parkingLot = new ParkingLot("ABC Parking Lot");
        }
        return parkingLot;
    }

    private ParkingLot(String name) {
        this.name = name;
        this.entrancePanels = new ArrayList<>();
        this.exitPanels = new ArrayList<>();
        this.displayBoard = DisplayBoard.getInstance();
        this.freeparkingSpots = new HashMap<>();
        this.occupiedParkingSpots = new HashMap<>();

        freeparkingSpots.put(ParkingSpotEnum.MINI, new ArrayList<>());
        freeparkingSpots.put(ParkingSpotEnum.COMPACT, new ArrayList<>());
        freeparkingSpots.put(ParkingSpotEnum.LARGE, new ArrayList<>());

        occupiedParkingSpots.put(ParkingSpotEnum.MINI, new ArrayList<>());
        occupiedParkingSpots.put(ParkingSpotEnum.COMPACT, new ArrayList<>());
        occupiedParkingSpots.put(ParkingSpotEnum.LARGE, new ArrayList<>());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<EntrancePanel> getEntrancePanels() {
        return entrancePanels;
    }

    public void setEntrancePanels(List<EntrancePanel> entrancePanels) {
        this.entrancePanels = entrancePanels;
    }

    public List<ExitPanel> getExitPanels() {
        return exitPanels;
    }

    public void setExitPanels(List<ExitPanel> exitPanels) {
        this.exitPanels = exitPanels;
    }

    public DisplayBoard getDisplayBoard() {
        return displayBoard;
    }

    public void setDisplayBoard(DisplayBoard displayBoard) {
        this.displayBoard = displayBoard;
    }

    public static ParkingLot getParkingLot() {
        return parkingLot;
    }

    public static void setParkingLot(ParkingLot parkingLot) {
        ParkingLot.parkingLot = parkingLot;
    }

    public Map<ParkingSpotEnum, List<ParkingSpot>> getFreeparkingSpots() {
        return freeparkingSpots;
    }

    public void setFreeparkingSpots(Map<ParkingSpotEnum, List<ParkingSpot>> freeparkingSpots) {
        this.freeparkingSpots = freeparkingSpots;
    }

    public Map<ParkingSpotEnum, List<ParkingSpot>> getOccupiedParkingSpots() {
        return occupiedParkingSpots;
    }

    public void setOccupiedParkingSpots(Map<ParkingSpotEnum, List<ParkingSpot>> occupiedParkingSpots) {
        this.occupiedParkingSpots = occupiedParkingSpots;
    }
}
