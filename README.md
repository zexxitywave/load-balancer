# Load Balancer in Java

A multithreaded application-level load balancer that distributes client requests across multiple worker servers. Supports **Round-Robin (Weighted)** and **Least-Connections** scheduling, with a live dashboard, connection pooling, and graceful shutdown.

## Architecture

```
Client  →  LoadBalancer (:12345)  →  Worker x5 (:20001–20005)  →  PostgreSQL DB
```

- **Client** — sends random student ID requests every 500ms, prints JSON response
- **LoadBalancer** — single entry point, picks a worker using WRR or LC scheduling
- **Worker** — queries PostgreSQL via a connection pool, returns JSON to the LB
- **PostgreSQL** — holds the `studentinfo` table

## Features

| Feature | Description |
|---|---|
| Weighted Round Robin | Workers get traffic proportional to their weight in `worker_list.txt` |
| Least Connections | Routes to the worker with the fewest active requests |
| Health Checks | Pings workers every N seconds, marks dead workers, skips them |
| Auto-restart | Automatically restarts a crashed worker process |
| Live Dashboard | Terminal table showing active load, total requests, avg latency, uptime per worker |
| Connection Pool | Each worker holds a pool of reusable DB connections (no race conditions) |
| Request Logging | Every request logged to console and `lb_requests.log` with timestamp and duration |
| Graceful Shutdown | Ctrl+C waits for in-flight requests to finish before closing |
| Config File | All settings (ports, DB credentials, pool size) in `config.properties` |

## Prerequisites

- JDK 8+
- PostgreSQL (running on port 5433)
- JAR files in `/jars/`:
  - `json-20180813.jar`
  - `postgresql-42.7.5.jar` — download from [jdbc.postgresql.org](https://jdbc.postgresql.org/download/)

## Setup

### 1. Database

Create the database and load the schema:
```powershell
psql -U postgres -p 5433 -c "CREATE DATABASE students;"
psql -U postgres -p 5433 -d students -f students.sql
```

### 2. Configuration

Edit `config.properties` with your settings:
```properties
lb.port=12345
worker.list=worker_list.txt

db.url=jdbc:postgresql://localhost:5433/students
db.user=postgres
db.password=YOUR_PASSWORD

healthcheck.interval=5
db.pool.size=5
```

### 3. Worker weights

Edit `worker_list.txt` — the third column is the weight (higher = more traffic):
```
localhost,20001,3
localhost,20002,2
localhost,20003,2
localhost,20004,1
localhost,20005,1
```

### 4. Compile

In IntelliJ press **Ctrl+F9**, or from terminal:
```powershell
javac -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar" -d "./out/production/load-balancing-java" *.java
```

## Running

Open a separate terminal for each process. Start workers first, then the LoadBalancer, then the Client.

```powershell
# Workers (one per terminal)
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" Worker 20001
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" Worker 20002
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" Worker 20003
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" Worker 20004
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" Worker 20005

# LoadBalancer — RR (Weighted Round-Robin) or LC (Least-Connections)
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" LoadBalancer RR

# Client
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" Client
```

## Project Structure

```
├── Client.java           — sends requests to the LoadBalancer
├── LoadBalancer.java     — main entry point, worker selection, health checks
├── LBRequestServer.java  — forwards requests/responses between client and worker
├── Worker.java           — accepts connections, manages DB connection pool
├── WorkerTask.java       — queries DB and returns JSON response
├── WorkerInfo.java       — holds worker metadata, stats, and ping logic
├── WorkerLoads.java      — tracks active connection counts per worker
├── WorkerDashboard.java  — live terminal stats dashboard
├── WorkerPool.java       — DB connection pool implementation
├── AppConfig.java        — reads config.properties
├── AppLogger.java        — thread-safe logger (console + file)
├── config.properties     — all runtime configuration
├── worker_list.txt       — worker host:port:weight entries
├── students.sql          — PostgreSQL schema and seed data
└── jars/                 — required JAR dependencies
```

## Sample Output

**LoadBalancer terminal:**
```
=== Load Balancer Dashboard ===
Worker   Host:Port              Status   Weight   Active   Total    Avg ms   Uptime
------------------------------------------------------------------------------------
0        localhost:20001        UP       3        1        312      11       00:02:14
1        localhost:20002        UP       2        0        208      9        00:02:14
2        localhost:20003        UP       2        1        208      10       00:02:14
3        localhost:20004        UP       1        0        104      12       00:02:14
4        localhost:20005        UP       1        0        104      11       00:02:14
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
