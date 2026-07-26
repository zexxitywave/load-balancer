import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/*
 * WHAT THIS FILE DOES:
 * --------------------
 * This is the request source — it simulates real users sending requests to the system.
 *
 * Client (main loop):
 *   - Runs forever in a while(true) loop
 *   - Every 500ms, opens a new TCP socket connection to the LoadBalancer on port 12345
 *   - Spawns a new thread (RequestSender) to handle that request
 *   - This way multiple requests can be in-flight at the same time
 *
 * RequestSender (one per request):
 *   - Picks a random student ID between 1 and 7
 *   - Sends the student ID to the LoadBalancer over the socket
 *   - Waits for the JSON response that comes back via the LoadBalancer
 *   - Parses the JSON and prints the student's full details to the console
 *
 * Flow: Client → TCP socket → LoadBalancer → Worker → PostgreSQL → back to Client
 */
public class Client {

    public static void main(String[] args) {
        try {
            while (true){

                // Open connection to Load Balancer.
                Socket loadBalancerSocket = new Socket("localhost", 12345);

                // Start a new thread to send request.
                Thread requestSender = new Thread(new RequestSender(loadBalancerSocket));
                requestSender.start();

                // Throttle request rate to avoid overwhelming the Load Balancer and exhausting resources.
                Thread.sleep(500);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class RequestSender implements Runnable{
    private Socket loadBalancerSocket;
    RequestSender(Socket loadBalancerSocket){
        this.loadBalancerSocket = loadBalancerSocket;
    }
    @Override
    public void run() {
        try {
            BufferedWriter lbWriter = new BufferedWriter(new OutputStreamWriter(loadBalancerSocket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader lbReader = new BufferedReader(new InputStreamReader(loadBalancerSocket.getInputStream(), StandardCharsets.UTF_8));

            // Get a random Student ID in range [1, 7](The number of rows).
            int sid = new Random().nextInt(7) + 1;

            // Send to Load Balancer.
            lbWriter.write(sid + "\n");
            lbWriter.flush();

            // Get worker's response, sent via Load Balancer.
            String jsonString = lbReader.readLine();
            JSONObject json = new JSONObject(jsonString);
            String result = "Information received for Student with ID="+sid+":"+
                            "\nName: "+json.getString("name")+
                            "\nDate of Birth: "+json.getString("dob")+
                            "\nMajor of Study: "+json.getString("major")+
                            "\nEducation Level: "+json.getString("level")+
                            "\nYear of Study: "+json.getString("year");
            System.out.println(result+"\n\n");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
