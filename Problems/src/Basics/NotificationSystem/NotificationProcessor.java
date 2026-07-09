package Basics.NotificationSystem;

public class NotificationProcessor {
    Notification notifier;

    public NotificationProcessor(Notification notifier){
        this.notifier = notifier;
    }

    public void send(String message){
        notifier.send(message);
    }
}
