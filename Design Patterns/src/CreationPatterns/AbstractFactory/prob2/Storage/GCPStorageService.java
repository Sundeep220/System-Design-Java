package CreationPatterns.AbstractFactory.prob2.Storage;

public class GCPStorageService implements StorageService {
    @Override
    public void storeFile(String filename) {
        System.out.println("Storing file in GCP Cloud Storage: " + filename);
    }
}

