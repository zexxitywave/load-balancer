import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhancement 3: Simple DB connection pool for Worker processes.
 *
 * Maintains a fixed pool of reusable JDBC connections.
 * Threads borrow a connection, use it, then return it.
 * This avoids creating a new connection per request and prevents
 * race conditions that arise from sharing a single connection across threads.
 *
 * Note: Uses a lightweight built-in pool instead of HikariCP to keep
 * the project dependency-free. For production use, swap this out for HikariCP.
 */
public class WorkerPool {

    private final List<Connection> pool     = new ArrayList<>();
    private final List<Boolean>    inUse    = new ArrayList<>();
    private final int              poolSize;
    private final String           url, user, password;

    public WorkerPool() throws SQLException, ClassNotFoundException {
        this(AppConfig.getDbPoolSize());
    }

    public WorkerPool(int poolSize) throws SQLException, ClassNotFoundException {
        this.poolSize = poolSize;
        this.url      = AppConfig.getDbUrl();
        this.user     = AppConfig.getDbUser();
        this.password = AppConfig.getDbPassword();

        Class.forName("org.postgresql.Driver");
        for (int i = 0; i < poolSize; i++) {
            pool.add(DriverManager.getConnection(url, user, password));
            inUse.add(false);
        }
        AppLogger.info("Connection pool initialized with " + poolSize + " connections.");
    }

    /**
     * Borrow a connection from the pool.
     * Blocks until one is available.
     */
    public synchronized Connection borrow() throws InterruptedException {
        while (true) {
            for (int i = 0; i < poolSize; i++) {
                if (!inUse.get(i)) {
                    // Reconnect if the connection went stale
                    try {
                        if (pool.get(i).isClosed()) {
                            pool.set(i, DriverManager.getConnection(url, user, password));
                        }
                    } catch (SQLException e) {
                        AppLogger.warn("Connection " + i + " is stale, reconnecting: " + e.getMessage());
                        try {
                            pool.set(i, DriverManager.getConnection(url, user, password));
                        } catch (SQLException ex) {
                            continue;
                        }
                    }
                    inUse.set(i, true);
                    return pool.get(i);
                }
            }
            // All connections busy — wait for one to be returned
            wait();
        }
    }

    /**
     * Return a borrowed connection back to the pool.
     */
    public synchronized void returnConnection(Connection conn) {
        for (int i = 0; i < poolSize; i++) {
            if (pool.get(i) == conn) {
                inUse.set(i, false);
                notifyAll(); // wake up any threads waiting in borrow()
                return;
            }
        }
    }

    public void closeAll() {
        for (Connection c : pool) {
            try { c.close(); } catch (SQLException ignored) {}
        }
        AppLogger.info("Connection pool closed.");
    }
}
