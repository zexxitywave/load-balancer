import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class LBRequestServer implements Runnable {
    private final Socket clientSocket, workerSocket;
    private final WorkerLoads workerLoads;
    private final int currentServer;
    private final ArrayList<WorkerInfo> workers;  // Enhancement 1: update per-worker stats

    LBRequestServer(Socket clientSocket, Socket workerSocket, WorkerLoads workerLoads,
                    int currentServer, ArrayList<WorkerInfo> workers) {
        this.clientSocket  = clientSocket;
        this.workerSocket  = workerSocket;
        this.workerLoads   = workerLoads;
        this.currentServer = currentServer;
        this.workers       = workers;
    }

    @Override
    public void run() {
        String sid = "?";
        long startTime = System.currentTimeMillis();
        try {
            BufferedWriter workerWriter = new BufferedWriter(new OutputStreamWriter(workerSocket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedWriter clientWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader workerReader = new BufferedReader(new InputStreamReader(workerSocket.getInputStream(), StandardCharsets.UTF_8));
            BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));

            // Read student ID from client
            sid = clientReader.readLine();

            // Enhancement 1: record request start on this worker's stats
            workers.get(currentServer).recordRequestStart();

            // Forward to worker
            workerWriter.write(sid + "\n");
            workerWriter.flush();

            // Get response from worker and send to client
            String response = workerReader.readLine();
            clientWriter.write(response + "\n");
            clientWriter.flush();

            long duration = System.currentTimeMillis() - startTime;

            // Enhancement 1: record end with duration
            workers.get(currentServer).recordRequestEnd(duration);

            AppLogger.logRequest(currentServer, sid, duration);

            workerSocket.close();
            clientSocket.close();

            // Decrement load (for LC scheduling)
            workerLoads.decrementLoad(currentServer);

        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            workers.get(currentServer).recordRequestEnd(duration);
            AppLogger.error("LBRequestServer error on worker " + currentServer + ": " + e.getMessage());
        }
    }
}
