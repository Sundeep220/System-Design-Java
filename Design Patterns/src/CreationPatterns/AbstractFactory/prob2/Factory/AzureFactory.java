package CreationPatterns.AbstractFactory.prob2.Factory;

import CreationPatterns.AbstractFactory.prob2.Compute.AWSComputeService;
import CreationPatterns.AbstractFactory.prob2.Compute.AzureComputeService;
import CreationPatterns.AbstractFactory.prob2.Compute.ComputeService;
import CreationPatterns.AbstractFactory.prob2.Storage.AWSStorageService;
import CreationPatterns.AbstractFactory.prob2.Storage.AzureStorageService;
import CreationPatterns.AbstractFactory.prob2.Storage.StorageService;

public class AzureFactory implements CloudFactory {
    @Override
    public ComputeService getComputeService() {
        return new AzureComputeService();
    }

    @Override
    public StorageService getStorageService() {
        return new AzureStorageService();
    }
}
