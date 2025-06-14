package CreationPatterns.AbstractFactory.prob2.Compute;

public class AWSComputeService implements ComputeService {
    @Override
    public void startInstance() {
        System.out.println("Starting AWS EC2instance...");
    }
}
