
#!/bin/bash

echo "Running Comprehensive VGS Benchmark Tests..."
echo "Timestamp: $(date)"

# Create results directory
mkdir -p results/$(date +%Y%m%d_%H%M%S)
RESULTS_DIR="results/$(date +%Y%m%d_%H%M%S)"

# Function to run test and capture results
run_test() {
    local test_name=$1
    local users=$2
    local spawn_rate=$3
    local run_time=$4
    
    echo "Running $test_name with $users users, spawn rate $spawn_rate for $run_time..."
    
    locust -f locustfile.py \
        --host=http://0.0.0.0:5000 \
        --users=$users \
        --spawn-rate=$spawn_rate \
        --run-time=$run_time \
        --headless \
        --csv=$RESULTS_DIR/$test_name
    
    echo "$test_name completed"
}

# Pre-test health check
echo "Performing pre-test health check..."
if ! curl -s http://0.0.0.0:5001/actuator/health | grep -q "UP"; then
    echo "ERROR: VGS service is not healthy. Please start the service first."
    exit 1
fi

# Test scenarios
echo "Starting test scenarios..."

# Light load test
run_test "light_load" 10 2 "30s"

# Medium load test  
run_test "medium_load" 50 5 "1m"

# Heavy load test
run_test "heavy_load" 100 10 "2m"

# Stress test
run_test "stress_test" 200 20 "1m"

# Spike test
run_test "spike_test" 500 50 "30s"

# Generate summary report
echo "Generating test summary..."
python3 << EOF
import json
import glob
import csv
from datetime import datetime

results = {}
csv_files = glob.glob('$RESULTS_DIR/*_stats.csv')

for file in csv_files:
    test_name = file.split('/')[-1].replace('_stats.csv', '')
    with open(file, 'r') as f:
        reader = csv.DictReader(f)
        for row in reader:
            if row['Name'] == 'Aggregated':
                results[test_name] = {
                    'requests': row['Request Count'],
                    'failures': row['Failure Count'],
                    'avg_response_time': row['Average Response Time'],
                    'max_response_time': row['Max Response Time'],
                    'requests_per_sec': row['Requests/s']
                }

summary = {
    'timestamp': datetime.now().isoformat(),
    'results': results
}

with open('$RESULTS_DIR/test_summary.json', 'w') as f:
    json.dump(summary, f, indent=2)
EOF

echo "Comprehensive testing complete!"
echo "Results saved in: $RESULTS_DIR"
echo "Summary: $RESULTS_DIR/test_summary.json"
