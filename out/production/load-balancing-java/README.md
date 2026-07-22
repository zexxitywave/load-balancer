# Load Balancer in Java

A multithreaded application-level load balancer built in pure Java. Distributes client requests across multiple worker servers using **Weighted Round-Robin** or **Least-Connections** scheduling. Backed by PostgreSQL, with a live terminal dashboard, per-worker connection pooling, automatic health checks, worker auto-restart, and graceful shutdown.

---

## Architecture

```
Client
  │  (sends student ID every 500ms)
  ▼
LoadBalancer :12345
  │  (picks worker using WRR or LC)
  ├──▶ Worker :20001 ──▶ PostgreSQL :5433
  ├──▶ Worker :20002 ──▶ PostgreSQL :5433
  ├──▶ Worker :20003 ──▶ PostgreSQL :5433
  ├──▶ Worker :20004 ──▶ PostgreSQL :5433
  └──▶ Worker :20005 ──▶ PostgreSQL :5433
```

Each component is fully multithreaded — the LoadBalancer, Workers, and Client all handle multiple requests concurrently.

---

## How It Works

### Client
- Every 500ms, opens a socket to the LoadBalancer on port `12345`
- Picks a random student ID (1–7) and sends it
- Receives a JSON response and prints the student's full info
- Each request runs in its own `RequestSender` thread

### LoadBalancer
- Reads `worker_list.txt` at startup to discover workers (host, port, weight)
- Listens on port `12345` for client connections
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
- Starts a connection pool of N PostgreSQL connections (configurable)
- Listens on its assigned port for LoadBalancer connections
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

---

## Project Structure

```
├── Client.java             sends requests to the LoadBalancer
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
db.pool.size=5
```

### 3. Worker weights

Edit `worker_list.txt` to adjust which workers get more traffic:
```
localhost,20001,3
localhost,20002,2
localhost,20003,2
localhost,20004,1
localhost,20005,1
```
Format: `host,port,weight`. Workers 20001–20003 get more requests than 20004–20005.

### 4. Compile

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

# Client
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" Client
```

---

## Sample Output

**LoadBalancer terminal (dashboard refreshes every 2s):**
```
=== Load Balancer Dashboard ===

Worker   Host:Port              Status   Weight   Active   Total    Avg ms   Uptime
--------------------------------------------------------------------------------------
0        localhost:20001        UP       3        1        312      11       00:02:14
1        localhost:20002        UP       2        0        208       9       00:02:14
2        localhost:20003        UP       2        1        208      10       00:02:14
3        localhost:20004        UP       1        0        104      12       00:02:14
4        localhost:20005        DOWN     1        0        104      11       00:02:14
--------------------------------------------------------------------------------------

[2026-07-17 13:30:05] [INFO ] Request handled | worker=0 | sid=3 | duration=11ms
[2026-07-17 13:30:05] [WARN ] Worker 4 (localhost:20005) is DOWN. Attempting restart...
[2026-07-17 13:30:08] [INFO ] Worker 4 confirmed alive after restart.
```

**Worker terminal:**
```
[2026-07-17 13:30:01] [INFO ] Connection pool initialized with 5 connections.
[2026-07-17 13:30:01] [INFO ] Worker started on port 20001
[2026-07-17 13:30:05] [INFO ] Worker sending info for sid=3
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

**Log file (`lb_requests.log`):**
```
[2026-07-17 13:30:05] [INFO ] Request handled | worker=0 | sid=3 | duration=11ms
[2026-07-17 13:30:06] [INFO ] Request handled | worker=1 | sid=7 | duration=9ms
[2026-07-17 13:30:06] [INFO ] Request handled | worker=0 | sid=1 | duration=12ms
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
