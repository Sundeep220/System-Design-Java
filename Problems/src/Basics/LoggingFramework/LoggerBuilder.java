package Basics.LoggingFramework;

import Basics.LoggingFramework.appender.Appender;
import Basics.LoggingFramework.enums.LogLevel;
import Basics.LoggingFramework.logger.DefaultLogger;
import Basics.LoggingFramework.logger.Logger;

import java.util.ArrayList;
import java.util.List;

public class LoggerBuilder {

    private static Logger instance;

    private LogLevel logLevel = LogLevel.INFO; // default
    private final List<Appender> appenders = new ArrayList<>();

    private LoggerBuilder() {
    }

    public static LoggerBuilder newBuilder() {
        return new LoggerBuilder();
    }

    public LoggerBuilder setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
        return this;
    }

    public LoggerBuilder addAppender(Appender appender) {
        this.appenders.add(appender);
        return this;
    }

    public synchronized Logger build() {
        if (instance == null) {
            if (appenders.isEmpty()) {
                throw new IllegalStateException("At least one appender must be configured");
            }
            instance = new DefaultLogger(logLevel, appenders);
        }
        return instance;
    }
}
