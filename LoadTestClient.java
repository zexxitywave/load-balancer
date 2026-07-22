import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple Load Testing Client for the Load Balancer
 * 
 * Usage: java LoadTestClient <threads> <duration_seconds>
 * Example: java LoadTestClient 50 60
 * 
 * This will spawn 50 concurrent threads sending requests for 60 seconds
 */
public class LoadTestClient {
    
    private static final String LB_HOST = "localhost";
    private static final int LB_PORT = 12345;
    
    // Statistics
    private static AtomicInteger totalRequests = new AtomicInteger(0);
    private static AtomicInteger successfulRequests = new AtomicInteger(0);
    private static AtomicInteger failedRequests = new AtomicInteger(0);
    private static List<Long> responseTimes = new CopyOnWriteArrayList<>();
    private static AtomicLong totalResponseTime = new AtomicLong(0);
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java LoadTestClient <threads> <duration_seconds>");
            System.out.println("Example: java LoadTestClient 50 60");
            System.out.println("  - threads: Number of concurrent users (e.g., 10, 50, 100)");
            System.out.println("  - duration_seconds: How long to run the test (e.g., 30, 60, 120)");
            return;
        }
        
        int numThreads = Integer.parseInt(args[0]);
        int durationSeconds = Integer.parseInt(args[1]);
        
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║        Load Balancer Load Test                        ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Configuration:");
        System.out.println("  Concurrent Threads: " + numThreads);
        System.out.println("  Test Duration: " + durationSeconds + " seconds");
        System.out.println("  Target: " + LB_HOST + ":" + LB_PORT);
        System.out.println();
        System.out.println("Starting load test...");
        System.out.println();
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (durationSeconds * 1000L);
        
        // Progress reporter thread
        Thread reporter = new Thread(() -> {
            try {
                while (System.currentTimeMillis() < endTime) {
                    Thread.sleep(5000); // Report every 5 seconds
                    long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                    int total = totalRequests.get();
                    int success = successfulRequests.get();
                    int failed = failedRequests.get();
                    double rps = total / (double) elapsed;
                    System.out.printf("[%ds] Requests: %d | Success: %d | Failed: %d | RPS: %.2f%n", 
                                      elapsed, total, success, failed, rps);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        reporter.start();
        
        // Submit load test tasks
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                Random random = new Random();
                while (System.currentTimeMillis() < endTime) {
                    try {
                        int studentId = random.nextInt(7) + 1;
                        long requestStart = System.currentTimeMillis();
                        
                        boolean success = sendRequest(studentId);
                        
                        long responseTime = System.currentTimeMillis() - requestStart;
                        
                        totalRequests.incrementAndGet();
                        if (success) {
                            successfulRequests.incrementAndGet();
                            responseTimes.add(responseTime);
                            totalResponseTime.addAndGet(responseTime);
                        } else {
                            failedRequests.incrementAndGet();
                        }
                        
                        // Small delay to avoid overwhelming the system
                        // Increased from 10ms to 20ms to reduce burst load
                        Thread.sleep(20);
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
        
        // Wait for test duration to complete
        try {
            Thread.sleep(durationSeconds * 1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Shutdown
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        
        reporter.interrupt();
        
        // Print final results
        printResults(startTime);
    }
    
    private static boolean sendRequest(int studentId) {
        try {
            // Create socket with timeout to prevent hanging
            Socket socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(LB_HOST, LB_PORT), 5000); // 5 second timeout
            socket.setSoTimeout(5000); // 5 second read timeout
            
            BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            
            // Send student ID
            writer.write(studentId + "\n");
            writer.flush();
            
            // Read response
            String response = reader.readLine();
            
            socket.close();
            return response != null && !response.isEmpty();
            
        } catch (IOException e) {
            return false;
        }
    }
    
    private static void printResults(long startTime) {
        long duration = (System.currentTimeMillis() - startTime) / 1000;
        int total = totalRequests.get();
        int success = successfulRequests.get();
        int failed = failedRequests.get();
        
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║              Load Test Results                        ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Duration: " + duration + " seconds");
        System.out.println();
        System.out.println("Requests:");
        System.out.println("  Total Requests:      " + total);
        System.out.println("  Successful:          " + success);
        System.out.println("  Failed:              " + failed);
        System.out.println("  Success Rate:        " + String.format("%.2f%%", (success * 100.0 / total)));
        System.out.println();
        System.out.println("Throughput:");
        System.out.println("  Requests/sec:        " + String.format("%.2f", total / (double) duration));
        System.out.println();
        
        if (!responseTimes.isEmpty()) {
            Collections.sort(responseTimes);
            long min = responseTimes.get(0);
            long max = responseTimes.get(responseTimes.size() - 1);
            long avg = totalResponseTime.get() / responseTimes.size();
            long median = responseTimes.get(responseTimes.size() / 2);
            long p90 = responseTimes.get((int)(responseTimes.size() * 0.90));
            long p95 = responseTimes.get((int)(responseTimes.size() * 0.95));
            long p99 = responseTimes.get((int)(responseTimes.size() * 0.99));
            
            System.out.println("Latency (milliseconds):");
            System.out.println("  Min:                 " + min + " ms");
            System.out.println("  Average:             " + avg + " ms");
            System.out.println("  Median (P50):        " + median + " ms");
            System.out.println("  P90:                 " + p90 + " ms");
            System.out.println("  P95:                 " + p95 + " ms");
            System.out.println("  P99:                 " + p99 + " ms");
            System.out.println("  Max:                 " + max + " ms");
        }
        
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println();
        System.out.println("Resume-worthy metrics:");
        if (!responseTimes.isEmpty()) {
            long p95 = responseTimes.get((int)(responseTimes.size() * 0.95));
            System.out.println("✓ Achieved " + String.format("%.2f", total / (double) duration) + " requests/sec throughput");
            System.out.println("✓ Maintained P95 latency under " + p95 + "ms");
            System.out.println("✓ " + String.format("%.2f%%", (success * 100.0 / total)) + " success rate across " + total + " requests");
        }
        System.out.println();
    }
}
