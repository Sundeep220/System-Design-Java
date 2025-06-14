package CreationPatterns.AbstractFactory.prob2.Compute;

public class GCPComputeService implements ComputeService {
    @Override
    public void startInstance() {
        System.out.println("Starting Compute Engine instance on GCP");
    }
}

