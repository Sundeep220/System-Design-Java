package CreationPatterns.Factory;

public class SMSNotification implements Notification {
    @Override
    public void sendNotification(String message) {
        System.out.println("Sending SMS: " + message);
    }
}
