
#!/bin/bash

echo "Starting VGS Embedded Document Service..."
echo "Timestamp: $(date)"

# Function to close port if in use
close_port_if_used() {
    local port=$1
    local service_name=$2
    
    # Find processes using the port
    local pids=$(lsof -ti:$port 2>/dev/null)
    
    if [ -n "$pids" ]; then
        echo "⚠️  Port $port is in use by $service_name. Attempting to close..."
        
        # Try graceful shutdown first
        for pid in $pids; do
            echo "Sending TERM signal to PID $pid"
            kill -TERM $pid 2>/dev/null
        done
        
        # Wait 5 seconds for graceful shutdown
        sleep 5
        
        # Check if still running, then force kill
        local remaining_pids=$(lsof -ti:$port 2>/dev/null)
        if [ -n "$remaining_pids" ]; then
            echo "Processes still running, forcing shutdown..."
            for pid in $remaining_pids; do
                echo "Sending KILL signal to PID $pid"
                kill -KILL $pid 2>/dev/null
            done
            sleep 2
        fi
        
        # Verify port is now free
        if ! lsof -ti:$port >/dev/null 2>&1; then
            echo "✅ Port $port is now available"
            return 0
        else
            echo "❌ Failed to free port $port"
            return 1
        fi
    else
        echo "✅ Port $port is available"
        return 0
    fi
}

# Clean up ports before starting
echo "Checking and cleaning up ports..."
close_port_if_used 5100 "Embedded Document Service"
close_port_if_used 5101 "Embedded Document Management"

# Navigate to embedded document directory
cd "$(dirname "$0")/../embedded-document" || {
    echo "❌ Failed to navigate to embedded-document directory"
    exit 1
}

# Build the application
echo "Building Embedded Document Service..."
mvn clean package -DskipTests || {
    echo "❌ Build failed"
    exit 1
}

# Start the service
echo "Starting Embedded Document Service on port 5100..."
nohup java -Xms1g -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+OptimizeStringConcat \
    -jar target/embedded-document-pattern-1.0.0.jar \
    --spring.profiles.active=replit \
    > logs/embedded-document.log 2>&1 &

SERVICE_PID=$!
echo "Embedded Document Service started with PID: $SERVICE_PID"

# Wait a moment and check if service started successfully
sleep 5
if ps -p $SERVICE_PID > /dev/null; then
    echo "✅ Embedded Document Service is running on port 5100"
    echo "Management interface available on port 5101"
    echo "Logs: logs/embedded-document.log"
else
    echo "❌ Failed to start Embedded Document Service"
    exit 1
fi
