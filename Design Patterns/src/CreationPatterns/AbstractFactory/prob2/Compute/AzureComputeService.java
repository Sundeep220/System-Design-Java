package CreationPatterns.AbstractFactory.prob2.Compute;

public class AzureComputeService implements ComputeService {
    @Override
    public void startInstance() {
        System.out.println("Starting Azure VM...");
    }
}
