import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * HTTP wrapper for the TCP-based Load Balancer.
 * Allows JMeter and other HTTP-based load testing tools to test the system.
 * 
 * Usage:
 * 1. Start your Workers and LoadBalancer as usual
 * 2. Run this wrapper: java HTTPLoadBalancerWrapper
 * 3. Access via HTTP: http://localhost:8080/student?id=3
 * 4. Or random student: http://localhost:8080/student/random
 */
public class HTTPLoadBalancerWrapper {
    
    private static final int HTTP_PORT = 8080;
    private static final String LB_HOST = "localhost";
    private static final int LB_PORT = 12345;
    
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);
        
        // Endpoint: /student?id=X or /student/random
        server.createContext("/student", new StudentHandler());
        server.createContext("/student/random", new RandomStudentHandler());
        server.createContext("/health", new HealthHandler());
        
        server.setExecutor(null); // Use default executor
        server.start();
        
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║   HTTP Load Balancer Wrapper Started                  ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("HTTP Server running on: http://localhost:" + HTTP_PORT);
        System.out.println("Forwarding to Load Balancer: " + LB_HOST + ":" + LB_PORT);
        System.out.println();
        System.out.println("Endpoints:");
        System.out.println("  GET /student?id=<1-7>     - Get specific student");
        System.out.println("  GET /student/random       - Get random student");
        System.out.println("  GET /health               - Health check");
        System.out.println();
        System.out.println("Example JMeter URL: http://localhost:8080/student/random");
        System.out.println();
        System.out.println("Press Ctrl+C to stop...");
    }
    
    static class StudentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            
            try {
                // Parse student ID from query parameter
                String query = exchange.getRequestURI().getQuery();
                int studentId;
                
                if (query != null && query.startsWith("id=")) {
                    studentId = Integer.parseInt(query.substring(3));
                    if (studentId < 1 || studentId > 7) {
                        sendResponse(exchange, 400, "{\"error\":\"Student ID must be between 1 and 7\"}");
                        return;
                    }
                } else {
                    // Random if no ID specified
                    studentId = new Random().nextInt(7) + 1;
                }
                
                // Forward request to Load Balancer
                String response = queryLoadBalancer(studentId);
                sendResponse(exchange, 200, response);
                
            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, "{\"error\":\"Invalid student ID\"}");
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\":\"Load Balancer unavailable: " + e.getMessage() + "\"}");
            }
        }
    }
    
    static class RandomStudentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            
            try {
                int studentId = new Random().nextInt(7) + 1;
                String response = queryLoadBalancer(studentId);
                sendResponse(exchange, 200, response);
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\":\"Load Balancer unavailable: " + e.getMessage() + "\"}");
            }
        }
    }
    
    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "{\"status\":\"ok\",\"service\":\"http-lb-wrapper\"}";
            sendResponse(exchange, 200, response);
        }
    }
    
    private static String queryLoadBalancer(int studentId) throws IOException {
        try (Socket socket = new Socket(LB_HOST, LB_PORT);
             BufferedWriter writer = new BufferedWriter(
                 new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(
                 new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            
            // Send student ID to Load Balancer
            writer.write(studentId + "\n");
            writer.flush();
            
            // Read JSON response from Load Balancer
            String jsonResponse = reader.readLine();
            return jsonResponse;
        }
    }
    
    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
