# Comprehensive Questions - Load Balancer Project
## (Excluding LoadBalancer.java and Worker.java)

---

# AppLogger.java Questions (35 Questions)

## Basic Understanding
1. What is the primary purpose of the AppLogger class in the load balancer system?
2. Which two output destinations does AppLogger write log entries to?
3. What is the exact format of a log entry produced by AppLogger?
4. Why is the write() method declared as synchronized in AppLogger?
5. What log file name does AppLogger use by default for persisting logs?
6. At what point in the class lifecycle does AppLogger open the log file?
7. What happens if AppLogger fails to open the log file during initialization?
8. Which Java class is used to write to the log file in append mode?
9. What DateTimeFormatter pattern is used for timestamps in AppLogger?
10. How many public logging methods does AppLogger expose, and what are they?

## Advanced Concepts
11. Explain why thread-safety is critical for a logger in a multithreaded load balancer.
12. What is the difference between info(), warn(), and error() methods in terms of output?
13. How does the logRequest() method differ from the generic info() method?
14. What three parameters does logRequest() accept, and what does each represent?
15. Why is append mode important for the log file in a production system?
16. What would happen if the synchronized keyword was removed from the write() method?
17. How does AppLogger handle the case when fileWriter is null?
18. What exception type is caught when opening the log file, and why?
19. Why is PrintWriter used instead of FileWriter directly?
20. What does the second 'true' parameter in PrintWriter constructor enable?

## Code Analysis
21. In the static initializer, what does "true" represent in `new FileWriter(LOG_FILE, true)`?
22. Why is the fileWriter field declared as static rather than instance-level?
23. What would be the consequence of not closing the PrintWriter in a logger?
24. How does the timestamp formatting ensure consistent log parsing?
25. What level strings are used for INFO, WARN, and ERROR (note the spacing)?
26. Why might the logger choose to print to both console and file simultaneously?
27. What design pattern does AppLogger exemplify (Singleton, Factory, etc.)?
28. How does AppLogger ensure that log entries from multiple threads don't interleave?
29. What would happen if two threads called info() at exactly the same time?
30. Why is LocalDateTime used instead of System.currentTimeMillis() for timestamps?

## Scenario-Based
31. If you wanted to add a DEBUG log level, what changes would you make to AppLogger?
32. How would you modify AppLogger to support log rotation (e.g., new file every day)?
33. What would happen to logging if the disk fills up while the application is running?
34. How could you extend AppLogger to support asynchronous logging for better performance?
35. If the log file gets deleted while the application is running, what would happen?

---

# HTTPLoadBalancerWrapper.java Questions (40 Questions)

## Basic Understanding
1. What problem does HTTPLoadBalancerWrapper solve in the load balancer architecture?
2. Which HTTP port does HTTPLoadBalancerWrapper listen on by default?
3. What TCP port does the wrapper connect to for forwarding requests to the LoadBalancer?
4. Name the three HTTP endpoints exposed by HTTPLoadBalancerWrapper.
5. What HTTP method is required for all endpoints in the wrapper?
6. What response is sent if a non-GET request is made to any endpoint?
7. How does the /student endpoint determine which student ID to query if none is provided?
8. What validation is performed on the student ID parameter in StudentHandler?
9. What JSON library is used to parse responses (imported at the top)?
10. Which Java package provides the HttpServer and HttpHandler classes?

## Handler Classes
11. What interface must StudentHandler implement, and why?
12. How does RandomStudentHandler differ from StudentHandler in functionality?
13. What is the purpose of the HealthHandler class?
14. What JSON response does the /health endpoint return?
15. How does StudentHandler extract the query string from the HTTP request?
16. What exception is thrown if the student ID cannot be parsed as an integer?
17. What HTTP status code is returned for a successful student query?
18. What HTTP status code is returned when the LoadBalancer is unavailable?
19. How does the wrapper handle exceptions during LoadBalancer communication?
20. What information is included in the error JSON response?

## Socket Communication
21. What type of socket connection does queryLoadBalancer() establish?
22. Which character encoding is used for socket communication?
23. How is the student ID formatted when sent to the LoadBalancer?
24. What method is used to ensure the data is sent immediately over the socket?
25. How does the wrapper read the response from the LoadBalancer?
26. What happens if the LoadBalancer doesn't respond within a reasonable time?
27. Why is try-with-resources used for socket handling?
28. What resources are automatically closed by the try-with-resources block?
29. How would you add a connection timeout to the socket in queryLoadBalancer()?
30. What would happen if the LoadBalancer returned malformed JSON?

## HTTP Response Handling
31. What Content-Type header is set for all responses?
32. Why is the Access-Control-Allow-Origin header set to "*"?
33. How is the response body converted before being sent to the client?
34. What method sends the HTTP status code and headers to the client?
35. Why is the OutputStream wrapped in a try-with-resources in sendResponse()?
36. What would happen if sendResponse() threw an IOException?
37. How could you add custom headers to the HTTP response?
38. What change would be needed to support POST requests with JSON body?
39. How would you implement request logging in the HTTP wrapper?
40. What modifications would allow the wrapper to support HTTPS?

---

# WorkerDashboard.java Questions (35 Questions)

## Basic Understanding
1. What is the primary function of the WorkerDashboard class?
2. How often does the dashboard refresh its display by default?
3. Which Java concurrency class is used for scheduling the periodic refresh?
4. What ANSI escape codes are used to clear the terminal screen?
5. How many columns are displayed in the dashboard table?
6. What data structure holds the worker information for the dashboard?
7. Which method starts the background scheduler for the dashboard?
8. How do you properly shut down the dashboard when the LoadBalancer stops?
9. What visual indicator shows a worker's UP/DOWN status?
10. Which color is used to display an UP worker versus a DOWN worker?

## Dashboard Columns
11. What information does the "Host:Port" column display?
12. How is the "Weight" value determined for each worker?
13. What does the "Active" column represent in real-time?
14. How is the "Total" column different from the "Active" column?
15. What calculation produces the "Avg ms" value?
16. How is the "Uptime" value formatted (what is the pattern)?
17. Which WorkerInfo method returns the current active load?
18. Which WorkerInfo method returns the total handled requests?
19. How does getAvgDurationMs() handle the case when no requests have been handled?
20. What Duration class methods are used to calculate uptime components?

## Scheduling Implementation
21. What type of executor is created in the WorkerDashboard constructor?
22. Why is scheduleAtFixedRate() used instead of scheduleWithFixedDelay()?
23. What is the initial delay before the first dashboard refresh?
24. What happens to the scheduler when stop() is called?
25. How does shutdownNow() differ from shutdown() for the scheduler?
26. What would happen if the print() method took longer than 2 seconds?
27. Why is a single-threaded scheduler sufficient for the dashboard?
28. How could you make the refresh rate configurable?
29. What exception might occur if print() throws an unchecked exception?
30. How does the scheduler handle exceptions in the scheduled task?

## Terminal Output
31. Why is StringBuilder used instead of multiple System.out.print() calls?
32. What is the purpose of the divider line in the dashboard output?
33. How does the ANSI escape sequence create the "updating in place" effect?
34. What terminals support the ANSI escape codes used in the dashboard?
35. How would you modify the dashboard to work on Windows CMD without ANSI support?

---

# AppConfig.java Questions (35 Questions)

## Basic Understanding
1. What design pattern does AppConfig implement in the load balancer system?
2. Which Java class is used to load and store configuration properties?
3. What file does AppConfig read configuration values from?
4. When is the config.properties file loaded during application startup?
5. What happens if config.properties cannot be found or opened?
6. How many getter methods does AppConfig expose for configuration access?
7. Which configuration value has no default fallback if missing from the file?
8. What is the default LoadBalancer port if not specified in config?
9. What is the default health check interval in seconds?
10. What is the default database connection pool size?

## Configuration Values
11. What property key retrieves the LoadBalancer port number?
12. What property key specifies the path to worker_list.txt?
13. What property key contains the JDBC connection URL?
14. What property key stores the database username?
15. What property key stores the database password?
16. Why should db.password not be hardcoded in source code?
17. What type conversion is performed on the lb.port property?
18. What exception could be thrown if lb.port contains non-numeric text?
19. How does getProperty() handle missing keys with default values?
20. What would getDbUrl() return if the db.url property is missing?

## Static Initialization
21. Why is the props field declared as static and final?
22. What is the advantage of using a static initializer block?
23. How many times is the config.properties file read during application lifetime?
24. What happens to other classes trying to use AppConfig if loading fails?
25. Why does the static initializer call System.exit(1) on failure?
26. Could the props object be modified after initialization? How?
27. What thread-safety guarantees does the static initializer provide?
28. How would you reload configuration without restarting the application?
29. What security concern exists with storing passwords in plain text properties?
30. How could you encrypt the database password in config.properties?

## Best Practices
31. Why is it better to use AppConfig than reading properties directly in each class?
32. What would happen if you added a new property but forgot to add a getter?
33. How does the default value pattern improve application robustness?
34. What configuration management improvements would you suggest for production?
35. How could you support multiple configuration files for different environments?

---

# WorkerInfo.java Questions (40 Questions)

## Basic Understanding
1. What is the purpose of the WorkerInfo class in the load balancer?
2. How many WorkerInfo objects exist in a typical 5-worker setup?
3. What three pieces of static information are stored per worker?
4. Which field tracks whether a worker is currently healthy?
5. What data type is used for the alive field and why?
6. When is the startTime field initialized for a WorkerInfo object?
7. Which class provides thread-safe integer operations for statistics?
8. Which class provides thread-safe long operations for duration tracking?
9. What method attempts to verify if a worker is reachable?
10. What timeout value is used when pinging a worker?

## Static Information Fields
11. How is the worker's hostname stored in WorkerInfo?
12. What integer value represents the worker's listening port?
13. What does the weight field control in the load balancing algorithm?
14. Are host, port, and weight fields mutable after construction?
15. What constructor parameters are required to create a WorkerInfo?
16. Why are there no setter methods for host, port, and weight?
17. How does the weight value affect traffic distribution in WRR?
18. What accessor method returns the worker's hostname?
19. What accessor method returns the worker's port number?
20. What accessor method returns the configured weight?

## Health State Management
21. What does the volatile keyword guarantee for the alive field?
22. Why is volatile sufficient for the alive field (not AtomicInteger)?
23. How is the alive field updated when a worker goes DOWN?
24. How is the alive field updated when a worker recovers?
25. What method checks if a worker is currently alive?
26. What method sets the alive status to a new value?
27. How does the ping() method determine worker reachability?
28. What exception types are caught silently in the ping() method?
29. What does ping() return if the socket connection succeeds?
30. What does ping() return if any exception occurs during connection?

## Live Statistics
31. What does recordRequestStart() do to the currentLoad counter?
32. What two operations does recordRequestEnd() perform?
33. How is the average duration calculated in getAvgDurationMs()?
34. What does getAvgDurationMs() return if totalHandled is zero?
35. Why is totalDurationMs an AtomicLong instead of AtomicInteger?
36. How does getCurrentLoad() help the Least-Connections algorithm?
37. What does getTotalHandled() represent over the worker's lifetime?
38. How is the uptime string formatted in getUptime()?
39. Which Duration method extracts hours from the elapsed time?
40. Which Duration method extracts minutes excluding hours (Java 9+)?

---

# Client.java Questions (35 Questions)

## Basic Understanding
1. What role does the Client class play in the load balancer system?
2. How frequently does Client send new requests to the LoadBalancer?
3. What port does Client connect to for reaching the LoadBalancer?
4. How does Client simulate multiple concurrent users?
5. What range of student IDs does Client randomly select from?
6. Which external library is used for JSON parsing in Client?
7. What class implements the actual request sending logic?
8. How many threads can be running RequestSender simultaneously?
9. What happens if the LoadBalancer is not running when Client starts?
10. Why does Client run in an infinite loop?

## Request Flow
11. What type of network connection does Client establish?
12. How is the random student ID generated in RequestSender?
13. What format is used to send the student ID to the LoadBalancer?
14. Which stream class writes data to the socket output?
15. Which stream class reads data from the socket input?
16. What character encoding is specified for the streams?
17. Why is flush() called after writing the student ID?
18. What method reads the complete JSON response line?
19. What five fields are extracted from the JSON response?
20. How is the result formatted for console display?

## Threading Model
21. Why is a new Thread created for each request?
22. What interface does RequestSender implement?
23. How does the RequestSender constructor receive the socket?
24. What happens to the main thread while RequestSender threads run?
25. Why doesn't Client wait for RequestSender to complete?
26. How could unlimited thread creation cause resource exhaustion?
27. What would happen if Thread.sleep(500) was removed?
28. How could you limit the maximum concurrent requests?
29. Why might using an ExecutorService be better than manual threads?
30. What exception is caught in the main loop, and how is it handled?

## Error Handling
31. What exception type is caught in RequestSender's run() method?
32. What happens to the socket if an IOException occurs during request?
33. How would you add retry logic for failed requests?
34. What improvement would you make to handle LoadBalancer downtime gracefully?
35. How could you add request timeout handling to prevent hanging?

---

# WorkerTask.java Questions (40 Questions)

## Basic Understanding
1. What is the responsibility of the WorkerTask class?
2. When is a new WorkerTask instance created?
3. Which interface does WorkerTask implement?
4. What two constructor parameters does WorkerTask require?
5. What SQL query does WorkerTask execute against PostgreSQL?
6. How many columns are retrieved from the studentinfo table?
7. What are the five column names queried from the database?
8. Which class prevents SQL injection in the database query?
9. What JSON library is used to build the response?
10. How does WorkerTask obtain a database connection?

## Database Operations
11. From where does WorkerTask borrow a database connection?
12. Why is PreparedStatement used instead of Statement?
13. How is the student ID parameter set in the prepared statement?
14. What method executes the SELECT query?
15. What assumption is made about the query result (rs.next())?
16. How are the five column values extracted from ResultSet?
17. Why is rs.close() called explicitly?
18. Why is stmt.close() called explicitly?
19. In which block is the connection returned to the pool?
20. What happens if an SQLException occurs during query execution?

## Socket Communication
21. What two stream types are created for socket I/O?
22. Which character encoding is used for socket communication?
23. How is the student ID received from the LoadBalancer?
24. What condition indicates a health check ping (not a real request)?
25. What action is taken if sid is null or empty?
26. How is the JSON response sent back to the LoadBalancer?
27. Why is a newline character appended to the JSON response?
28. What method ensures immediate transmission of the response?
29. What log message is written before sending the response?
30. What happens to the socket after the response is sent?

## Connection Pool Integration
31. Why is WorkerPool passed as a constructor parameter?
32. When is borrow() called in the request lifecycle?
33. When is returnConnection() guaranteed to be called?
34. What design pattern ensures connection return even on errors?
35. What would happen if the finally block was omitted?
36. How does connection pooling improve performance vs. creating new connections?
37. What exception types can borrow() throw?
38. Why is InterruptedException a possible outcome from borrow()?
39. How would you handle the case when the pool is exhausted?
40. What enhancement does WorkerPool provide over single-connection Workers?

---

# LBRequestServer.java Questions (40 Questions)

## Basic Understanding
1. What role does LBRequestServer play in the load balancer architecture?
2. How many sockets does each LBRequestServer instance manage?
3. What are the two socket types managed by LBRequestServer?
4. When is a new LBRequestServer thread created?
5. Which interface does LBRequestServer implement?
6. What four constructor parameters does LBRequestServer require?
7. What does the currentServer parameter represent?
8. Why is the workers ArrayList passed to LBRequestServer?
9. What interface defines the contract for workerLoads?
10. How does LBRequestServer enable concurrent request handling?

## Request Processing Flow
11. What is recorded at the very beginning of request processing?
12. Which method reads the student ID from the client?
13. When is recordRequestStart() called on the worker's stats?
14. How is the student ID forwarded to the selected Worker?
15. Which method reads the Worker's JSON response?
16. How is the response sent back to the original Client?
17. When is the request duration calculated?
18. Which method records the request completion with duration?
19. What logger method is called to log the completed request?
20. In what order are the two sockets closed?

## Stream Management
21. How many BufferedWriter instances are created per request?
22. How many BufferedReader instances are created per request?
23. What character encoding is used for all stream operations?
24. Why are separate readers/writers needed for client and worker sockets?
25. What would happen if you mixed up clientWriter and workerWriter?
26. How does the newline character affect message boundaries?
27. What exception is thrown if socket I/O fails?
28. How are IOExceptions handled in the catch block?
29. Is the request duration still recorded if an exception occurs?
30. What log message is written when an error occurs?

## Statistics Tracking
31. Which WorkerInfo method increments the active load counter?
32. Which WorkerInfo method decrements active load and records duration?
33. Why must recordRequestStart() be called before forwarding to worker?
34. Why must recordRequestEnd() be called in both try and catch blocks?
35. What does decrementLoad() do on the WorkerLoads object?
36. Why is decrementLoad() necessary for Least-Connections scheduling?
37. How does accurate load tracking affect future request routing?
38. What would happen if recordRequestEnd() was never called?
39. How does the dashboard reflect the currentLoad changes?
40. Why is timing started before reading from client (not after)?

## Thread Safety
41. Can multiple LBRequestServer threads access the same WorkerInfo?
42. Why don't LBRequestServer methods need synchronization?
43. How do AtomicInteger and AtomicLong ensure thread-safe stats updates?
44. What race condition could occur without atomic operations?
45. Why is the workers list safe to read from multiple threads?
46. How does LBRequestServer contribute to overall system throughput?
47. What limits the maximum concurrent LBRequestServer threads?
48. How could you add request queuing if too many threads are created?
49. What would happen if LBRequestServer blocked instead of being threaded?
50. How does this design support the LoadBalancer's non-blocking acceptance?

---

# WorkerPool.java Questions (45 Questions)

## Basic Understanding
1. What problem does WorkerPool solve in the Worker architecture?
2. How many database connections does a WorkerPool manage?
3. Which Java collection stores the actual Connection objects?
4. Which Java collection tracks which connections are currently in use?
5. What three configuration values are read from AppConfig?
6. Which JDBC driver class is loaded dynamically?
7. What exception types can the constructor throw?
8. How many constructor overloads does WorkerPool provide?
9. What default constructor behavior delegates to the parameterized version?
10. What log message confirms successful pool initialization?

## Connection Pool Structure
11. What is the relationship between pool and inUse lists?
12. Why are two separate lists needed instead of one?
13. What boolean value indicates a connection is available?
14. What boolean value indicates a connection is currently borrowed?
15. How does the pool size relate to config.properties?
16. What happens during construction for each pool slot?
17. Why is DriverManager.getConnection() called poolSize times?
18. What initial state is set for each inUse flag?
19. Could the pool contain null connections after initialization?
20. What would happen if PostgreSQL was down during WorkerPool construction?

## Borrow Operation
21. What keyword makes borrow() thread-safe?
22. What does borrow() return to the caller?
23. What loop structure does borrow() use to find available connections?
24. What condition is checked before returning a connection?
25. What check is performed on the connection before handing it out?
26. What happens if a connection is found to be closed?
27. What exception is caught when checking isClosed()?
28. What log message is written when a stale connection is detected?
29. What happens if reconnection also fails?
30. What method is called when all connections are busy?

## Wait/Notify Mechanism
31. Why does borrow() call wait() in a while loop?
32. What object's monitor is used for wait()/notifyAll()?
33. What wakes up a thread waiting in borrow()?
34. Why is notifyAll() used instead of notify()?
35. What would happen if wait() was called outside a synchronized block?
36. What exception can interrupt a waiting thread?
37. How does the while loop handle spurious wakeups?
38. What guarantees that a connection will eventually become available?
39. Could a deadlock occur in the borrow/return cycle?
40. How does this implementation prevent busy-waiting?

## Return Operation
41. What keyword makes returnConnection() thread-safe?
42. How does returnConnection() find which connection to mark free?
43. What boolean value is set when returning a connection?
44. Why is notifyAll() called after marking the connection free?
45. What would happen if returnConnection() wasn't synchronized?

## Pool Shutdown
46. What method closes all connections in the pool?
47. Does closeAll() check if connections are already closed?
48. What exception type is caught and ignored during closeAll()?
49. What log message confirms pool closure?
50. When should closeAll() be called in the Worker lifecycle?

## Production Considerations
51. Why does the comment mention HikariCP as a production alternative?
52. What advantages would HikariCP provide over this simple pool?
53. How would you add connection validation before borrowing?
54. What improvement would you add to track pool utilization metrics?
55. How could you implement idle connection timeout/eviction?

---

# LoadTestClient.java Questions (50 Questions)

## Basic Understanding
1. What is the primary purpose of LoadTestClient?
2. What two command-line arguments does LoadTestClient require?
3. What does the first argument (threads) control?
4. What does the second argument (duration_seconds) control?
5. What default usage message is shown if arguments are missing?
6. Which executor type is used to manage test threads?
7. How many statistical counters are maintained during testing?
8. What data structure stores individual response times?
9. Why is CopyOnWriteArrayList chosen for responseTimes?
10. What atomic class tracks total response time accumulation?

## Test Configuration
11. What host does LoadTestClient target by default?
12. What port does LoadTestClient connect to?
13. How is numThreads parsed from command-line args?
14. How is durationSeconds parsed from command-line args?
15. What ASCII art banner is printed at test start?
16. What three configuration values are displayed before testing?
17. How is endTime calculated from startTime and duration?
18. What TimeUnit is used for time calculations?
19. How would you add a third argument for custom target host?
20. What validation should be added for negative thread counts?

## Thread Pool Execution
21. What Executors method creates the thread pool?
22. How many tasks are submitted to the executor?
23. What lambda expression defines each load test task?
24. What condition controls the inner request loop?
25. How is the random student ID generated per request?
26. When is requestStart timestamp captured?
27. What method actually sends the request to LoadBalancer?
28. How is responseTime calculated after request completion?
29. Which counter is incremented for every attempt?
30. Which counter is incremented only on successful requests?

## Statistics Collection
31. When is a response time added to the responseTimes list?
32. When is totalResponseTime updated?
33. Which counter tracks failed requests?
34. What sleep duration is used between requests and why?
35. Why was the delay increased from 10ms to 20ms?
36. What exception causes the task loop to break early?
37. How does Thread.currentThread().interrupt() help?
38. What would happen without the sleep delay?
39. How could you make the delay configurable?
40. Why is Random instantiated once per thread, not per request?

## Progress Reporter
41. What is the purpose of the reporter thread?
42. How often does the reporter print progress updates?
43. What four metrics are shown in each progress line?
44. How is RPS (requests per second) calculated?
45. What condition ends the reporter loop?
46. Why is reporter.start() called before submitting tasks?
47. What exception is caught in the reporter loop?
48. How is the reporter thread stopped after test completion?
49. What formatting is used for the progress output?
50. How could you add real-time graphing of progress?

## Request Sending
51. What socket construction allows timeout configuration?
52. What connect timeout is set for establishing connections?
53. What read timeout is set for receiving responses?
54. What return value indicates a successful request?
55. What return value indicates a failed request?
56. Which exception type causes sendRequest() to return false?
57. What condition checks for empty/null response?
58. Why is socket.close() called explicitly?
59. How would you add retry logic to sendRequest()?
60. What improvement would connection pooling bring?

## Results Calculation
61. When is printResults() called?
62. How is test duration calculated in seconds?
63. What formula calculates success rate percentage?
64. What formula calculates overall throughput?
65. What preprocessing is done on responseTimes before percentile calculation?
66. How is the median (P50) index calculated?
67. How is the P90 index calculated?
68. How is the P95 index calculated?
69. How is the P99 index calculated?
70. What happens if responseTimes is empty?

## Output Formatting
71. What ASCII border surrounds the results section?
72. What six latency metrics are reported?
73. How are floating-point values formatted in output?
74. What "resume-worthy" metrics are highlighted at the end?
75. How could you export results to CSV format?
76. How could you generate an HTML report?
77. What additional metrics would be valuable to track?
78. How would you compare results across multiple test runs?
79. What visualization would help understand latency distribution?
80. How could you integrate with monitoring systems?

## Graceful Shutdown
81. What method initiates executor shutdown?
82. How long does awaitTermination() wait for tasks to complete?
83. What happens if tasks don't complete within the timeout?
84. Why is shutdownNow() called on timeout?
85. What cleanup is needed after executor termination?
86. How would you handle SIGINT (Ctrl+C) during testing?
87. What resources need explicit cleanup?
88. How could you save partial results if test is interrupted?
89. What shutdown hook would you add for graceful termination?
90. How would you implement a warm-up period before measuring?

## Advanced Scenarios
91. How would you implement ramp-up (gradual thread increase)?
92. How would you add different request patterns (not just random)?
93. How would you test specific worker failure scenarios?
94. How would you measure server-side vs. network latency?
95. How would you add distributed load testing across multiple machines?
96. How would you implement automated performance regression detection?
97. How would you add custom request payloads?
98. How would you test WebSocket connections instead of TCP?
99. How would you integrate with CI/CD pipelines?
100. What security considerations exist for load testing tools?

---

# config.properties Questions (25 Questions)

## File Structure
1. What format is used for configuration in config.properties?
2. What character denotes a comment line in the properties file?
3. How many configuration sections are logically organized in the file?
4. What separator is used between keys and values?
5. Are spaces around the equals sign significant?

## Load Balancer Settings
6. What property defines the LoadBalancer listening port?
7. What is the default LoadBalancer port value?
8. What property specifies the worker list file path?
9. Can worker.list reference a file in a different directory?
10. How would you configure multiple LoadBalancer instances?

## Database Settings
11. What property contains the PostgreSQL JDBC URL?
12. What port does the JDBC URL specify for PostgreSQL?
13. What database name is used in the JDBC URL?
14. What property stores the database username?
15. What property stores the database password?
16. Why should the password be changed before committing to version control?
17. What JDBC driver is implied by the "jdbc:postgresql" prefix?
18. How would you configure SSL for the database connection?
19. What additional parameters could be added to the JDBC URL?
20. How would you configure connection to a different PostgreSQL host?

## Health Check Settings
21. What property controls how often workers are pinged?
22. What unit is used for healthcheck.interval?
23. What is the default health check interval?
24. What would happen if you set healthcheck.interval=1?
25. What would happen if you set healthcheck.interval=300?

## Connection Pool Settings
26. What property controls the number of DB connections per worker?
27. What is the recommended pool size in the comments?
28. Why does the comment mention "5 workers × 20 = 100 total"?
29. What PostgreSQL setting must be increased for larger pools?
30. How would you calculate optimal pool size for your system?

## Security & Best Practices
31. Why shouldn't config.properties with real passwords be committed to Git?
32. What file should you add to .gitignore?
33. How could you use environment variables instead of hardcoded passwords?
34. What encryption could protect sensitive configuration values?
35. How would you support different configs for dev/staging/production?
36. What validation should be performed on property values?
37. How could you detect missing required properties at startup?
38. What backup strategy should you use for configuration files?
39. How would you implement configuration hot-reloading?
40. What documentation should accompany config.properties changes?

---

# worker_list.txt Questions (20 Questions)

## File Format
1. What format is used for each line in worker_list.txt?
2. What three values are specified per worker entry?
3. What character separates the three values?
4. How many workers are configured in the default file?
5. What hostname is used for all default workers?

## Port Configuration
6. What port does Worker 1 listen on?
7. What port does Worker 2 listen on?
8. What port does Worker 3 listen on?
9. What port does Worker 4 listen on?
10. What port does Worker 5 listen on?
11. What pattern do the port numbers follow?
12. Could you use non-sequential ports?

## Weight Configuration
13. What weight is assigned to Worker 1 (port 20001)?
14. What weight is assigned to Worker 2 (port 20002)?
15. What weight is assigned to Worker 3 (port 20003)?
16. What weight is assigned to Worker 4 (port 20004)?
17. What weight is assigned to Worker 5 (port 20005)?
18. What traffic ratio does [3,2,2,1,1] produce?
19. How would you configure equal distribution among all workers?
20. What would happen if you set a worker's weight to 0?

## Advanced Configuration
21. How would you add a sixth worker to the configuration?
22. Could you use different hostnames for different workers?
23. What validation should be performed on worker entries?
24. How would you temporarily disable a worker without removing it?
25. What happens if worker_list.txt contains duplicate entries?

---

# README.md Questions (30 Questions)

## Architecture Understanding
1. What two request sources can initiate traffic to the LoadBalancer?
2. What scheduling algorithms does the LoadBalancer support?
3. How many Worker instances are typically deployed?
4. What database backend do Workers connect to?
5. What port does the LoadBalancer listen on?
6. What port range do Workers listen on?
7. What component acts as the "middleman" for each request?
8. How does Client simulate real user behavior?
9. What metrics does LoadTestClient collect?
10. What does WRR stand for and how does it work?

## Feature Comprehension
11. What does the live dashboard display every 2 seconds?
12. How does automatic worker restart work?
13. What mechanism prevents SQL injection attacks?
14. Where are request logs persisted?
15. What triggers graceful shutdown?
16. How long does LoadBalancer wait for in-flight requests?
17. What file contains all runtime configuration?
18. What percentiles does LoadTestClient report?
19. How are stale database connections handled?
20. What ANSI feature enables the updating dashboard?

## Load Test Results
21. What was the total requests in the sample load test?
22. What success rate was achieved in the sample test?
23. What throughput (req/sec) was measured?
24. What was the P95 latency in milliseconds?
25. What was the P99 latency in milliseconds?
26. How were requests distributed across workers with WRR?
27. Did all workers remain UP throughout the test?
28. What was the average latency per worker?
29. What weight distribution was used in the test?
30. How would you interpret these results for a resume bullet?

---

# LOAD_TEST_QUICK_START.md Questions (25 Questions)

## Quick Start Guide
1. What three prerequisites must be running before load testing?
2. What PowerShell script automates the load test process?
3. What four steps does the automation script perform?
4. What javac command compiles LoadTestClient?
5. What java command runs a light load test?

## Test Scenarios
6. What parameters define a "Light Load" test?
7. What parameters define a "Medium Load" test?
8. What parameters define a "Heavy Load" test?
9. What parameters define a "Stress Test"?
10. What is the purpose of running tests in sequence?

## Understanding Output
11. What does the "RPS" metric represent?
12. What four values are shown in progress updates?
13. What seven latency metrics are reported in final results?
14. What does P95 latency mean practically?
15. What does "resume-worthy metrics" section provide?

## Monitoring & Tuning
16. What three things should you watch during testing?
17. What tuning option addresses high latency first?
18. What tuning option addresses connection timeouts?
19. What should you check if workers go DOWN?
20. What PostgreSQL setting affects connection capacity?

## Resume Bullets
21. What four example bullet styles are provided?
22. What metrics should you capture for resume bullets?
23. How do you fill in the [X], [Y], [Z] placeholders?
24. What technical keywords help with ATS systems?
25. What are the six tips for strong resume bullets?

---

# LOAD_TEST_IMPROVEMENTS.md Questions (25 Questions)

## Failure Analysis
1. What was the original success rate before improvements?
2. What was identified as the primary cause of failures?
3. How many DB connections did each worker originally have?
4. What was the total system DB connection capacity?
5. Why did connection pool exhaustion cause failures?

## Root Causes
6. What problem existed with socket timeouts originally?
7. What was the original request delay between iterations?
8. Why was the original delay too aggressive?
9. What TCP queue issue contributed to failures?
10. What file descriptor concern exists on Windows?

## Improvements Made
11. What change was made to db.pool.size?
12. By what factor was the pool size increased?
13. What two timeout configurations were added to sockets?
14. What sleep duration replaced the original 10ms?
15. What LoadBalancer improvement is suggested optionally?

## Expected Results
16. What success rate improvement is expected?
17. What throughput trade-off is acceptable?
18. Why is slightly lower throughput acceptable?
19. What stability improvement is anticipated?
20. How do you restart workers to pick up new config?

## Production Recommendations
21. What connection pooling improvement is suggested for LB?
22. What pattern prevents cascading failures?
23. What adaptive feature could improve routing?
24. What queuing strategy could smooth bursts?
25. What monitoring capability should production have?

---

# JMETER_LOAD_TEST_GUIDE.md Questions (25 Questions)

## JMeter Setup
1. Where do you download Apache JMeter?
2. What additional component is needed for JMeter to work with this LB?
3. What port does HTTPLoadBalancerWrapper listen on?
4. What compile command builds the HTTP wrapper?
5. What curl command tests the wrapper manually?

## Test Plan Creation
6. What JMeter component defines concurrent users?
7. What three Thread Group settings control load?
8. What HTTP Request settings target the wrapper?
9. What four Listeners help visualize results?
10. What assertion verifies valid JSON response?

## Test Execution
11. How do you launch JMeter GUI?
12. What command runs tests in non-GUI mode?
13. What output file format stores raw results?
14. How do you generate HTML reports?
15. Where do you open the HTML report?

## Test Scenarios
16. What thread count defines "Light Load"?
17. What thread count defines "Heavy Load"?
18. What scenario tests sudden traffic spikes?
19. What ramp-up prevents instant overload?
20. What duration is recommended for stress testing?

## Analysis & Troubleshooting
21. What five key metrics should you analyze?
22. What does Aggregate Report show that Summary doesn't?
23. What causes "Connection refused" errors?
24. What causes high error rates?
25. What resume-worthy metrics should you capture?

---

# RESUME_BULLETS.md Questions (20 Questions)

## Bullet Point Structure
1. What three bullet point options are initially recommended?
2. What distinguishes Option 1 from Option 2?
3. What focus does Option 3 emphasize?
4. What metrics template is provided for recording results?
5. What four test scenarios have templates?

## Metrics to Capture
6. What six metrics should you record per test?
7. What placeholder format is used for customization?
8. What three updated bullet versions incorporate metrics?
9. What seven key metrics are highlighted for resumes?
10. What technical keyword categories are listed?

## ATS Optimization
11. What load balancing keywords help ATS?
12. What concurrency keywords help ATS?
13. What database keywords help ATS?
14. What reliability keywords help ATS?
15. What monitoring keywords help ATS?
16. What performance keywords help ATS?

## Best Practices
17. What six tips are given for strong bullets?
18. What action verbs are recommended?
19. Why is quantification important?
20. What balance should bullets achieve?

---

## Total Question Count Summary

| File | Question Count |
|------|---------------|
| AppLogger.java | 35 |
| HTTPLoadBalancerWrapper.java | 40 |
| WorkerDashboard.java | 35 |
| AppConfig.java | 35 |
| WorkerInfo.java | 40 |
| Client.java | 35 |
| WorkerTask.java | 40 |
| LBRequestServer.java | 50 |
| WorkerPool.java | 55 |
| LoadTestClient.java | 100 |
| config.properties | 40 |
| worker_list.txt | 25 |
| README.md | 30 |
| LOAD_TEST_QUICK_START.md | 25 |
| LOAD_TEST_IMPROVEMENTS.md | 25 |
| JMETER_LOAD_TEST_GUIDE.md | 25 |
| RESUME_BULLETS.md | 20 |
| **TOTAL** | **650+** |

Note: This comprehensive question set covers conceptual understanding, code analysis, best practices, troubleshooting, and advanced scenarios for each file in the load balancer project (excluding LoadBalancer.java and Worker.java as requested).
