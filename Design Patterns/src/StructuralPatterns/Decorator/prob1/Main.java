package StructuralPatterns.Decorator.prob1;

import StructuralPatterns.Decorator.prob1.decorators.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("===> 1. Basic Logging Notifier:");
        Notifier loggingNotifier = new LoggingDecorator(new BasicNotifier());
        loggingNotifier.notify("Server Started");

        System.out.println("\n===> 2. Retry + Encrypted + Logging Notifier:");
        Notifier retryEncryptedLogger = new RetryDecorator(
                new EncryptionDecorator(
                        new LoggingDecorator(
                                new BasicNotifier())));
//        Notifier encryptedLogger = new EncryptionDecorator(loggingNotifier);
//        Notifier retryEncryptedLogger = new RetryDecorator(encryptedLogger);
        retryEncryptedLogger.notify("User registration completed");

        System.out.println("\n===> 3. High Priority + Delay + Encrypted + Retry + Notifier:");
        Notifier fullStack = new PriorityTagDecorator(
                new DelaySendingDecorator(
                        new EncryptionDecorator(
                                new RetryDecorator(
                                        new BasicNotifier())), 2000));
        fullStack.notify("Critical system alert!");
    }
}

