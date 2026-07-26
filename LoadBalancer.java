import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * WHAT THIS FILE DOES:
 * --------------------
 * This is the core of the system — the actual load balancer.
 * It sits between the Client and the Workers, deciding which Worker handles each request.
 *
 * Startup:
 *   - Reads worker_list.txt to discover all 5 workers (host, port, weight)
 *   - Builds a weighted sequence for WRR (e.g., weights [3,2,2,1,1] → [0,0,0,1,1,2,2,3,4])
 *   - Starts the health checker (pings workers every N seconds)
 *   - Starts the live dashboard (refreshes terminal every 2s)
 *   - Opens a ServerSocket on port 12345 (backlog: 200) to accept client connections
 *
 * Request Routing (main loop):
 *   - Accepts incoming client connections
 *   - Picks a worker using the chosen algorithm:
 *       WRR (Weighted Round-Robin): cycles through the weighted sequence
 *       LC  (Least-Connections): picks the worker with fewest active requests
 *   - Skips workers that are DOWN
 *   - Opens a socket to the selected worker
 *   - Hands both sockets (client + worker) to LBRequestServer thread to handle
 *
 * Health Checker:
 *   - Runs on a background thread every N seconds
 *   - Pings each worker — marks DOWN if unreachable, UP if recovered
 *   - Automatically restarts crashed workers using ProcessBuilder
 *
 * WorkerLoads (inner class):
 *   - Tracks active connection count per worker (used by LC algorithm)
 *   - Thread-safe using synchronized methods
 *
 * Graceful Shutdown:
 *   - On Ctrl+C, stops accepting new requests and waits up to 10s for in-flight ones to finish
 *
 * Flow: Client connects → LB picks worker → opens socket to worker → hands off to LBRequestServer
 */
public class LoadBalancer {

    private static final ArrayList<WorkerInfo> workers = new ArrayList<>();
    private static WorkerLoads workerLoads;

    // Enhancement 2: Weighted Round Robin — expanded list where each worker
    // appears weight times. e.g. weight=3 means 3 slots in the rotation.
    private static final ArrayList<Integer> weightedSequence = new ArrayList<>();
    private static int wrIndex = -1;  // current position in weightedSequence

    // Enhancement 4: track in-flight request count for graceful shutdown
    private static final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    // -------------------------------------------------------------------------
    // Enhancement 1 & previous: Health checks + auto-restart
    // -------------------------------------------------------------------------
    private static void startHealthChecker() {
        int intervalSecs = AppConfig.getHealthCheckInterval();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            for (int i = 0; i < workers.size(); i++) {
                WorkerInfo w = workers.get(i);
                boolean reachable = w.ping();
                if (!reachable && w.isAlive()) {
                    w.setAlive(false);
                    AppLogger.warn("Worker " + i + " (" + w.getHost() + ":" + w.getPort() + ") is DOWN. Attempting restart...");
                    restartWorker(i, w);
                } else if (reachable && !w.isAlive()) {
                    w.setAlive(true);
                    AppLogger.info("Worker " + i + " (" + w.getHost() + ":" + w.getPort() + ") is back UP.");
                }
            }
        }, intervalSecs, intervalSecs, TimeUnit.SECONDS);
    }

    private static void restartWorker(int index, WorkerInfo w) {
        new Thread(() -> {
            try {
                String cp = System.getProperty("java.class.path");
                ProcessBuilder pb = new ProcessBuilder("java", "-cp", cp, "Worker", String.valueOf(w.getPort()));
                pb.inheritIO();
                Process p = pb.start();
                AppLogger.info("Worker " + index + " restarted on port " + w.getPort() + " (PID: " + p.pid() + ")");
                Thread.sleep(3000);
                if (w.ping()) {
                    w.setAlive(true);
                    AppLogger.info("Worker " + index + " confirmed alive after restart.");
                }
            } catch (Exception e) {
                AppLogger.error("Failed to restart worker " + index + ": " + e.getMessage());
            }
        }, "worker-restart-" + index).start();
    }

    // -------------------------------------------------------------------------
    // Enhancement 2: Build weighted sequence from worker weights
    // e.g. workers with weights [3,2,1] → sequence [0,0,0,1,1,2]
    // -------------------------------------------------------------------------
    private static void buildWeightedSequence() {
        for (int i = 0; i < workers.size(); i++) {
            for (int w = 0; w < workers.get(i).getWeight(); w++) {
                weightedSequence.add(i);
            }
        }
        AppLogger.info("Weighted RR sequence built: " + weightedSequence);
    }

    // -------------------------------------------------------------------------
    // Main load balancer loop
    // -------------------------------------------------------------------------
    private static void startLoadBalancer(String schedAlgo) {
        try {
            // Read worker list from config
            BufferedReader workerFile = new BufferedReader(new FileReader(AppConfig.getWorkerList()));
            String line;
            while ((line = workerFile.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] info = line.split(",");
                String host  = info[0].trim();
                int    port  = Integer.parseInt(info[1].trim());
                // Enhancement 2: read optional weight (default 1 if not specified)
                int    weight = info.length >= 3 ? Integer.parseInt(info[2].trim()) : 1;
                workers.add(new WorkerInfo(host, port, weight));
            }
            workerFile.close();

            workerLoads = new WorkerLoads(workers.size());

            // Enhancement 2: build weighted sequence if RR mode
            if (schedAlgo.equals("RR")) buildWeightedSequence();

            // Health checker
            startHealthChecker();

            // Enhancement 1: Start live dashboard
            WorkerDashboard dashboard = new WorkerDashboard(workers);
            dashboard.start();

            int lbPort = AppConfig.getLBPort();
            // Increase backlog to 200 to handle high concurrent load (default is 50)
            ServerSocket balancerSocket = new ServerSocket(lbPort, 200);
            AppLogger.info("LoadBalancer started on port " + lbPort + " using " + schedAlgo + " scheduling.");

            // Enhancement 4: Graceful shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                shuttingDown.set(true);
                AppLogger.info("LoadBalancer shutting down — waiting for in-flight requests...");
                dashboard.stop();
                try { balancerSocket.close(); } catch (IOException ignored) {}
                // Give in-flight requests up to 10 seconds to finish
                try { Thread.sleep(10000); } catch (InterruptedException ignored) {}
                AppLogger.info("LoadBalancer shut down cleanly.");
            }, "lb-shutdown"));

            int currentWorker = -1;
            while (!balancerSocket.isClosed() && !shuttingDown.get()) {
                Socket clientSocket;
                try {
                    clientSocket = balancerSocket.accept();
                } catch (IOException e) {
                    if (shuttingDown.get()) break; // normal shutdown
                    throw e;
                }

                // Find next alive worker
                int attempts = 0;
                do {
                    if (schedAlgo.equals("RR")) {
                        // Enhancement 2: advance through weighted sequence
                        wrIndex = (wrIndex + 1) % weightedSequence.size();
                        currentWorker = weightedSequence.get(wrIndex);
                    } else if (schedAlgo.equals("LC")) {
                        currentWorker = workerLoads.getMinLoadServer();
                    }
                    attempts++;
                    if (attempts > workers.size()) {
                        AppLogger.error("All workers are DOWN. Dropping request.");
                        clientSocket.close();
                        currentWorker = -1;
                        break;
                    }
                } while (!workers.get(currentWorker).isAlive());

                if (currentWorker == -1) continue;

                if (schedAlgo.equals("RR")) {
                    AppLogger.info("Selected worker " + currentWorker + " (WRR, weight=" + workers.get(currentWorker).getWeight() + ").");
                } else {
                    AppLogger.info("Selected worker " + currentWorker + " with load: " + workerLoads.getLoad(currentWorker) + " (LC).");
                    workerLoads.incrementLoad(currentWorker);
                }

                Socket workerSocket = new Socket(
                    workers.get(currentWorker).getHost(),
                    workers.get(currentWorker).getPort()
                );

                // Pass workers list so LBRequestServer can update stats
                Thread lbRequestServer = new Thread(
                    new LBRequestServer(clientSocket, workerSocket, workerLoads, currentWorker, workers)
                );
                lbRequestServer.start();
            }

        } catch (IOException e) {
            AppLogger.error("LoadBalancer error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 1 || (!args[0].equals("RR") && !args[0].equals("LC"))) {
            System.err.println("Usage: java LoadBalancer <RR|LC>");
            System.err.println("  RR = Round-Robin (Weighted), LC = Least-Connections");
            System.exit(1);
        }
        startLoadBalancer(args[0]);
    }
}


class WorkerLoads {
    private final ArrayList<Integer> workerLoads = new ArrayList<>();

    WorkerLoads(int num_servers) {
        for (int i = 0; i < num_servers; i++)
            workerLoads.add(0);
    }

    int getLoad(int index) { return workerLoads.get(index); }

    synchronized int getMinLoadServer() {
        int minLoad = workerLoads.get(0), min_ind = 0;
        for (int i = 1; i < workerLoads.size(); i++) {
            if (workerLoads.get(i) < minLoad) {
                minLoad = workerLoads.get(i);
                min_ind = i;
            }
        }
        return min_ind;
    }

    synchronized void incrementLoad(int index) { workerLoads.set(index, workerLoads.get(index) + 1); }
    synchronized void decrementLoad(int index)  { workerLoads.set(index, workerLoads.get(index) - 1); }
}
