package StructuralPatterns.Decorator.prob1.decorators;

import StructuralPatterns.Decorator.prob1.Notifier;

public class EncryptionDecorator extends NotifierDecorator {
    public EncryptionDecorator(Notifier notifier) {
        super(notifier);
    }

    @Override
    public void notify(String message) {
        String encrypted = encrypt(message);
        super.notify(encrypted);
    }

    private String encrypt(String message) {
        return "ENCRYPTED(" + message + ")";
    }
}
