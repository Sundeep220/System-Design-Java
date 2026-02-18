package Basics.DesignDoor;

public class Main {

    public static void main(String[] args) {

        Lock pinLock = new PinLock();
        Door door = new Door(Materials.WOOD, pinLock);

        Authenticator authenticator =
                new PinAuthenticator("1234", "1234");

        door.unlock(authenticator);
        door.open();
    }
}
