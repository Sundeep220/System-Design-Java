package CreationPatterns.Factory;

public class EmailNotification implements Notification{
    @Override
    public void sendNotification(String message) {
        System.out.println("Sending email: " + message);
    }
}
