package StructuralPatterns.Decorator.prob1.decorators;

import StructuralPatterns.Decorator.prob1.Notifier;

abstract class NotifierDecorator implements Notifier {
    protected Notifier notifier;

    public NotifierDecorator(Notifier notifier) {
        this.notifier = notifier;
    }

    public void notify(String message) {
        notifier.notify(message);
    }
}
