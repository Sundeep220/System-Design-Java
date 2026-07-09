package Basics.LoggingFramework.enums;

public enum LogLevel {

    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4),
    FATAL(5);

    private final int priority;

    LogLevel(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isLessSevereThan(LogLevel other) {
        return this.priority < other.priority;
    }
}
