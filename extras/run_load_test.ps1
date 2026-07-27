# Load Test Runner Script
# Quick script to compile and run load tests

Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║     Load Balancer - Load Test Runner                 ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Check if LoadTestClient.java exists
if (-not (Test-Path "LoadTestClient.java")) {
    Write-Host "Error: LoadTestClient.java not found!" -ForegroundColor Red
    exit 1
}

# Compile LoadTestClient
Write-Host "[1/3] Compiling LoadTestClient..." -ForegroundColor Yellow
javac -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar" -d "./out/production/load-balancing-java" LoadTestClient.java

if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed!" -ForegroundColor Red
    exit 1
}

Write-Host "✓ Compilation successful" -ForegroundColor Green
Write-Host ""

# Check if LoadBalancer is running
Write-Host "[2/3] Checking if Load Balancer is running..." -ForegroundColor Yellow
$connection = Test-NetConnection -ComputerName localhost -Port 12345 -WarningAction SilentlyContinue

if (-not $connection.TcpTestSucceeded) {
    Write-Host "⚠ Warning: Load Balancer is not running on port 12345!" -ForegroundColor Red
    Write-Host "Please start the Load Balancer and Workers first." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Start in this order:" -ForegroundColor Yellow
    Write-Host "  1. Workers (20001-20005)" -ForegroundColor Yellow
    Write-Host "  2. LoadBalancer" -ForegroundColor Yellow
    Write-Host "  3. This load test" -ForegroundColor Yellow
    Write-Host ""
    $continue = Read-Host "Continue anyway? (y/n)"
    if ($continue -ne "y") {
        exit 0
    }
} else {
    Write-Host "✓ Load Balancer is running" -ForegroundColor Green
}

Write-Host ""
Write-Host "[3/3] Starting Load Test..." -ForegroundColor Yellow
Write-Host ""

# Prompt for test parameters
Write-Host "Test Configuration:" -ForegroundColor Cyan
$threads = Read-Host "Number of concurrent threads (default: 50)"
if ([string]::IsNullOrWhiteSpace($threads)) { $threads = 50 }

$duration = Read-Host "Test duration in seconds (default: 60)"
if ([string]::IsNullOrWhiteSpace($duration)) { $duration = 60 }

Write-Host ""
Write-Host "Running load test with $threads threads for $duration seconds..." -ForegroundColor Green
Write-Host ""

# Run the load test
java -cp "./jars/json-20180813.jar;./jars/postgresql-42.7.5.jar;./out/production/load-balancing-java" LoadTestClient $threads $duration

Write-Host ""
Write-Host "Load test completed!" -ForegroundColor Green
