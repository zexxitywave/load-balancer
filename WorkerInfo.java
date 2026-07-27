import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/*
 * WHAT THIS FILE DOES:
 * --------------------
 * This is the data model for a single Worker. One WorkerInfo object exists per worker
 * (5 total). It stores everything the LoadBalancer needs to know about a worker:
 *
 * Static info (set at startup from worker_list.txt):
 *   - host       → e.g. "localhost"
 *   - port       → e.g. 20001
 *   - weight     → e.g. 3 (used by WRR — higher weight = more traffic)
 *
 * Health state:
 *   - alive      → true if worker is UP, false if DOWN
 *   - startTime  → when this WorkerInfo was created (used to calculate uptime)
 *
 * Live stats (updated in real-time by LBRequestServer):
 *   - currentLoad    → number of requests being processed right now (AtomicInteger)
 *   - totalHandled   → total requests handled since startup (AtomicInteger)
 *   - totalDurationMs → sum of all request durations (AtomicLong)
 *   - getAvgDurationMs() → totalDurationMs / totalHandled = average latency
 *
 * Health check:
 *   - ping() → tries to open a TCP socket to this worker with a 2-second timeout
 *             → returns true if reachable, false if DOWN
 *             → called by the health checker thread every N seconds
 *
 * Thread safety:
 *   - alive uses volatile (single read/write, no compound ops needed)
 *   - stats use AtomicInteger/AtomicLong (lock-free thread-safe increments)
 */
public class WorkerInfo {
    private final String host;
    private final int port;
    private final int weight;              // Enhancement 2: weighted round-robin weight

    // Health state
    // Volatile ensures that when one thread updates alive to false, every other thread immediately sees the updated value the next time it reads alive.
    private volatile boolean alive = true;
    private final Instant startTime = Instant.now();  // Enhancement 1: uptime tracking

    // Enhancement 1: live stats
    private final AtomicInteger totalHandled = new AtomicInteger(0);
    private final AtomicInteger currentLoad  = new AtomicInteger(0);
    private final AtomicLong    totalDurationMs = new AtomicLong(0);
    // AtomicInteger is used when multiple threads need to safely update the same integer at the same time.

    WorkerInfo(String host, int port, int weight) {
        this.host   = host;
        this.port   = port;
        this.weight = weight;
    }

    // ---- accessors ----
    String getHost()   { return host; }
    int    getPort()   { return port; }
    int    getWeight() { return weight; }

    public boolean isAlive()                  {
        return alive;
    }
    public void    setAlive(boolean alive)    {
        this.alive = alive;
    }

    // ---- stats ----
    public void recordRequestStart()          {
        currentLoad.incrementAndGet();
    }
    public void recordRequestEnd(long durationMs) {
        currentLoad.decrementAndGet();
        totalHandled.incrementAndGet();
        totalDurationMs.addAndGet(durationMs);
    }

    public int  getTotalHandled()  {
        return totalHandled.get();
    }
    public int  getCurrentLoad()   {
        return currentLoad.get();
    }
    public long getAvgDurationMs() {
        int total = totalHandled.get();
        return total == 0 ? 0 : totalDurationMs.get() / total;
    }
    public String getUptime() {
        Duration d = Duration.between(startTime, Instant.now());
        return String.format("%02d:%02d:%02d", d.toHours(), d.toMinutesPart(), d.toSecondsPart());
    }

    /**
     * Try opening a socket to this worker with a short timeout.
     * Returns true if the worker is reachable, false otherwise.
     */
    public boolean ping() {
        try (Socket s = new Socket()) {
            s.connect(new java.net.InetSocketAddress(host, port), 2000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
