import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Loads and exposes config.properties values.
 * All other classes read config from here instead of hardcoding values.
 *
 * WHAT THIS FILE DOES:
 * --------------------
 * Single place where all configuration is loaded and exposed to the rest of the system.
 * No other class reads config.properties directly — they all call AppConfig methods.
 *
 * How it works:
 *   - Static initializer runs once when the class is first used
 *   - Loads config.properties file into a Properties object
 *   - Exits the process if the file is missing (nothing can run without config)
 *
 * What it exposes:
 *   getLBPort()              → port the LoadBalancer listens on (default: 12345)
 *   getWorkerList()          → path to worker_list.txt (default: "worker_list.txt")
 *   getDbUrl()               → JDBC connection URL for PostgreSQL
 *   getDbUser()              → database username
 *   getDbPassword()          → database password
 *   getHealthCheckInterval() → how often to ping workers in seconds (default: 5)
 *   getDbPoolSize()          → how many DB connections per worker pool (default: 5)
 *
 * Why this pattern:
 *   - Changing any setting only requires editing config.properties — no recompile needed
 *   - Passwords and URLs are not hardcoded in source code
 *   - Default values are provided as fallbacks in case a key is missing
 */
public class AppConfig {
    private static final Properties props = new Properties();

    static {
        try {
            props.load(new FileReader("config.properties"));
        } catch (IOException e) {
            System.err.println("[Config] Could not load config.properties: " + e.getMessage());
            System.exit(1);
        }
    }

    public static int getLBPort() {
        return Integer.parseInt(props.getProperty("lb.port", "12345"));
    }

    public static String getWorkerList() {
        return props.getProperty("worker.list", "worker_list.txt");
    }

    public static String getDbUrl() {
        return props.getProperty("db.url");
    }

    public static String getDbUser() {
        return props.getProperty("db.user");
    }

    public static String getDbPassword() {
        return props.getProperty("db.password");
    }

    public static int getHealthCheckInterval() {
        return Integer.parseInt(props.getProperty("healthcheck.interval", "5"));
    }

    public static int getDbPoolSize() {
        return Integer.parseInt(props.getProperty("db.pool.size", "5"));
    }
}
