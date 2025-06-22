package CreationPatterns.Factory.prob;

public class NotificationFactory {
    public static Notification sendNotification (String type) throws IllegalArgumentException{
        if (type == null) {
            throw new IllegalArgumentException("Notification type cannot be null");
        }
        return switch (type) {
            case "EMAIL" -> new EmailNotification();
            case "SMS" -> new SMSNotification();
            case "PUSH" -> new PushNotification();
            default -> throw new IllegalArgumentException("Invalid notification type: " + type);
        };
    }
}
