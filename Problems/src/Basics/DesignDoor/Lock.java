package Basics.DesignDoor;

public interface Lock {

    boolean unlock(Authenticator authenticator);

    void lock();

    boolean isLocked();
}

