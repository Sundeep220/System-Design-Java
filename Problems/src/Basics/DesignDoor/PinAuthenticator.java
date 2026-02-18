package Basics.DesignDoor;

public class PinAuthenticator implements Authenticator {

    private String correctPin;
    private String enteredPin;

    public PinAuthenticator(String correctPin, String enteredPin) {
        this.correctPin = correctPin;
        this.enteredPin = enteredPin;
    }

    @Override
    public boolean authenticate() {
        return correctPin.equals(enteredPin);
    }
}
