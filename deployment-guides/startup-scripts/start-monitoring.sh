
#!/bin/bash

echo "Starting VGS Monitoring Services..."
echo "Timestamp: $(date)"

# Check if monitoring directory exists
if [ ! -d "monitoring" ]; then
    echo "Creating monitoring directory..."
    mkdir -p monitoring
fi

cd monitoring

# Start metrics server for benchmark testing
echo "Starting metrics server..."
cd ../benchmark-testing
nohup python3 metrics_server.py > metrics-server.log 2>&1 &
METRICS_PID=$!
echo "Metrics server started with PID: $METRICS_PID"

# Wait for services to start
sleep 5

# Check service status
echo "Service Status:"
echo "Metrics Server: http://0.0.0.0:8000/metrics"

echo "Monitoring services ready!"
