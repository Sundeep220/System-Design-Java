package CreationPatterns.AbstractFactory.prob2.Factory;

import CreationPatterns.AbstractFactory.prob2.Compute.ComputeService;
import CreationPatterns.AbstractFactory.prob2.Compute.GCPComputeService;
import CreationPatterns.AbstractFactory.prob2.Storage.GCPStorageService;
import CreationPatterns.AbstractFactory.prob2.Storage.StorageService;

public class GCPFactory implements CloudFactory {
    @Override
    public ComputeService getComputeService() {
        return new GCPComputeService();
    }

    @Override
    public StorageService getStorageService() {
        return new GCPStorageService();
    }
}
