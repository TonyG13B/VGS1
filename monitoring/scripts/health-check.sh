
#!/bin/bash

echo "VGS System Health Check..."
echo "Timestamp: $(date)"

# Function to check HTTP endpoint
check_endpoint() {
    local url=$1
    local service_name=$2
    
    if curl -s -o /dev/null -w "%{http_code}" "$url" | grep -q "200"; then
        echo "✅ $service_name: Healthy"
        return 0
    else
        echo "❌ $service_name: Unhealthy"
        return 1
    fi
}

# Function to check service health endpoint
check_health_endpoint() {
    local url=$1
    local service_name=$2
    
    response=$(curl -s "$url")
    if echo "$response" | grep -q '"status":"UP"'; then
        echo "✅ $service_name: UP"
        return 0
    else
        echo "❌ $service_name: DOWN"
        echo "Response: $response"
        return 1
    fi
}

# Check VGS services
echo "Checking VGS Services..."
check_health_endpoint "http://0.0.0.0:5001/actuator/health" "VGS Embedded Service"

# Check metrics endpoint
echo ""
echo "Checking Metrics..."
check_endpoint "http://0.0.0.0:5001/actuator/prometheus" "Prometheus Metrics"

# Check system resources
echo ""
echo "System Resources:"
echo "CPU Usage: $(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | awk -F'%' '{print $1}')%"
echo "Memory Usage: $(free | grep Mem | awk '{printf "%.1f%%", $3/$2 * 100.0}')"
echo "Disk Usage: $(df -h / | awk 'NR==2{printf "%s", $5}')"

# Check network connectivity
echo ""
echo "Network Connectivity:"
if ping -c 1 google.com > /dev/null 2>&1; then
    echo "✅ Internet connectivity: Available"
else
    echo "❌ Internet connectivity: Not available"
fi

# Check Java processes
echo ""
echo "Java Processes:"
ps aux | grep java | grep -v grep || echo "No Java processes running"

echo ""
echo "Health check complete!"
