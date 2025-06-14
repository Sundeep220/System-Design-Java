package CreationPatterns.AbstractFactory.prob2;

import CreationPatterns.AbstractFactory.prob2.Client.CloudClient;
import CreationPatterns.AbstractFactory.prob2.Factory.AWSFactory;
import CreationPatterns.AbstractFactory.prob2.Factory.AzureFactory;
import CreationPatterns.AbstractFactory.prob2.Factory.CloudFactory;


public class Main {
    public static void main(String[] args) {
//        // Provision on AWS
//        CloudFactory awsFactory = new AWSFactory();
//        CloudClient awsClient = new CloudClient(awsFactory);
//        awsClient.provision();
//
//        System.out.println();
//
//        // Provision on Azure
//        CloudFactory azureFactory = new AzureFactory();
//        CloudClient azureClient = new CloudClient(azureFactory);
//        azureClient.provision();

        provisionFor(CloudProvider.AWS);
        System.out.println();
        provisionFor(CloudProvider.AZURE);
        System.out.println();
        provisionFor(CloudProvider.GCP);
    }

    private static void provisionFor(CloudProvider provider) {
        System.out.println("Provisioning for: " + provider);
        CloudFactory factory = CloudFactoryProvider.getFactory(provider);
        CloudClient client = new CloudClient(factory);
        client.provision();
    }
}

