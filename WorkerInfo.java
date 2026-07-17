import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class WorkerInfo {
    private final String host;
    private final int port;
    private final int weight;              // Enhancement 2: weighted round-robin weight

    // Health state
    private volatile boolean alive = true;
    private final Instant startTime = Instant.now();  // Enhancement 1: uptime tracking

    // Enhancement 1: live stats
    private final AtomicInteger totalHandled = new AtomicInteger(0);
    private final AtomicInteger currentLoad  = new AtomicInteger(0);
    private final AtomicLong    totalDurationMs = new AtomicLong(0);

    WorkerInfo(String host, int port, int weight) {
        this.host   = host;
        this.port   = port;
        this.weight = weight;
    }

    // ---- accessors ----
    String getHost()   { return host; }
    int    getPort()   { return port; }
    int    getWeight() { return weight; }

    public boolean isAlive()                  { return alive; }
    public void    setAlive(boolean alive)    { this.alive = alive; }

    // ---- stats ----
    public void recordRequestStart()          { currentLoad.incrementAndGet(); }
    public void recordRequestEnd(long durationMs) {
        currentLoad.decrementAndGet();
        totalHandled.incrementAndGet();
        totalDurationMs.addAndGet(durationMs);
    }

    public int  getTotalHandled()  { return totalHandled.get(); }
    public int  getCurrentLoad()   { return currentLoad.get(); }
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
