package CreationPatterns.AbstractFactory.prob2.Factory;

import CreationPatterns.AbstractFactory.prob2.Compute.ComputeService;
import CreationPatterns.AbstractFactory.prob2.Storage.StorageService;

public interface CloudFactory {
    ComputeService getComputeService();
    StorageService getStorageService();
}
