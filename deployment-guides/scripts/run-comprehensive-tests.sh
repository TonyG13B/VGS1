
#!/bin/bash

echo "Starting VGS Comprehensive Load Testing..."
echo "Target: http://0.0.0.0:5000"
echo "Timestamp: $(date)"

# Create results directory
mkdir -p test_results/$(date +%Y%m%d_%H%M%S)
RESULT_DIR="test_results/$(date +%Y%m%d_%H%M%S)"

cd benchmark-testing

# Install requirements if needed
pip install -r requirements.txt

# Smoke test
echo "Running smoke test..."
locust -f locustfile.py --host=http://0.0.0.0:5000 --users=5 --spawn-rate=1 --run-time=30s --headless --csv=$RESULT_DIR/smoke

# Ramp up test
echo "Running ramp-up test..."
locust -f locustfile.py --host=http://0.0.0.0:5000 --users=100 --spawn-rate=10 --run-time=5m --headless --csv=$RESULT_DIR/rampup

# Stress test
echo "Running stress test..."
locust -f locustfile.py --host=http://0.0.0.0:5000 --users=500 --spawn-rate=50 --run-time=5m --headless --csv=$RESULT_DIR/stress

# Peak load test
echo "Running peak load test..."
locust -f locustfile.py --host=http://0.0.0.0:5000 --users=1000 --spawn-rate=100 --run-time=2m --headless --csv=$RESULT_DIR/peak

echo "Testing complete. Results in $RESULT_DIR"

# Generate summary report
python3 -c "
import glob
import os
import json
from datetime import datetime

result_dir = '$RESULT_DIR'
results = {}

for test_type in ['smoke', 'rampup', 'stress', 'peak']:
    stats_file = f'{result_dir}/{test_type}_stats.csv'
    if os.path.exists(stats_file):
        with open(stats_file, 'r') as f:
            lines = f.readlines()
            if len(lines) > 1:
                data = lines[1].split(',')
                results[test_type] = {
                    'avg_response_time': float(data[7]) if data[7] else 0,
                    'requests': int(data[1]) if data[1] else 0,
                    'failures': int(data[2]) if data[2] else 0
                }

with open(f'{result_dir}/summary.json', 'w') as f:
    json.dump(results, f, indent=2)

print('Test Summary:')
for test, data in results.items():
    print(f'{test.upper()}: {data[\"requests\"]} reqs, {data[\"avg_response_time\"]:.2f}ms avg')
"
