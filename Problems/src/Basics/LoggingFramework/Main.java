package Basics.LoggingFramework;

import Basics.LoggingFramework.appender.Appender;
import Basics.LoggingFramework.appender.ConsoleAppender;
import Basics.LoggingFramework.appender.FileAppender;
import Basics.LoggingFramework.enums.LogLevel;
import Basics.LoggingFramework.formatter.Formatter;
import Basics.LoggingFramework.formatter.PlainTextFormatter;
import Basics.LoggingFramework.logger.AsyncLogger;
import Basics.LoggingFramework.logger.Logger;

import java.util.List;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        // -------------------------------
        // Create Formatter
        // -------------------------------
        Formatter plainFormatter = new PlainTextFormatter();

        // -------------------------------
        // Create Appenders
        // -------------------------------
        Appender consoleAppender = new ConsoleAppender(plainFormatter);
        Appender fileAppender = new FileAppender("app.log", plainFormatter);

        // (Optional) DatabaseAppender example
        // Appender dbAppender = new DatabaseAppender(
        //         "jdbc:mysql://localhost:3306/logdb",
        //         "root",
        //         "password",
        //         plainFormatter
        // );

        // -------------------------------
        // Build Base Logger
        // -------------------------------
        Logger baseLogger = LoggerBuilder.newBuilder()
                .setLogLevel(LogLevel.DEBUG)
                .addAppender(consoleAppender)
                .addAppender(fileAppender)
                // .addAppender(dbAppender)
                .build();

        // -------------------------------
        // Wrap with Async Logger (Thread-Safe)
        // -------------------------------
        Logger logger = new AsyncLogger(baseLogger);

        // -------------------------------
        // Single Thread Logging
        // -------------------------------
        logger.debug("Debug message");
        logger.info("Application started");
        logger.warn("This is a warning");
        logger.error("Something went wrong");
        logger.fatal("Fatal error occurred");

        // -------------------------------
        // Multi-Threaded Logging Test
        // -------------------------------
        Runnable task = () -> {
            for (int i = 0; i < 5; i++) {
                logger.info("Logging from thread: " + Thread.currentThread().getName());
            }
        };

        Thread t1 = new Thread(task, "Worker-1");
        Thread t2 = new Thread(task, "Worker-2");
        Thread t3 = new Thread(task, "Worker-3");

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        // Give async logger time to process queue
        Thread.sleep(1000);

        // Shutdown async logger gracefully
        if (logger instanceof AsyncLogger asyncLogger) {
            asyncLogger.shutdown();
        }

        System.out.println("Logging test completed.");
    }
}