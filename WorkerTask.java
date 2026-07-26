import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.json.JSONObject;

/*
 * WHAT THIS FILE DOES:
 * --------------------
 * This is the actual request handler that runs inside each Worker.
 * Worker spawns one WorkerTask thread per incoming request from the LoadBalancer.
 *
 * What it does per request:
 *   1. Reads the student ID forwarded by the LoadBalancer via the socket
 *   2. Borrows a PostgreSQL connection from the WorkerPool
 *   3. Runs a parameterized SQL query:
 *        SELECT name, dob, major, level, year FROM studentinfo WHERE sid=?
 *   4. Builds a JSON object from the result (name, dob, major, level, year)
 *   5. Sends the JSON string back to the LoadBalancer (which forwards it to the Client)
 *   6. Always returns the DB connection back to the pool in the finally block
 *
 * Key design decisions:
 *   - Uses PreparedStatement → prevents SQL injection
 *   - Borrows/returns connection in try/finally → connection always returned even on error
 *   - Empty/null student ID is handled gracefully (skips and closes socket)
 *
 * Flow: LoadBalancer sends sid → WorkerTask queries DB → builds JSON → sends back to LoadBalancer
 */
public class WorkerTask implements Runnable {
    private static final String[] columns = {"name", "dob", "major", "level", "year"};
    private final Socket loadBalancerSocket;
    private final WorkerPool pool;  // Enhancement 3: use pool instead of single connection

    WorkerTask(Socket loadBalancerSocket, WorkerPool pool) {
        this.loadBalancerSocket = loadBalancerSocket;
        this.pool = pool;
    }

    @Override
    public void run() {
        Connection conn = null;
        try {
            BufferedWriter lbWriter = new BufferedWriter(new OutputStreamWriter(loadBalancerSocket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader lbReader = new BufferedReader(new InputStreamReader(loadBalancerSocket.getInputStream(), StandardCharsets.UTF_8));

            // Get student ID sent by client (via Load Balancer).
            String sid = lbReader.readLine();

            // Socket was closed before data arrived — this is a health check ping, silently ignore.
            if (sid == null || sid.trim().isEmpty()) {
                loadBalancerSocket.close();
                return;
            }

            // Enhancement 3: Borrow a connection from the pool
            conn = pool.borrow();

            // Query student data using PreparedStatement (prevents SQL injection).
            String query = "SELECT name, dob, major, level, year FROM studentinfo WHERE sid=?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(sid.trim()));
            ResultSet rs = stmt.executeQuery();
            rs.next();

            // Build JSON response.
            JSONObject json = new JSONObject();
            for (int i = 0; i < 5; i++)
                json.put(columns[i], rs.getString(i + 1));
            rs.close();
            stmt.close();

            AppLogger.info("Worker sending info for sid=" + sid.trim());

            // Send JSON back to Load Balancer.
            lbWriter.write(json.toString() + "\n");
            lbWriter.flush();

        } catch (IOException | SQLException | InterruptedException e) {
            AppLogger.error("WorkerTask error: " + e.getMessage());
        } finally {
            // Enhancement 3: Always return the connection to the pool
            if (conn != null) pool.returnConnection(conn);

            // Receive a student ID from the Load Balancer.
            //Query the database for that student's details.
            //Convert the result into JSON.
            //Send the JSON back to the Load Balancer.
            //Return the database connection to the pool.
        }
    }
}
