package StructuralPatterns.Decorator.prob1.decorators;

import StructuralPatterns.Decorator.prob1.Notifier;

public class DelaySendingDecorator extends NotifierDecorator {
    private final long delayMillis;

    public DelaySendingDecorator(Notifier notifier, long delayMillis) {
        super(notifier);
        this.delayMillis = delayMillis;
    }

    @Override
    public void notify(String message) {
        try {
            System.out.println("[Delay] Waiting for " + delayMillis + "ms...");
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.notify(message);
    }
}

