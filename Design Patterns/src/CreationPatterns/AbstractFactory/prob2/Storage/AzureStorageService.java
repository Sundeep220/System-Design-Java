package CreationPatterns.AbstractFactory.prob2.Storage;

public class AzureStorageService implements StorageService {
    @Override
    public void storeFile(String filename) {
        System.out.println("Storing Azure Blob Storage file: " + filename);
    }
}
