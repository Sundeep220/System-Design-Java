package Basics.NotificationSystem;

public class Main {
    public static void main(String[] args) {
        Notification email = new EmailNotification();
        Notification sms = new SMSNotification();

        NotificationProcessor processor = new NotificationProcessor(email);
        processor.send("Hellow");

        System.out.println("=========================Using Factory ====================");
        Notification emailNotification = NotificationFactory.getNotification(NotificationType.EMAIL);
        emailNotification.send("Hola");
    }
}
