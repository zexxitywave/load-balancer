# Load Balancer in Java

A multithreaded application-level load balancer built in pure Java. Distributes client requests across multiple worker servers using **Weighted Round-Robin** or **Least-Connections** scheduling. Backed by PostgreSQL, with a live terminal dashboard, per-worker connection pooling, automatic health checks, worker auto-restart, and graceful shutdown.

---

## Architecture

```
Request Sources
  │
  ├── Client.java          (simulates real users — 1 req/500ms)
  └── LoadTestClient.java  (stress testing   — 50 concurrent threads)
          │
          ▼
  LoadBalancer :12345
    │  (picks worker using WRR or LC)
    ├──▶ Worker :20001 ──▶ PostgreSQL :5433
    ├──▶ Worker :20002 ──▶ PostgreSQL :5433
    ├──▶ Worker :20003 ──▶ PostgreSQL :5433
    ├──▶ Worker :20004 ──▶ PostgreSQL :5433
    └──▶ Worker :20005 ──▶ PostgreSQL :5433
```

Each component is fully multithreaded — the LoadBalancer, Workers, Client, and LoadTestClient all handle multiple requests concurrently.

---

## How It Works

### Request Sources

There are two ways requests enter the system:

**Client.java** — simulates real users
- Every 500ms, opens a socket to the LoadBalancer on port `12345`
- Picks a random student ID (1–7) and sends it
- Receives a JSON response and prints the student's full info
- Each request runs in its own `RequestSender` thread

**LoadTestClient.java** — stress/load testing
- Spawns N configurable concurrent threads (e.g. 50)
- Each thread continuously sends requests for a set duration
- Collects and reports latency percentiles (P50, P90, P95, P99), throughput, and success rate
- Used to measure system performance and identify bottlenecks

### LoadBalancer
- Reads `worker_list.txt` at startup to discover workers (host, port, weight)
- Listens on port `12345` for incoming connections (backlog: 200)
- Selects a worker using the chosen scheduling algorithm:
  - **WRR** — Weighted Round-Robin: workers with higher weight receive proportionally more requests. e.g. weights `[3,2,2,1,1]` → sequence `[0,0,0,1,1,2,2,3,4]`
  - **LC** — Least-Connections: always routes to the worker with the fewest active requests
- Skips workers marked as DOWN
- Drops the request if all workers are DOWN
- Hands off client+worker sockets to `LBRequestServer` in a new thread

### LBRequestServer
- Middleman thread inside the LoadBalancer
- Reads the student ID from the client, forwards it to the worker
- Reads the JSON response from the worker, forwards it to the client
- Records request duration and updates per-worker stats for the dashboard
- Closes both sockets after the round-trip

### Worker
- Starts a connection pool of N PostgreSQL connections (configurable via `db.pool.size`)
- Listens on its assigned port for LoadBalancer connections (backlog: 200)
- Spawns a `WorkerTask` thread per request
- Registers a shutdown hook — on Ctrl+C, closes the pool and socket cleanly

### WorkerTask
- Borrows a connection from the pool
- Runs a parameterized SQL query: `SELECT name, dob, major, level, year FROM studentinfo WHERE sid=?`
- Builds a JSON response and sends it back to the LoadBalancer
- Always returns the connection to the pool (even on error)

---

## Features

| Feature | Details |
|---|---|
| Weighted Round-Robin | Weight per worker in `worker_list.txt` — higher weight = more traffic |
| Least-Connections | Routes to the worker with fewest active requests |
| Health Checks | Pings each worker every N seconds (configurable), marks dead workers |
| Auto-restart | Crashed workers are automatically restarted via `ProcessBuilder` |
| Live Dashboard | Refreshes every 2s in the LB terminal — shows status, weight, active load, total requests, avg latency, uptime per worker |
| Connection Pool | Each Worker holds a pool of reusable DB connections — no race conditions, stale connections are auto-reconnected |
| Request Logging | Every request logged to console and `lb_requests.log` with timestamp, worker index, student ID, and duration |
| Graceful Shutdown | Ctrl+C on LoadBalancer waits up to 10s for in-flight requests; Workers close pool and socket cleanly |
| Config File | All runtime settings in `config.properties` — no hardcoded values |
| SQL Injection Prevention | All DB queries use `PreparedStatement` with parameterized inputs |
| Load Testing | Built-in `LoadTestClient` with configurable threads, duration, and real-time metrics (P50/P90/P95/P99) |

---

## Load Test Results

Load tested with 50 concurrent threads over 60 seconds:

```
Total Requests:      7,929
Successful:          7,664
Success Rate:        96.66%
Throughput:          113 requests/sec

Latency (ms):
  Min:               5 ms
  Average:           12 ms (per worker, from dashboard)
  P90:               300 ms
  P95:               436 ms
  P99:               2389 ms

Worker Distribution (Weighted Round-Robin 3:2:2:1:1):
  Worker 0 (weight=3): 2556 requests  avg 23ms
  Worker 1 (weight=2): 1704 requests  avg 26ms
  Worker 2 (weight=2): 1702 requests  avg 28ms
  Worker 3 (weight=1):  851 requests  avg 31ms
  Worker 4 (weight=1):  851 requests  avg  9ms
```

All 5 workers remained UP throughout the test. WRR distributed traffic proportionally according to weights.

---

## Project Structure

```
├── Client.java             simulates real users — sends 1 request every 500ms
├── LoadTestClient.java     stress testing — configurable concurrent threads + duration
├── LoadBalancer.java       entry point, worker selection, health checks, dashboard
├── LBRequestServer.java    forwards requests/responses, updates stats
├── Worker.java             accepts LB connections, manages connection pool
├── WorkerTask.java         queries DB, builds and sends JSON response
├── WorkerInfo.java         worker metadata, live stats (load/total/avg/uptime), ping
├── WorkerPool.java         thread-safe DB connection pool
├── WorkerDashboard.java    live terminal stats table (refreshes every 2s)
├── AppConfig.java          reads and exposes all config.properties values
├── AppLogger.java          thread-safe logger — console + lb_requests.log
├── config.properties       all runtime configuration
├── worker_list.txt         worker entries: host,port,weight
├── students.sql            PostgreSQL schema and seed data (7 students)
└── jars/
    ├── json-20180813.jar
    └── postgresql-42.7.5.jar
```

---

## Prerequisites

- JDK 8 or above
- PostgreSQL server running on port `5433`
- JAR files in the `/jars/` folder:
  - `json-20180813.jar` — already included
  - `postgresql-42.7.5.jar` — download via PowerShell:
    ```powershell
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.5/postgresql-42.7.5.jar" -OutFile "./jars/postgresql-42.7.5.jar"
    ```

---

## Setup

### 1. Database

Create the database and load the schema:
```powershell
psql -U postgres -p 5433 -c "CREATE DATABASE students;"
psql -U postgres -p 5433 -d students -f students.sql
```

Or open pgAdmin, create a database named `students`, open the Query Tool, and paste the contents of `students.sql`.

### 2. Configuration

Edit `config.properties` — update the password to match your PostgreSQL setup:

```properties
# Load Balancer
lb.port=12345
worker.list=worker_list.txt

# PostgreSQL
db.url=jdbc:postgresql://localhost:5433/students
db.user=postgres
db.password=YOUR_PASSWORD_HERE

# Health check ping interval (seconds)
healthcheck.interval=5

# DB connections per worker
# 5 workers × 20 = 100 total (requires PostgreSQL max_connections >= 110)
db.pool.size=20
```

### 3. PostgreSQL Connection Limit

If using `db.pool.size=20` with 5 workers, increase PostgreSQL's connection limit:

Find and edit `postgresql.conf`:
```
max_connections = 200
```
Then restart PostgreSQL.

### 4. Worker weights

Edit `worker_list.txt` to adjust which workers get more traffic:
```
localhost,20001,3
localhost,20002,2
localhost,20003,2
localhost,20004,1
localhost,20005,1
```
Format: `host,port,weight`. Workers 20001–20003 get more requests than 20004–20005.

### 5. Compile

In IntelliJ press **Ctrl+F9**, or from the terminal:
```powershell
javac -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar" -d "./out/production/load-balancing-java" *.java
```

---

## Running

Open a **separate terminal** for each process. Start in this order: Workers → LoadBalancer → Client.

```powershell
# Worker 1
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" Worker 20001

# Worker 2
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" Worker 20002

# Worker 3
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" Worker 20003

# Worker 4
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" Worker 20004

# Worker 5
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" Worker 20005

# LoadBalancer — pass RR (Weighted Round-Robin) or LC (Least-Connections)
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" LoadBalancer RR

# Client (real user simulation — 1 request every 500ms)
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" Client
```

---

## Load Testing

Use `LoadTestClient` to stress test the system with configurable concurrent users:

```powershell
# Usage: LoadTestClient <threads> <duration_seconds>

# Light load — 10 concurrent users, 30 seconds
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" LoadTestClient 10 30

# Medium load — 50 concurrent users, 60 seconds
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" LoadTestClient 50 60

# Heavy load — 100 concurrent users, 120 seconds
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" LoadTestClient 100 120
```

The load test reports throughput, success rate, and latency percentiles (P50, P90, P95, P99) at the end.

---

## Sample Output

**LoadBalancer terminal (dashboard refreshes every 2s):**
```
=== Load Balancer Dashboard ===

Worker   Host:Port              Status   Weight   Active   Total    Avg ms   Uptime
--------------------------------------------------------------------------------------
0        localhost:20001        UP       3        1        2556     23       00:03:04
1        localhost:20002        UP       2        0        1704     26       00:03:04
2        localhost:20003        UP       2        1        1702     28       00:03:04
3        localhost:20004        UP       1        0         851     31       00:03:04
4        localhost:20005        UP       1        0         851      9       00:03:04
--------------------------------------------------------------------------------------

[2026-07-22 23:45:42] [INFO ] Request handled | worker=0 | sid=3 | duration=1ms
[2026-07-22 23:45:42] [INFO ] Selected worker 1 (WRR, weight=2).
[2026-07-22 23:45:42] [WARN ] Worker 4 (localhost:20005) is DOWN. Attempting restart...
[2026-07-22 23:45:45] [INFO ] Worker 4 confirmed alive after restart.
```

**Worker terminal:**
```
[2026-07-22 23:45:42] [INFO ] Connection pool initialized with 20 connections.
[2026-07-22 23:45:42] [INFO ] Worker started on port 20001
[2026-07-22 23:45:43] [INFO ] Worker sending info for sid=3
```

**Client terminal:**
```
Information received for Student with ID=3:
Name: Mark Straten
Date of Birth: 5/12/96
Major of Study: Interactive Games and Media
Education Level: Undergraduate
Year of Study: Senior
```

**LoadTestClient terminal:**
```
╔════════════════════════════════════════════════════════╗
║        Load Balancer Load Test                        ║
╚════════════════════════════════════════════════════════╝

Configuration:
  Concurrent Threads: 50
  Test Duration: 60 seconds
  Target: localhost:12345

[5s]  Requests: 827  | Success: 827  | Failed: 0   | RPS: 165.40
[10s] Requests: 1106 | Success: 1106 | Failed: 0   | RPS: 110.60
[36s] Requests: 7533 | Success: 7533 | Failed: 0   | RPS: 209.25

Load Test Results
Duration: 60 seconds

Requests:
  Total Requests:      7929
  Successful:          7664
  Failed:              265
  Success Rate:        96.66%

Throughput:
  Requests/sec:        113.27

Latency (milliseconds):
  Min:                 5 ms
  Average:             12 ms
  Median (P50):        165 ms
  P90:                 300 ms
  P95:                 436 ms
  P99:                 2389 ms
  Max:                 10376 ms
```

**Log file (`lb_requests.log`):**
```
[2026-07-22 23:45:42] [INFO ] Request handled | worker=0 | sid=3 | duration=1ms
[2026-07-22 23:45:42] [INFO ] Request handled | worker=1 | sid=7 | duration=2ms
[2026-07-22 23:45:42] [INFO ] Request handled | worker=2 | sid=4 | duration=1ms
```

---

## Database Schema

```sql
CREATE TABLE studentinfo (
    sid   INTEGER PRIMARY KEY,
    name  VARCHAR(60),
    dob   VARCHAR(15),
    major VARCHAR(50),
    level VARCHAR(15),
    year  VARCHAR(20)
);
```

7 student records are pre-loaded via `students.sql`.
