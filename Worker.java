import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/*
 * WHAT THIS FILE DOES:
 * --------------------
 * This is one backend server — there are 5 of them running simultaneously (ports 20001–20005).
 * Each Worker is an independent process that the LoadBalancer sends requests to.
 *
 * Startup:
 *   - Takes a port number as a command-line argument (e.g., java Worker 20001)
 *   - Creates a WorkerPool — pre-opens 20 PostgreSQL connections ready to use
 *   - Starts a ServerSocket on the given port with backlog of 200
 *
 * Request Handling:
 *   - Waits in a loop for the LoadBalancer to connect
 *   - Every incoming connection = one request
 *   - Spawns a new WorkerTask thread to handle that request
 *   - Goes back to waiting for the next connection immediately (non-blocking)
 *
 * Graceful Shutdown:
 *   - Registers a shutdown hook (triggered on Ctrl+C)
 *   - Closes the ServerSocket and all DB connections cleanly
 *
 * Flow: LoadBalancer connects → Worker accepts → spawns WorkerTask → WorkerTask queries DB → sends response back
 */
public class Worker {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Worker <port>");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);

        try {
            // Enhancement 3: Create a connection pool (size from config.properties)
            WorkerPool pool = new WorkerPool();

            // Increase backlog to 200 to handle high concurrent load from LoadBalancer
            ServerSocket workerSocket = new ServerSocket(port, 200);
            AppLogger.info("Worker started on port " + port);

            // Enhancement 4: Graceful shutdown — close pool and socket on Ctrl+C
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                AppLogger.info("Worker on port " + port + " shutting down...");
                try { 
                    workerSocket.close(); 
                    } catch (IOException ignored) {}
                pool.closeAll();
                AppLogger.info("Worker on port " + port + " shut down cleanly.");
            }, "worker-shutdown-" + port));

            while (!workerSocket.isClosed()) {
                Socket loadBalancerSocket = workerSocket.accept();
                // Enhancement 3: Pass pool to WorkerTask instead of a single connection
                Thread workerTask = new Thread(new WorkerTask(loadBalancerSocket, pool));
                workerTask.start();
            }

        } catch (IOException e) {
            // ServerSocket.close() from shutdown hook causes accept() to throw — ignore it
            if (!e.getMessage().contains("Socket closed")) {
                AppLogger.error("Worker on port " + port + " error: " + e.getMessage());
            }
        } catch (Exception e) {
            AppLogger.error("Worker on port " + port + " failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
