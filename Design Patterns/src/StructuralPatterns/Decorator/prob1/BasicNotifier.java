package StructuralPatterns.Decorator.prob1;

public class BasicNotifier implements Notifier {
    public void notify(String message) {
        System.out.println("Sending notification: " + message);
    }
}
