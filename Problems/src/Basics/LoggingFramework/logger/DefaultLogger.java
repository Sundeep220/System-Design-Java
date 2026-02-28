package Basics.LoggingFramework.logger;
import Basics.LoggingFramework.appender.Appender;
import Basics.LoggingFramework.enitity.LogRecord;
import Basics.LoggingFramework.enums.LogLevel;

import java.util.List;

public class DefaultLogger implements Logger {

    private final LogLevel currentLevel;
    private final List<Appender> appenders;

    public DefaultLogger(LogLevel currentLevel, List<Appender> appenders) {
        this.currentLevel = currentLevel;
        this.appenders = appenders;
    }

    @Override
    public void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    @Override
    public void info(String message) {
        log(LogLevel.INFO, message);
    }

    @Override
    public void warn(String message) {
        log(LogLevel.WARN, message);
    }

    @Override
    public void error(String message) {
        log(LogLevel.ERROR, message);
    }

    @Override
    public void fatal(String message) {
        log(LogLevel.FATAL, message);
    }

    private void log(LogLevel level, String message) {

        // Level filtering
        if (!level.isGreaterOrEqual(currentLevel)) {
            return;
        }

        // Create log record
        LogRecord record = new LogRecord(message, level);

        // Dispatch to all appenders
        for (Appender appender : appenders) {
            try {
                appender.append(record);
            } catch (Exception e) {
                // Logging should never crash application
                // Swallow exception (best-effort logging)
            }
        }
    }

    void internalLog(LogRecord record) {

        if (!record.getLevel().isGreaterOrEqual(currentLevel)) {
            return;
        }

        for (Appender appender : appenders) {
            try {
                appender.append(record);
            } catch (Exception ignored) {
            }
        }
    }
}