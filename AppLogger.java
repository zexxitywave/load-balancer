import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple thread-safe logger.
 * Writes log entries to both the console and lb_requests.log file.
 * Format: [YYYY-MM-DD HH:mm:ss] [LEVEL] message
 *
 * WHAT THIS FILE DOES:
 * --------------------
 * Centralized logging for the entire system. Every component (LoadBalancer,
 * Worker, LBRequestServer, etc.) uses AppLogger instead of System.out.println.
 *
 * How it works:
 *   - Static initializer opens lb_requests.log in append mode at class load time
 *   - write() is synchronized → only one thread can write at a time (thread-safe)
 *   - Every log entry gets a timestamp + level prefix:
 *       [2026-07-22 23:45:42] [INFO ] Request handled | worker=0 | sid=3 | duration=1ms
 *       [2026-07-22 23:45:42] [WARN ] Worker 4 is DOWN. Attempting restart...
 *       [2026-07-22 23:45:42] [ERROR] LoadBalancer error: Address already in use
 *
 * Log levels:
 *   info()       → general operational events (request handled, worker started)
 *   warn()       → non-fatal issues (worker down, stale connection)
 *   error()      → failures and exceptions
 *   logRequest() → specialized method for completed requests (worker, sid, duration)
 *
 * Output goes to:
 *   1. Console (System.out) → visible in the terminal
 *   2. lb_requests.log file → persists across runs (append mode)
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
