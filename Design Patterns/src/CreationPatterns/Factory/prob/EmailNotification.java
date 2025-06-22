package CreationPatterns.Factory.prob;

public class EmailNotification implements Notification{
    @Override
    public void sendNotification(String message) {
        System.out.println("Sending email: " + message);
    }
}
