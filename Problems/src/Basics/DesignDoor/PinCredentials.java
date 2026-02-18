package Basics.DesignDoor;

public class PinCredentials implements Credentials {
    private String pin;

    public PinCredentials(String pin) {
        this.pin = pin;
    }

    public String getPin() {
        return pin;
    }
}
