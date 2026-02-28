package Basics.LoggingFramework.appender;

import Basics.LoggingFramework.enitity.LogRecord;
import Basics.LoggingFramework.formatter.Formatter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseAppender implements Appender {

    private final Formatter formatter; // optional if storing raw fields
    private final String url;
    private final String username;
    private final String password;

    public DatabaseAppender(String url, String username, String password, Formatter formatter) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.formatter = formatter;
    }

    @Override
    public void append(LogRecord record) {

        String sql = "INSERT INTO logs(timestamp, level, message, thread_name) VALUES (?, ?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, record.getTimestamp());
            statement.setString(2, record.getLevel().name());
            statement.setString(3, record.getMessage());
            statement.setString(4, record.getThreadName());

            statement.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Database logging failed: " + e.getMessage());
        }
    }
}