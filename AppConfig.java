import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Loads and exposes config.properties values.
 * All other classes read config from here instead of hardcoding values.
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
