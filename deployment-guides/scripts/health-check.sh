
#!/bin/bash

echo "VGS System Health Check"
echo "======================"
echo "Timestamp: $(date)"
echo ""

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print status
print_status() {
    local status=$1
    local message=$2
    if [ "$status" = "OK" ]; then
        echo -e "${GREEN}✅ $message${NC}"
    elif [ "$status" = "WARNING" ]; then
        echo -e "${YELLOW}⚠️  $message${NC}"
    else
        echo -e "${RED}❌ $message${NC}"
    fi
}

# Check if services are running
echo "1. Service Status"
echo "-----------------"

# Check VGS application
if curl -s http://0.0.0.0:5001/actuator/health | grep -q "UP"; then
    print_status "OK" "VGS Application is running"
else
    print_status "ERROR" "VGS Application is not responding"
fi

# Check metrics endpoint
if curl -s http://0.0.0.0:5001/actuator/prometheus > /dev/null; then
    print_status "OK" "Metrics endpoint is accessible"
else
    print_status "ERROR" "Metrics endpoint is not accessible"
fi

# Check API endpoint
response=$(curl -s -w "%{http_code}" -o /dev/null http://0.0.0.0:5000/api/v1/health)
if [ "$response" = "200" ]; then
    print_status "OK" "API endpoint is responding"
else
    print_status "ERROR" "API endpoint returned: $response"
fi

echo ""
echo "2. Performance Metrics"
echo "---------------------"

# Check response time
response_time=$(curl -o /dev/null -s -w "%{time_total}" http://0.0.0.0:5001/actuator/health)
response_time_ms=$(echo "$response_time * 1000" | bc)

if (( $(echo "$response_time_ms < 20" | bc -l) )); then
    print_status "OK" "Response time: ${response_time_ms}ms"
elif (( $(echo "$response_time_ms < 50" | bc -l) )); then
    print_status "WARNING" "Response time: ${response_time_ms}ms (slower than target)"
else
    print_status "ERROR" "Response time: ${response_time_ms}ms (too slow)"
fi

# Check memory usage if available
if command -v free &> /dev/null; then
    memory_usage=$(free | grep Mem | awk '{printf "%.1f", $3/$2 * 100}')
    if (( $(echo "$memory_usage < 80" | bc -l) )); then
        print_status "OK" "Memory usage: ${memory_usage}%"
    elif (( $(echo "$memory_usage < 90" | bc -l) )); then
        print_status "WARNING" "Memory usage: ${memory_usage}%"
    else
        print_status "ERROR" "Memory usage: ${memory_usage}% (too high)"
    fi
fi

echo ""
echo "3. Database Connectivity"
echo "------------------------"

# Test database connection through application
health_response=$(curl -s http://0.0.0.0:5001/actuator/health)
if echo "$health_response" | grep -q "couchbase.*UP"; then
    print_status "OK" "Couchbase connection is healthy"
elif echo "$health_response" | grep -q "couchbase"; then
    print_status "ERROR" "Couchbase connection issues detected"
else
    print_status "WARNING" "Couchbase health status not available"
fi

echo ""
echo "4. Recent Logs"
echo "-------------"

# Show recent application logs if available
if [ -f "vgs-application/embedded-document/embedded-service.log" ]; then
    echo "Last 5 log entries:"
    tail -5 vgs-application/embedded-document/embedded-service.log
else
    echo "No log file found"
fi

echo ""
echo "Health check complete!"
