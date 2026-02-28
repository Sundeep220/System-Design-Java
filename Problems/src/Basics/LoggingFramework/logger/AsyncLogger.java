package Basics.LoggingFramework.logger;

import Basics.LoggingFramework.enitity.LogRecord;
import Basics.LoggingFramework.enums.LogLevel;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AsyncLogger implements Logger {

    private final Logger delegate;
    private final BlockingQueue<LogRecord> queue;
    private final Thread workerThread;
    private volatile boolean running = true;

    public AsyncLogger(Logger delegate) {
        this.delegate = delegate;
        this.queue = new LinkedBlockingQueue<>();

        this.workerThread = new Thread(this::processLogs);
        this.workerThread.setDaemon(true);
        this.workerThread.start();
    }

    private void processLogs() {
        while (running) {
            try {
                LogRecord record = queue.take();
                dispatch(record);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void dispatch(LogRecord record) {
        // We call underlying logger logic directly.
        if (delegate instanceof DefaultLogger defaultLogger) {
            defaultLogger.internalLog(record);
        }
    }

    @Override
    public void debug(String message) {
        enqueue(LogLevel.DEBUG, message);
    }

    @Override
    public void info(String message) {
        enqueue(LogLevel.INFO, message);
    }

    @Override
    public void warn(String message) {
        enqueue(LogLevel.WARN, message);
    }

    @Override
    public void error(String message) {
        enqueue(LogLevel.ERROR, message);
    }

    @Override
    public void fatal(String message) {
        enqueue(LogLevel.FATAL, message);
    }

    private void enqueue(LogLevel level, String message) {
        LogRecord record = new LogRecord(message, level);
        queue.offer(record);
    }

    public void shutdown() {
        running = false;
        workerThread.interrupt();
    }
}
