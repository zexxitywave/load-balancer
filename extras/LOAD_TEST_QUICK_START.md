# Load Test Quick Start Guide

## Simple Manual Load Testing (No JMeter Required!)

This guide uses a custom Java load test client - no external tools needed.

---

## Prerequisites

1. ✅ Workers running (ports 20001-20005)
2. ✅ LoadBalancer running (port 12345)
3. ✅ PostgreSQL database running

---

## Option 1: Automated Script (Easiest)

Just run this PowerShell script:

```powershell
.\run_load_test.ps1
```

It will:
- Compile the load test client
- Check if LoadBalancer is running
- Ask you for test parameters
- Run the load test
- Show detailed results

---

## Option 2: Manual Commands

### Step 1: Compile

```powershell
javac -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar" -d "./out/production/load-balancing-java" LoadTestClient.java
```

### Step 2: Run Load Test

**Light Load (10 concurrent users, 30 seconds):**
```powershell
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" LoadTestClient 10 30
```

**Medium Load (50 concurrent users, 60 seconds):**
```powershell
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" LoadTestClient 50 60
```

**Heavy Load (100 concurrent users, 120 seconds):**
```powershell
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" LoadTestClient 100 120
```

**Stress Test (200 concurrent users, 180 seconds):**
```powershell
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" LoadTestClient 200 180
```

---

## Understanding the Output

### During Test:
```
[5s] Requests: 245 | Success: 245 | Failed: 0 | RPS: 49.00
[10s] Requests: 498 | Success: 498 | Failed: 0 | RPS: 49.80
[15s] Requests: 751 | Success: 751 | Failed: 0 | RPS: 50.07
```

- **Requests**: Total requests sent so far
- **Success**: Successfully completed requests
- **Failed**: Failed/timeout requests
- **RPS**: Requests per second (throughput)

### Final Results:
```
╔════════════════════════════════════════════════════════╗
║              Load Test Results                        ║
╚════════════════════════════════════════════════════════╝

Duration: 60 seconds

Requests:
  Total Requests:      3000
  Successful:          2985
  Failed:              15
  Success Rate:        99.50%

Throughput:
  Requests/sec:        50.00

Latency (milliseconds):
  Min:                 5 ms
  Average:             12 ms
  Median (P50):        11 ms
  P90:                 18 ms
  P95:                 22 ms
  P99:                 35 ms
  Max:                 87 ms

═══════════════════════════════════════════════════════

Resume-worthy metrics:
✓ Achieved 50.00 requests/sec throughput
✓ Maintained P95 latency under 22ms
✓ 99.50% success rate across 3000 requests
```

---

## What to Watch During Testing

### 1. Load Balancer Dashboard
Watch the live dashboard in the LoadBalancer terminal:
- Worker status (UP/DOWN)
- Active connections per worker
- Request distribution
- Average latency per worker

### 2. Task Manager
Monitor system resources:
- CPU usage (should stay reasonable)
- Memory usage
- Network I/O

### 3. Worker Terminals
Check for any errors or connection issues

---

## Recommended Test Sequence

Run tests in this order to understand your system's limits:

### Test 1: Baseline (Warmup)
```powershell
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" LoadTestClient 10 30
```
- Purpose: Verify everything works
- Expected: Very low latency, 0% errors

### Test 2: Normal Load
```powershell
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" LoadTestClient 50 60
```
- Purpose: Simulate typical production load
- Expected: Stable latency, <1% errors

### Test 3: Peak Load
```powershell
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" LoadTestClient 100 120
```
- Purpose: Test system under high load
- Expected: Slightly higher latency, <5% errors

### Test 4: Stress Test
```powershell
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" LoadTestClient 200 180
```
- Purpose: Find breaking point
- Expected: System may struggle, errors may increase

---

## Tuning Tips

### If you see high latency:

1. **Increase DB connection pool size**
   - Edit `config.properties`
   - Change `db.pool.size=5` to `db.pool.size=10`

2. **Add more workers**
   - Start additional workers on different ports
   - Add them to `worker_list.txt`

3. **Check PostgreSQL performance**
   - Add indexes to the `studentinfo` table
   - Tune PostgreSQL configuration

### If you see errors:

1. **Connection timeouts**
   - Reduce number of concurrent threads
   - Increase health check interval

2. **Workers going DOWN**
   - Check worker logs
   - Verify PostgreSQL is not overloaded
   - Restart failed workers

3. **Database errors**
   - Check PostgreSQL connection limits
   - Increase max_connections in postgresql.conf

---

## Sample Results for Resume

After running your tests, use these metrics:

**Example 1: Throughput-focused**
> "Load-tested with 100 concurrent clients achieving 85 requests/sec throughput with 99.2% success rate"

**Example 2: Latency-focused**
> "Maintained P95 latency under 25ms while handling 50 concurrent connections and 5000+ requests"

**Example 3: Reliability-focused**
> "Achieved 99.8% uptime and request success rate across 180-second stress test with 200 concurrent users"

**Example 4: Scale-focused**
> "System scaled linearly from 10 to 100 concurrent connections, distributing 6000+ requests across 5 workers using Weighted Round-Robin"

---

## Troubleshooting

### "Connection refused" error
```
✗ Load Balancer is not running on port 12345!
```
**Solution**: Start the LoadBalancer first
```powershell
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" LoadBalancer RR
```

### High failure rate (>10%)
**Possible causes:**
- Workers are down or overloaded
- Database connection pool exhausted
- PostgreSQL is not responding

**Solution**: 
1. Check worker status in LoadBalancer dashboard
2. Restart workers if needed
3. Increase `db.pool.size` in config.properties

### Very slow requests (>100ms average)
**Possible causes:**
- Database query is slow
- Too many concurrent requests
- Network latency

**Solution**:
1. Add database indexes
2. Reduce number of concurrent threads
3. Add more workers

---

## Next Steps

1. ✅ Run baseline test (10 threads, 30 seconds)
2. ✅ Run normal load test (50 threads, 60 seconds)
3. ✅ Run peak load test (100 threads, 120 seconds)
4. ✅ Document your best results
5. ✅ Add metrics to your resume!

---

## Quick Reference

| Command | Threads | Duration | Use Case |
|---------|---------|----------|----------|
| `LoadTestClient 10 30` | 10 | 30s | Baseline/warmup |
| `LoadTestClient 50 60` | 50 | 60s | Normal load |
| `LoadTestClient 100 120` | 100 | 120s | Peak load |
| `LoadTestClient 200 180` | 200 | 180s | Stress test |

---

**Ready to start? Just run:**
```powershell
.\run_load_test.ps1
```
