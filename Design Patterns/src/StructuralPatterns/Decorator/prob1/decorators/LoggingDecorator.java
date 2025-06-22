package StructuralPatterns.Decorator.prob1.decorators;

import StructuralPatterns.Decorator.prob1.Notifier;

public class LoggingDecorator extends NotifierDecorator {
    public LoggingDecorator(Notifier notifier) {
        super(notifier);
    }

    public void notify(String message) {
        System.out.println("Logging: " + message);
        super.notify(message);
    }
}
