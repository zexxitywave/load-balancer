import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;

public class Worker {
    public static void main(String[] args) {
        try {
            // Load PostgreSQL JDBC Driver.
            Class.forName("org.postgresql.Driver");
            Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5433/students", "postgres", "postgres123");

            // Open socket for this worker.
            ServerSocket workerSocket = new ServerSocket(Integer.valueOf(args[0]));
            while(true){
                // Accept connection from Load Balancer.
                Socket loadBalancerSocket = workerSocket.accept();

                // Start a new thread to service this request.
                Thread workerTask = new Thread(new WorkerTask(loadBalancerSocket, conn));
                workerTask.start();
            }
        } catch (IOException | ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }
}
