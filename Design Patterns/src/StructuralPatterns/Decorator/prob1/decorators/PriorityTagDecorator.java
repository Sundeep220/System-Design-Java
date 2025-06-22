package StructuralPatterns.Decorator.prob1.decorators;

import StructuralPatterns.Decorator.prob1.Notifier;

public class PriorityTagDecorator extends NotifierDecorator {
    public PriorityTagDecorator(Notifier notifier) {
        super(notifier);
    }

    public void notify(String message) {
        String tagged = "[HIGH PRIORITY] " + message;
        super.notify(tagged);
    }
}
