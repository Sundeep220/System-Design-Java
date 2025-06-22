package CreationPatterns.Factory.prob;

public class Main {
    public static void main(String[] args) throws IllegalArgumentException {
        try {
            Notification n1 = NotificationFactory.sendNotification("EMAIL");
            n1.sendNotification("Hello from Email!");

            Notification n2 = NotificationFactory.sendNotification("SMS");
            n2.sendNotification("Hello from SMS!");

            Notification n3 = NotificationFactory.sendNotification("PUSH");
            n3.sendNotification("Hello from Push!");

            // Invalid notification type
            Notification n4 = NotificationFactory.sendNotification("WHATSAPP");
            n4.sendNotification("Hello from WhatsApp!");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }
}

