package Basics.NotificationSystem;

class NotificationFactory {

    public static Notification getNotification(NotificationType type) {

        switch (type) {
            case EMAIL:
                return new EmailNotification();

            case SMS:
                return new SMSNotification();

            default:
                throw new IllegalArgumentException("Invalid notification type");
        }
    }
}
