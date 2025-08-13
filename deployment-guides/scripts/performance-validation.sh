
#!/bin/bash

echo "Validating VGS Performance Criteria..."

# Function to check if a service is running
check_service() {
    local url=$1
    local service_name=$2
    
    if curl -s "$url" > /dev/null; then
        echo "✅ $service_name is accessible"
        return 0
    else
        echo "❌ $service_name is not accessible"
        return 1
    fi
}

# Function to validate response time
check_response_time() {
    local url=$1
    local max_time=$2
    
    response_time=$(curl -o /dev/null -s -w "%{time_total}" "$url")
    response_time_ms=$(echo "$response_time * 1000" | bc)
    
    if (( $(echo "$response_time_ms < $max_time" | bc -l) )); then
        echo "✅ Response time: ${response_time_ms}ms (target: <${max_time}ms)"
        return 0
    else
        echo "❌ Response time: ${response_time_ms}ms (target: <${max_time}ms)"
        return 1
    fi
}

# Basic service checks
echo "1. Checking service availability..."
check_service "http://0.0.0.0:5001/actuator/health" "VGS Health Endpoint"
check_service "http://0.0.0.0:5001/actuator/prometheus" "VGS Metrics Endpoint"

# Response time checks
echo -e "\n2. Checking response times..."
check_response_time "http://0.0.0.0:5001/actuator/health" 20

# API functionality check
echo -e "\n3. Checking API functionality..."
response=$(curl -s -X POST http://0.0.0.0:5000/api/v1/transactions \
    -H "Content-Type: application/json" \
    -d '{
        "playerId": "test-player-123",
        "gameId": "test-game-456",
        "amount": 100,
        "transactionType": "BET"
    }')

if [[ $response == *"transactionId"* ]]; then
    echo "✅ Transaction creation successful"
else
    echo "❌ Transaction creation failed"
    echo "Response: $response"
fi

# Memory and CPU check
echo -e "\n4. Checking system resources..."
memory_usage=$(free | grep Mem | awk '{printf "%.1f", $3/$2 * 100}')
echo "Memory usage: ${memory_usage}%"

if (( $(echo "$memory_usage < 90" | bc -l) )); then
    echo "✅ Memory usage within limits"
else
    echo "⚠️  High memory usage detected"
fi

echo -e "\nPerformance validation complete!"
