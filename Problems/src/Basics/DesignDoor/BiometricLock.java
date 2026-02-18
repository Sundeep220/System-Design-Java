package Basics.DesignDoor;

public class BiometricLock implements Lock {

    private boolean locked = true;

    @Override
    public boolean unlock(Authenticator authenticator) {
        locked = false;
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
