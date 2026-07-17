import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple thread-safe logger.
 * Writes log entries to both the console and lb_requests.log file.
 * Format: [YYYY-MM-DD HH:mm:ss] [LEVEL] message
 */
public class AppLogger {
    private static final String LOG_FILE = "lb_requests.log";
    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static PrintWriter fileWriter;

    static {
        try {
            // append=true so logs persist across restarts
            fileWriter = new PrintWriter(new FileWriter(LOG_FILE, true), true);
        } catch (IOException e) {
            System.err.println("[Logger] Could not open log file: " + e.getMessage());
        }
    }

    private static synchronized void write(String level, String message) {
        String entry = "[" + LocalDateTime.now().format(fmt) + "] [" + level + "] " + message;
        System.out.println(entry);
        if (fileWriter != null) fileWriter.println(entry);
    }

    public static void info(String message)  {
        write("INFO ", message);
    }
    public static void warn(String message)  { write("WARN ", message); }
    public static void error(String message) { write("ERROR", message); }

    /**
     * Log a completed request with duration.
     * @param workerIndex  which worker handled the request
     * @param sid          student ID that was requested
     * @param durationMs   how long the round-trip took in milliseconds
     */
    public static void logRequest(int workerIndex, String sid, long durationMs) {
        info("Request handled | worker=" + workerIndex + " | sid=" + sid.trim() + " | duration=" + durationMs + "ms");
    }
}
