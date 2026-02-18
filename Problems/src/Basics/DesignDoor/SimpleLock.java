package Basics.DesignDoor;

public class SimpleLock implements Lock {

    private boolean locked = true;

    @Override
    public boolean unlock(Authenticator authenticator) {
        locked = false;
        return true;
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

