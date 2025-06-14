package CreationPatterns.AbstractFactory.prob2.Factory;

import CreationPatterns.AbstractFactory.prob2.Compute.AWSComputeService;
import CreationPatterns.AbstractFactory.prob2.Compute.ComputeService;
import CreationPatterns.AbstractFactory.prob2.Storage.AWSStorageService;
import CreationPatterns.AbstractFactory.prob2.Storage.StorageService;

public class AWSFactory implements CloudFactory {
    @Override
    public ComputeService getComputeService() {
        return new AWSComputeService();
    }

    @Override
    public StorageService getStorageService() {
        return new AWSStorageService();
    }
}
