package Basics.DesignDoor;

import java.util.Scanner;

public class PinLock implements Lock {

    private boolean locked = true;

    @Override
    public boolean unlock(Authenticator authenticator) {

        if (authenticator.authenticate()) {
            locked = false;
            return true;
        }

        System.out.println("Authentication failed.");
        return false;
    }

    @Override
    public void lock() {
        locked = true;
    }

    @Override
    public boolean isLocked() {
        return locked;
    }
}



