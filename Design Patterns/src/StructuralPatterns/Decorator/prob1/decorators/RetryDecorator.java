package StructuralPatterns.Decorator.prob1.decorators;

import StructuralPatterns.Decorator.prob1.Notifier;

public class RetryDecorator extends NotifierDecorator {
    public RetryDecorator(Notifier notifier) {
        super(notifier);
    }

    public void notify(String message) {
        try {
            super.notify(message);
        } catch (Exception e) {
            System.out.println("Attempt 1 to send message...");
            super.notify(message);
        }
    }
}
