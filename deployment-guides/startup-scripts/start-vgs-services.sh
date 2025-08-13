
#!/bin/bash

echo "Starting VGS Application Services..."
echo "Timestamp: $(date)"

# Set environment variables
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export SPRING_PROFILES_ACTIVE=replit

# Function to check service health
check_health() {
    local port=$1
    local service_name=$2
    echo "Checking $service_name health on port $port..."
    
    for i in {1..30}; do
        if curl -s "http://0.0.0.0:$port/actuator/health" | grep -q "UP"; then
            echo "$service_name is healthy!"
            return 0
        fi
        echo "Waiting for $service_name to start... ($i/30)"
        sleep 5
    done
    
    echo "$service_name failed to start properly"
    return 1
}

# Start Embedded Document Service
echo "Starting Embedded Document Service..."
cd vgs-application/embedded-document
mvn clean package -DskipTests -q
nohup java -Xms1g -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication \
    -jar target/embedded-document-pattern-1.0.0.jar \
    --spring.profiles.active=replit \
    --server.port=5000 \
    --management.server.port=5001 > embedded-service.log 2>&1 &
EMBEDDED_PID=$!
echo "Embedded Service started with PID: $EMBEDDED_PID"

# Wait and check health
sleep 10
check_health 5001 "Embedded Document Service"

echo "VGS Services startup complete!"
echo "Embedded Document Service: http://0.0.0.0:5000"
echo "Health endpoint: http://0.0.0.0:5001/actuator/health"
echo "Metrics endpoint: http://0.0.0.0:5001/actuator/prometheus"
