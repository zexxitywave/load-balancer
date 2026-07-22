import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

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

            ServerSocket workerSocket = new ServerSocket(port);
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
