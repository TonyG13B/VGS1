
#!/bin/bash

echo "VGS Application Health Check..."
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

# Function to check service health
check_service_health() {
    local port=$1
    local service_name=$2
    
    echo "Checking $service_name..."
    
    # Check if port is open
    if ! nc -z 0.0.0.0 "$port" 2>/dev/null; then
        echo "❌ $service_name: Port $port not accessible"
        return 1
    fi
    
    # Check health endpoint
    local health_response=$(curl -s "http://0.0.0.0:$port/actuator/health" 2>/dev/null)
    
    if echo "$health_response" | grep -q '"status":"UP"'; then
        echo "✅ $service_name: Healthy"
        
        # Extract additional health details
        if command -v jq >/dev/null 2>&1; then
            echo "   Details: $(echo "$health_response" | jq -r '.components | keys[]' | tr '\n' ', ' | sed 's/,$//')"
        fi
        return 0
    else
        echo "❌ $service_name: Unhealthy"
        echo "   Response: $health_response"
        return 1
    fi
}

# Check Java processes
echo "Checking Java processes..."
java_processes=$(ps aux | grep java | grep -v grep | wc -l)
echo "Active Java processes: $java_processes"

if [ "$java_processes" -gt 0 ]; then
    echo "Java processes:"
    ps aux | grep java | grep -v grep | awk '{print "  PID: " $2 " - " $11 " " $12 " " $13}'
fi

echo ""

# Clean up ports before health checks
echo "Cleaning up ports if necessary..."
close_port_if_used 5100 "Embedded Document Service"
close_port_if_used 5101 "Embedded Document Management"
close_port_if_used 5300 "Transaction Index Service"
close_port_if_used 5301 "Transaction Index Management"

echo ""

# Check VGS services
services_healthy=0
total_services=0

# Check Embedded Document Service
if check_service_health 5101 "Embedded Document Service"; then
    ((services_healthy++))
fi
((total_services++))

# Check Transaction Index Service
if check_service_health 5301 "Transaction Index Service"; then
    ((services_healthy++))
fi
((total_services++))

echo ""
echo "Health Check Summary:"
echo "Services healthy: $services_healthy/$total_services"

if [ "$services_healthy" -eq "$total_services" ]; then
    echo "✅ All VGS services are healthy!"
    exit 0
else
    echo "❌ Some VGS services are unhealthy!"
    exit 1
fies++))

echo ""

# Check Transaction Index Service (if running)
if nc -z 0.0.0.0 6001 2>/dev/null; then
    if check_service_health 6001 "Transaction Index Service"; then
        ((services_healthy++))
    fi
    ((total_services++))
fi

# System resources
echo "System Resources:"
echo "  CPU Load: $(uptime | awk -F'load average:' '{print $2}')"
echo "  Memory: $(free -h | awk 'NR==2{printf "Used: %s/%s (%.1f%%)", $3,$2,$3*100/$2}')"
echo "  Disk: $(df -h / | awk 'NR==2{printf "Used: %s/%s (%s)", $3,$2,$5}')"

echo ""

# Database connectivity (if configured)
if [ -n "$COUCHBASE_CONNECTION_STRING" ]; then
    echo "Database Connectivity:"
    # This would require a more sophisticated check in a real environment
    echo "  Couchbase: Configuration detected"
else
    echo "Database Connectivity:"
    echo "  Couchbase: Not configured (environment variables missing)"
fi

echo ""

# Summary
echo "Health Check Summary:"
echo "  Services healthy: $services_healthy/$total_services"

if [ "$services_healthy" -eq "$total_services" ] && [ "$total_services" -gt 0 ]; then
    echo "  Overall status: ✅ HEALTHY"
    exit 0
else
    echo "  Overall status: ❌ UNHEALTHY"
    exit 1
fi
