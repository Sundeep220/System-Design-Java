package CreationPatterns.AbstractFactory.prob2.Storage;

public class AWSStorageService implements StorageService {
    @Override
    public void storeFile(String filename) {
        System.out.println("Storing AWS S3 file: " + filename);
    }
}
