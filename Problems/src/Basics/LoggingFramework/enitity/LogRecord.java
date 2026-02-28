package Basics.LoggingFramework.enitity;

import Basics.LoggingFramework.enums.LogLevel;

import java.time.Instant;

public final class LogRecord {

    private final String message;
    private final LogLevel level;
    private final Instant timestamp;
    private final String threadName;

    public LogRecord(String message, LogLevel level) {
        this.message = message;
        this.level = level;
        this.timestamp = Instant.now();
        this.threadName = Thread.currentThread().getName();
    }

    public String getMessage() {
        return message;
    }

    public LogLevel getLevel() {
        return level;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getThreadName() {
        return threadName;
    }
}