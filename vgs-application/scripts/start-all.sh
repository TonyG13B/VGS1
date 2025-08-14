
#!/bin/bash

echo "Starting All VGS Services..."
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

# Clean up all VGS ports
echo "Cleaning up all VGS ports..."
close_port_if_used 5100 "Embedded Document Service"
close_port_if_used 5101 "Embedded Document Management"
close_port_if_used 5300 "Transaction Index Service"
close_port_if_used 5301 "Transaction Index Management"

# Create logs directory if it doesn't exist
mkdir -p logs

# Get script directory
SCRIPT_DIR="$(dirname "$0")"

# Start Embedded Document Service
echo ""
echo "Starting Embedded Document Service..."
"$SCRIPT_DIR/start-embedded.sh" &
EMBEDDED_PID=$!

# Wait a moment before starting next service
sleep 3

# Start Transaction Index Service
echo ""
echo "Starting Transaction Index Service..."
"$SCRIPT_DIR/start-transaction.sh" &
TRANSACTION_PID=$!

# Wait for both services to start
echo ""
echo "Waiting for services to initialize..."
sleep 10

# Check service status
echo ""
echo "Checking service status..."

# Check if ports are responding
if nc -z 0.0.0.0 5100 2>/dev/null; then
    echo "✅ Embedded Document Service is responding on port 5100"
else
    echo "❌ Embedded Document Service is not responding on port 5100"
fi

if nc -z 0.0.0.0 5300 2>/dev/null; then
    echo "✅ Transaction Index Service is responding on port 5300"
else
    echo "❌ Transaction Index Service is not responding on port 5300"
fi

echo ""
echo "All VGS services startup completed!"
echo "Check individual service logs for details:"
echo "  - Embedded Document: logs/embedded-document.log"
echo "  - Transaction Index: logs/transaction-index.log"
