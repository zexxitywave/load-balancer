import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Enhancement 1: Live terminal dashboard.
 * Refreshes every 2 seconds and prints a table showing per-worker stats:
 *   Worker | Host:Port | Status | Weight | Active | Total | Avg ms | Uptime
 *
 * WHAT THIS FILE DOES:
 * --------------------
 * Displays a live refreshing table in the LoadBalancer terminal so you can
 * monitor all 5 workers in real-time while the system is running.
 *
 * How it works:
 *   - start() → schedules the print() method to run every 2 seconds on a background thread
 *   - print() → reads the latest stats from each WorkerInfo object and prints a formatted table
 *   - Uses ANSI escape code (\033[H\033[2J) to clear the terminal before each refresh
 *     so it looks like the table is updating in place rather than scrolling
 *   - stop() → called on LoadBalancer shutdown to stop the background scheduler
 *
 * What each column shows:
 *   Worker   → index (0–4)
 *   Host:Port → e.g. localhost:20001
 *   Status   → UP (green) or DOWN (red) based on WorkerInfo.isAlive()
 *   Weight   → WRR weight from worker_list.txt
 *   Active   → current in-flight requests on this worker right now
 *   Total    → total requests handled since startup
 *   Avg ms   → average response time in milliseconds
 *   Uptime   → how long since this WorkerInfo was created (HH:MM:SS)
 */
public class WorkerDashboard {

    private final ArrayList<WorkerInfo> workers;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public WorkerDashboard(ArrayList<WorkerInfo> workers) {
        this.workers = workers;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::print, 2, 2, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    private void print() {
        // ANSI escape: move cursor up to overwrite previous table output
        // First print clears screen; subsequent ones move up N lines to redraw in place
        String header = String.format(
            "%n%-8s %-22s %-8s %-8s %-8s %-8s %-8s %-10s%n",
            "Worker", "Host:Port", "Status", "Weight", "Active", "Total", "Avg ms", "Uptime"
        );
        String divider = "-".repeat(86) + "\n";

        StringBuilder sb = new StringBuilder();
        sb.append("\033[H\033[2J"); // clear screen (works on most terminals)
        sb.append("=== Load Balancer Dashboard ===\n");
        sb.append(header);
        sb.append(divider);

        for (int i = 0; i < workers.size(); i++) {
            WorkerInfo w = workers.get(i);
            String status = w.isAlive() ? "\033[32mUP  \033[0m" : "\033[31mDOWN\033[0m";
            sb.append(String.format(
                "%-8d %-22s %-14s %-8d %-8d %-8d %-8d %-10s%n",
                i,
                w.getHost() + ":" + w.getPort(),
                status,
                w.getWeight(),
                w.getCurrentLoad(),
                w.getTotalHandled(),
                w.getAvgDurationMs(),
                w.getUptime()
            ));
        }
        sb.append(divider);
        System.out.print(sb);
    }
}
