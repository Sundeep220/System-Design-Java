package Basics.DesignDoor;

public class Door {

    private Materials material;
    private Lock lock;
    private boolean open;

    public Door(Materials material, Lock lock) {
        this.material = material;
        this.lock = lock;
    }

    public void open() {
        if (lock.isLocked()) {
            System.out.println("Door is locked.");
            return;
        }
        open = true;
        System.out.println("Door opened.");
    }

    public void close() {
        open = false;
        System.out.println("Door closed.");
    }

    public void unlock(Authenticator authenticator) {
        if (lock.unlock(authenticator)) {
            System.out.println("Door unlocked.");
        }
    }

    public void lock() {
        lock.lock();
        System.out.println("Door locked.");
    }
}

