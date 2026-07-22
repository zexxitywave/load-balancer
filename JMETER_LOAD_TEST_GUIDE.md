# JMeter Load Testing Guide

## Overview
This guide will help you perform load testing on the Load Balancer system using Apache JMeter.

## Prerequisites

1. **Install JMeter**
   - Download from: https://jmeter.apache.org/download_jmeter.cgi
   - Extract to a folder (e.g., `C:\apache-jmeter-5.6.3`)
   - Add JMeter's `bin` folder to PATH or navigate to it directly

2. **System Running**
   - All 5 Workers started
   - LoadBalancer started
   - HTTPLoadBalancerWrapper started (HTTP interface for JMeter)

---

## Step 1: Start the HTTP Wrapper

The load balancer uses raw TCP sockets. We need an HTTP wrapper for JMeter.

### Compile the HTTP Wrapper:
```powershell
javac -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" -d "./out/production/load-balancing-java" HTTPLoadBalancerWrapper.java
```

### Run the HTTP Wrapper:
```powershell
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" HTTPLoadBalancerWrapper
```

This will start an HTTP server on **port 8080** that forwards requests to your TCP load balancer on port 12345.

**Test it manually first:**
```powershell
# In another terminal or browser
curl http://localhost:8080/student/random
```

You should see JSON response with student data.

---

## Step 2: Launch JMeter

### Windows:
```powershell
cd C:\apache-jmeter-5.6.3\bin
.\jmeter.bat
```

### If you want GUI mode (recommended for first time):
The above command opens the GUI automatically.

### For command-line mode (for actual testing):
```powershell
.\jmeter.bat -n -t "path\to\LoadBalancer-LoadTest.jmx" -l results.jtl -e -o report
```

---

## Step 3: Create Test Plan in JMeter GUI

### Manual Setup (if .jmx file doesn't work):

1. **Add Thread Group**
   - Right-click Test Plan → Add → Threads (Users) → Thread Group
   - Configure:
     - Number of Threads (users): `100`
     - Ramp-up period (seconds): `10`
     - Loop Count: `10` (or Infinite for continuous)

2. **Add HTTP Request**
   - Right-click Thread Group → Add → Sampler → HTTP Request
   - Configure:
     - Server Name: `localhost`
     - Port: `8080`
     - Path: `/student/random`
     - Method: `GET`

3. **Add Listeners (for viewing results)**
   - Right-click Thread Group → Add → Listener → View Results Tree
   - Right-click Thread Group → Add → Listener → Summary Report
   - Right-click Thread Group → Add → Listener → Aggregate Report
   - Right-click Thread Group → Add → Listener → Response Time Graph

4. **Add Assertions (optional)**
   - Right-click HTTP Request → Add → Assertions → Response Assertion
   - Pattern: `"name"` (to verify JSON contains student data)

5. **Save Test Plan**
   - File → Save Test Plan As → `LoadBalancer-LoadTest.jmx`

---

## Step 4: Run the Load Test

### Option A: From GUI (for monitoring)
1. Click the green "Start" button (play icon)
2. Watch the results in real-time in the listeners
3. Click red "Stop" button when done

### Option B: From Command Line (for serious testing)
```powershell
cd C:\apache-jmeter-5.6.3\bin
.\jmeter.bat -n -t "C:\Users\jagdish\Downloads\Telegram Desktop\spring-boot-projects\load-balancing-java\LoadBalancer-LoadTest.jmx" -l results.jtl -e -o htmlreport
```

This generates an HTML report in the `htmlreport` folder.

---

## Step 5: Analyze Results

### Key Metrics to Look For:

1. **Throughput**: Requests per second
2. **Average Response Time**: Should stay consistent under load
3. **Error Rate**: Should be 0% or very low
4. **P90/P95/P99 Latency**: 90th/95th/99th percentile response times
5. **Max Response Time**: Worst case latency

### In JMeter GUI:
- **Summary Report**: Shows throughput, avg/min/max times, error %
- **Aggregate Report**: Shows median, 90%, 95%, 99% percentiles
- **Response Time Graph**: Visual representation of latency over time

### From HTML Report:
Open `htmlreport/index.html` in a browser for detailed charts and statistics.

---

## Load Test Scenarios

### Scenario 1: Light Load (Baseline)
- Threads: 10
- Ramp-up: 5 seconds
- Duration: 60 seconds
- Expected: Low latency, 0% errors

### Scenario 2: Medium Load
- Threads: 50
- Ramp-up: 10 seconds
- Duration: 120 seconds
- Expected: Slightly higher latency, 0% errors

### Scenario 3: Heavy Load
- Threads: 100
- Ramp-up: 10 seconds
- Duration: 180 seconds
- Expected: Test system limits

### Scenario 4: Stress Test (Find Breaking Point)
- Threads: 200+
- Ramp-up: 20 seconds
- Duration: 300 seconds
- Expected: Find maximum capacity before errors

### Scenario 5: Spike Test
- Threads: 1 → 100 instantly
- Ramp-up: 1 second
- Duration: 60 seconds
- Expected: Test sudden traffic spikes

---

## Monitoring During Test

### Watch Load Balancer Dashboard
The LoadBalancer terminal shows live metrics:
- Active connections per worker
- Total requests per worker
- Average latency per worker
- Worker status (UP/DOWN)

### Watch System Resources
Use Task Manager or `perfmon` to monitor:
- CPU usage
- Memory usage
- Network I/O

### Check Logs
Monitor `lb_requests.log` for any errors or unusual patterns.

---

## Expected Results (Sample)

With 100 concurrent users and proper configuration:

```
Throughput: ~200-500 requests/sec
Average Response Time: 10-20ms
P95 Latency: 20-30ms
P99 Latency: 30-50ms
Error Rate: 0%
```

Actual numbers will depend on your hardware and database performance.

---

## Troubleshooting

### "Connection refused" errors
- Ensure HTTPLoadBalancerWrapper is running
- Check port 8080 is not blocked
- Verify LoadBalancer is running on port 12345

### High error rate
- Check if workers are running
- Check PostgreSQL connection pool size
- Increase worker pool size in config.properties

### Slow response times
- Increase database connection pool size
- Add more workers
- Check PostgreSQL performance
- Check network latency

### Workers marked DOWN
- Check worker logs for errors
- Verify PostgreSQL connectivity
- Increase health check interval

---

## Resume-Worthy Metrics

After testing, capture these for your resume:

✅ **Throughput**: "Achieved X requests/sec with 100 concurrent users"
✅ **Latency**: "Maintained P95 latency under Xms under load"
✅ **Reliability**: "99.X% success rate across Y million requests"
✅ **Scalability**: "System scaled to X concurrent connections"
✅ **Load Distribution**: "Evenly distributed load across 5 workers with WRR algorithm"

---

## Next Steps

1. Run baseline test with 10 users
2. Gradually increase to 50, 100, 200 users
3. Note when errors start appearing (breaking point)
4. Document your findings
5. Add metrics to your resume
