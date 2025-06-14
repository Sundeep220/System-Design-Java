package CreationPatterns.Singleton.prob1;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Logger {

    private static Logger logger;
    private List<String> logs = new ArrayList<>();

    private Logger() {
        System.out.println("Logger instance created");
    }

    public static Logger getInstance() {
//        if(logger == null){
//            logger = new Logger();
//        }
//        return logger;

        if (logger == null) {  // double-checked locking for ensuring thread safety
            synchronized (Logger.class) {
                if (logger == null) {
                    logger = new Logger();
                }
            }
        }
        return logger;
    }

    public void log(String message) {
        LocalDateTime now = LocalDateTime.now(); // get current time stamp
        logs.add(now.toString() + ": " + message);
        System.out.println(now.toString() + ": " + message);
    }
    public List<String> getLogs() {
        return logs;
    }
}
