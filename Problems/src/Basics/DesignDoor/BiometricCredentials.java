package Basics.DesignDoor;

public class BiometricCredentials implements Credentials {
    private String fingerprintData;

    public BiometricCredentials(String fingerprintData) {
        this.fingerprintData = fingerprintData;
    }

    public String getFingerprintData() {
        return fingerprintData;
    }
}
