package CreationPatterns.AbstractFactory.prob2;

import CreationPatterns.AbstractFactory.prob2.Factory.AWSFactory;
import CreationPatterns.AbstractFactory.prob2.Factory.AzureFactory;
import CreationPatterns.AbstractFactory.prob2.Factory.CloudFactory;
import CreationPatterns.AbstractFactory.prob2.Factory.GCPFactory;

public class CloudFactoryProvider {
    public static CloudFactory getFactory(CloudProvider provider) throws IllegalArgumentException {
        return switch (provider) {
            case AWS -> {
                System.out.println(provider.getDisplayName());
                yield new AWSFactory();  // yield is used to return the value of the expression unlike return here as we are using switch expression not switch statement
            }
            case AZURE -> {
                System.out.println(provider.getDisplayName());
                yield new AzureFactory();
            }
            case GCP ->{
                System.out.println(provider.getDisplayName());
                yield new GCPFactory();
            }
            default -> throw new IllegalArgumentException("Unsupported cloud provider: " + provider);
        };
    }
}

