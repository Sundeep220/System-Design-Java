package CreationPatterns.AbstractFactory.prob2.Client;

import CreationPatterns.AbstractFactory.prob2.Factory.CloudFactory;

public class CloudClient {
    private final CloudFactory factory;

    public CloudClient(CloudFactory factory) {
        this.factory = factory;
    }

    public void provision(){
        factory.getComputeService().startInstance();
        factory.getStorageService().storeFile("log.txt");
    }
}
