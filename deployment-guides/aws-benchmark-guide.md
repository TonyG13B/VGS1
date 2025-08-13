
# Benchmark Testing Deployment Guide for AWS/Ubuntu

## Overview

This guide will help you set up comprehensive benchmark testing for your VGS application on AWS Ubuntu instances. The testing system simulates thousands of concurrent users to verify your application meets gaming industry standards: 100% write success, ≤50ms round data retrieval, and ≤20ms request completion.

## What You'll Set Up

- **Locust Load Testing**: Modern Python-based load testing tool
- **Performance Metrics Collection**: Automated result tracking
- **Concurrent User Simulation**: Test with up to 1000+ simultaneous users
- **Real-time Monitoring**: Watch performance during tests

## Prerequisites

1. **AWS Account**: Active AWS account with EC2 access
2. **VGS Application**: Running on AWS or accessible endpoint
3. **Ubuntu Server**: 22.04 LTS (recommended)
4. **SSH Client**: PuTTY (Windows) or Terminal (Mac/Linux)

## Step 1: Launch AWS EC2 Instance

1. **Log into AWS Console**:
   - Navigate to EC2 Dashboard
   - Click "Launch Instance"

2. **Configure Instance**:
   - **Name**: "VGS-Benchmark-Testing"
   - **AMI**: Ubuntu Server 22.04 LTS
   - **Instance Type**: t3.large (recommended for load testing)
   - **Key Pair**: Create new or select existing
   - **Security Group**: Create with these rules:
     - SSH (22): Your IP only
     - HTTP (80): Your IP only
     - Custom TCP (8089): Your IP only (Locust UI)

3. **Launch Instance**:
   - Review settings and click "Launch"
   - Note the Public IP address

## Step 2: Connect to Your Instance

1. **SSH Connection**:
   ```bash
   chmod 400 your-key.pem
   ssh -i your-key.pem ubuntu@YOUR-PUBLIC-IP
   ```

2. **Update System**:
   ```bash
   sudo apt update && sudo apt upgrade -y
   ```

## Step 3: Install Required Software

1. **Install Python and Dependencies**:
   ```bash
   sudo apt install python3 python3-pip python3-venv -y
   ```

2. **Install System Tools**:
   ```bash
   sudo apt install git htop curl -y
   ```

3. **Verify Installation**:
   ```bash
   python3 --version
   pip3 --version
   ```

## Step 4: Download and Setup Testing Environment

1. **Clone Repository**:
   ```bash
   git clone https://github.com/TonyG13B/VGS-KV.git
   cd VGS-KV/benchmark-testing
   ```

2. **Install Python Dependencies**:
   ```bash
   cd VGS-KV/benchmark-testing
   pip3 install -r requirements.txt
   ```

3. **Set Environment Variables**:
   ```bash
   export VGS_APP_HOST=http://your-vgs-app-ip:5000
   export VGS_APP_PORT=5000
   export TEST_DURATION=300
   export MAX_USERS=1000
   export SPAWN_RATE=50
   ```

4. **Make Variables Persistent**:
   ```bash
   echo 'export VGS_APP_HOST=http://your-vgs-app-ip:5000' >> ~/.bashrc
   echo 'export VGS_APP_PORT=5000' >> ~/.bashrc
   echo 'export TEST_DURATION=300' >> ~/.bashrc
   echo 'export MAX_USERS=1000' >> ~/.bashrc
   echo 'export SPAWN_RATE=50' >> ~/.bashrc
   source ~/.bashrc
   ```

## Step 5: Configure Load Test Scenarios

1. **Review Test Configuration**:
   ```bash
   nano locustfile.py
   ```

2. **Customize Test Parameters**:
   The file is pre-configured but you can modify:
   - Test data: Player IDs, game IDs, amounts
   - Wait times between requests
   - Request patterns and weights

3. **Verify Configuration**:
   ```bash
   locust --version
   python3 -c "import locust; print('Locust installed successfully')"
   ```

## Step 6: Run Basic Load Tests

1. **Start Simple Test**:
   ```bash
   locust -f locustfile.py --host=$VGS_APP_HOST --users=10 --spawn-rate=2 --run-time=60s --headless
   ```

2. **View Results**:
   The output will show:
   ```
   Name                          # reqs      # fails  |     Avg     Min     Max  Median  |   req/s
   POST /api/v1/transactions       150         0(0.00%) |      12       8      25      11  |    2.50
   GET /api/v1/rounds              75          0(0.00%) |       8       5      15       8  |    1.25
   ```

3. **Interpret Results**:
   - **# reqs**: Total requests sent
   - **# fails**: Failed requests (should be 0%)
   - **Avg**: Average response time (target <20ms)
   - **req/s**: Requests per second (throughput)

## Step 7: Run High-Concurrency Tests

1. **Stress Test**:
   ```bash
   locust -f locustfile.py --host=$VGS_APP_HOST --users=500 --spawn-rate=50 --run-time=5m --headless
   ```

2. **Peak Load Test**:
   ```bash
   locust -f locustfile.py --host=$VGS_APP_HOST --users=1000 --spawn-rate=100 --run-time=2m --headless
   ```

3. **Endurance Test**:
   ```bash
   locust -f locustfile.py --host=$VGS_APP_HOST --users=200 --spawn-rate=20 --run-time=30m --headless
   ```

## Step 8: Interactive Testing with Web UI

1. **Start Locust Web Interface**:
   ```bash
   locust -f locustfile.py --host=$VGS_APP_HOST --web-host=0.0.0.0
   ```

2. **Access Web UI**:
   - Open browser to `http://YOUR-PUBLIC-IP:8089`
   - You'll see the Locust web interface

3. **Configure Test via Web**:
   - Enter number of users (e.g., 100)
   - Enter spawn rate (e.g., 10 users/second)
   - Click "Start Swarming"

4. **Monitor Real-time Results**:
   - **Statistics Tab**: Request statistics and response times
   - **Charts Tab**: Real-time performance graphs
   - **Failures Tab**: Failed requests and reasons
   - **Download Data**: Export results as CSV

## Step 9: Advanced Testing Scenarios

1. **Smoke Test**:
   ```bash
   python3 smoke_bench.py
   ```

2. **Ramp-Up Test**:
   ```bash
   locust -f locustfile.py --host=$VGS_APP_HOST --users=1000 --spawn-rate=10 --run-time=10m --headless
   ```

3. **Spike Test**:
   ```bash
   locust -f locustfile.py --host=$VGS_APP_HOST --users=1000 --spawn-rate=1000 --run-time=30s --headless
   ```

## Step 10: Automated Testing and Reporting

1. **Create Test Script**:
   ```bash
   nano run-comprehensive-tests.sh
   ```

   Add content:
   ```bash
   #!/bin/bash
   
   echo "Starting VGS Comprehensive Load Testing..."
   echo "Target: $VGS_APP_HOST"
   echo "Timestamp: $(date)"
   
   # Create results directory
   mkdir -p test_results/$(date +%Y%m%d_%H%M%S)
   RESULT_DIR="test_results/$(date +%Y%m%d_%H%M%S)"
   
   # Smoke test
   echo "Running smoke test..."
   locust -f locustfile.py --host=$VGS_APP_HOST --users=5 --spawn-rate=1 --run-time=30s --headless --csv=$RESULT_DIR/smoke
   
   # Ramp up test
   echo "Running ramp-up test..."
   locust -f locustfile.py --host=$VGS_APP_HOST --users=100 --spawn-rate=10 --run-time=5m --headless --csv=$RESULT_DIR/rampup
   
   # Stress test
   echo "Running stress test..."
   locust -f locustfile.py --host=$VGS_APP_HOST --users=500 --spawn-rate=50 --run-time=5m --headless --csv=$RESULT_DIR/stress
   
   # Peak load test
   echo "Running peak load test..."
   locust -f locustfile.py --host=$VGS_APP_HOST --users=1000 --spawn-rate=100 --run-time=2m --headless --csv=$RESULT_DIR/peak
   
   echo "Testing complete. Results in $RESULT_DIR"
   ```

2. **Make Script Executable**:
   ```bash
   chmod +x run-comprehensive-tests.sh
   ```

3. **Run Automated Tests**:
   ```bash
   ./run-comprehensive-tests.sh
   ```

## Step 11: Performance Analysis and Reporting

1. **Generate Summary Report**:
   ```bash
   nano generate_report.py
   ```

   Add Python script:
   ```python
   import pandas as pd
   import glob
   import os
   from datetime import datetime
   
   def analyze_results():
       latest_dir = max(glob.glob('test_results/*'), key=os.path.getctime)
       
       print(f"Analyzing results from: {latest_dir}")
       
       for test_type in ['smoke', 'rampup', 'stress', 'peak']:
           csv_file = f"{latest_dir}/{test_type}_stats.csv"
           if os.path.exists(csv_file):
               df = pd.read_csv(csv_file)
               print(f"\n{test_type.upper()} TEST RESULTS:")
               print(f"Average Response Time: {df['Average Response Time'].mean():.2f}ms")
               print(f"95th Percentile: {df['95%'].mean():.2f}ms")
               print(f"Failure Rate: {df['Failure Count'].sum() / df['Request Count'].sum() * 100:.2f}%")
               print(f"Total Requests: {df['Request Count'].sum()}")
   
   if __name__ == "__main__":
       analyze_results()
   ```

2. **Run Analysis**:
   ```bash
   pip3 install pandas
   python3 generate_report.py
   ```

## Step 12: Continuous Testing Setup

1. **Create Cron Job for Regular Testing**:
   ```bash
   crontab -e
   ```

   Add line for daily testing:
   ```bash
   0 2 * * * /home/ubuntu/VGS-KV/benchmark-testing/run-comprehensive-tests.sh
   ```

2. **Setup Log Rotation**:
   ```bash
   sudo nano /etc/logrotate.d/vgs-testing
   ```

   Add configuration:
   ```
   /home/ubuntu/VGS-KV/benchmark-testing/test_results/*.log {
       daily
       rotate 30
       compress
       delaycompress
       missingok
       notifempty
   }
   ```

## Step 13: Success Criteria Validation

### Performance Targets

✅ **Write Success Rate**: 100% (no failed POST requests)
✅ **Response Time**: Average <20ms for all endpoints
✅ **Round Data Retrieval**: <50ms for GET requests
✅ **Throughput**: >100 TPS (transactions per second)
✅ **Error Rate**: 0% under normal load (<500 users)
✅ **Stability**: No performance degradation over 30 minutes

### Automated Validation Script

```bash
nano validate_performance.sh
```

Add script:
```bash
#!/bin/bash

LATEST_RESULT=$(ls -t test_results/*/stress_stats.csv | head -1)

if [ -f "$LATEST_RESULT" ]; then
    echo "Validating performance criteria..."
    python3 -c "
import pandas as pd
df = pd.read_csv('$LATEST_RESULT')
avg_time = df['Average Response Time'].mean()
failure_rate = df['Failure Count'].sum() / df['Request Count'].sum() * 100
print(f'Average Response Time: {avg_time:.2f}ms (Target: <20ms)')
print(f'Failure Rate: {failure_rate:.2f}% (Target: 0%)')
print('PASS' if avg_time < 20 and failure_rate == 0 else 'FAIL')
"
else
    echo "No test results found"
fi
```

## Troubleshooting

### Common Issues and Solutions

1. **Connection Refused Errors**:
   ```bash
   # Check target application status
   curl -I $VGS_APP_HOST/actuator/health
   
   # Verify network connectivity
   ping your-vgs-app-ip
   
   # Check security groups allow traffic
   ```

2. **High Response Times**:
   ```bash
   # Monitor system resources on test machine
   htop
   
   # Check network latency
   ping your-vgs-app-ip
   
   # Monitor target server resources
   ```

3. **Locust Installation Issues**:
   ```bash
   # Reinstall with specific version
   pip3 uninstall locust
   pip3 install locust==2.15.1
   ```

4. **Memory Issues**:
   ```bash
   # Monitor memory usage
   free -h
   
   # Reduce concurrent users if needed
   export MAX_USERS=500
   ```

## Step 14: Integration with Monitoring

1. **Export Metrics to External Systems**:
   ```bash
   # Install metrics exporter
   pip3 install prometheus_client
   ```

2. **Create Metrics Endpoint**:
   ```bash
   nano metrics_server.py
   ```

   The file already exists in your repository with Prometheus metrics export.

3. **Start Metrics Server**:
   ```bash
   python3 metrics_server.py &
   ```

## Success Validation

Your benchmark testing is successful when:

- ✅ All test scenarios run without errors
- ✅ Results consistently meet success criteria
- ✅ Performance remains stable under load
- ✅ Automated testing runs on schedule
- ✅ Results are properly archived and analyzed

## Next Steps

1. **Scale Testing**: Use multiple instances for distributed load testing
2. **CI/CD Integration**: Add performance tests to deployment pipeline
3. **Advanced Scenarios**: Custom user behavior patterns
4. **Monitoring Integration**: Connect with application monitoring

Your benchmark testing environment is now ready to validate VGS application performance on AWS!
