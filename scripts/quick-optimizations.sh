#!/bin/bash

echo "🚀 VGS-KV Quick Optimizations Script"
echo "====================================="
echo "This script implements the most critical optimizations identified in the codebase analysis."
echo ""

# Function to backup original files
backup_file() {
    local file="$1"
    if [ -f "$file" ]; then
        cp "$file" "${file}.backup.$(date +%Y%m%d_%H%M%S)"
        echo "✅ Backed up: $file"
    fi
}

# Function to check if file exists
check_file() {
    local file="$1"
    if [ ! -f "$file" ]; then
        echo "❌ File not found: $file"
        return 1
    fi
    return 0
}

echo "📋 Phase 1: Critical Performance Fixes"
echo "--------------------------------------"

# 1. Optimize Couchbase connection settings
echo "🔧 Optimizing Couchbase connection settings..."

# Embedded Document Pattern
if check_file "vgs-application/embedded-document/src/main/resources/application.yml"; then
    backup_file "vgs-application/embedded-document/src/main/resources/application.yml"
    
    # Update connection settings
    sed -i 's/min-endpoints: 2/min-endpoints: 4/' vgs-application/embedded-document/src/main/resources/application.yml
    sed -i 's/max-endpoints: 8/max-endpoints: 16/' vgs-application/embedded-document/src/main/resources/application.yml
    sed -i 's/idle-http-connection-timeout: 30s/idle-http-connection-timeout: 60s/' vgs-application/embedded-document/src/main/resources/application.yml
    sed -i 's/connect: 10s/connect: 5s/' vgs-application/embedded-document/src/main/resources/application.yml
    sed -i 's/key-value: 2500ms/key-value: 1000ms/' vgs-application/embedded-document/src/main/resources/application.yml
    sed -i 's/query: 30s/query: 15s/' vgs-application/embedded-document/src/main/resources/application.yml
    
    echo "✅ Updated Embedded Document connection settings"
fi

# Transaction Index Pattern
if check_file "vgs-application/transaction-index/src/main/resources/application.yml"; then
    backup_file "vgs-application/transaction-index/src/main/resources/application.yml"
    
    # Update connection settings
    sed -i 's/max-http-connections: 50/max-http-connections: 100/' vgs-application/transaction-index/src/main/resources/application.yml
    sed -i 's/connect-timeout: 10s/connect-timeout: 5s/' vgs-application/transaction-index/src/main/resources/application.yml
    sed -i 's/kv-timeout: 3s/kv-timeout: 1s/' vgs-application/transaction-index/src/main/resources/application.yml
    sed -i 's/num-kv-endpoints: 4/num-kv-endpoints: 16/' vgs-application/transaction-index/src/main/resources/application.yml
    
    echo "✅ Updated Transaction Index connection settings"
fi

# 2. Add environment-specific configurations
echo "⚙️ Creating environment-specific configurations..."

# Create dev configuration
cat > vgs-application/embedded-document/src/main/resources/application-dev.yml << 'EOF'
spring:
  couchbase:
    connection-string: ${COUCHBASE_DEV_CONNECTION_STRING:couchbases://localhost:8091}
    username: ${COUCHBASE_DEV_USERNAME:Administrator}
    password: ${COUCHBASE_DEV_PASSWORD:password}
    bucket-name: ${COUCHBASE_DEV_BUCKET_NAME:vgs-gaming-dev}
    io:
      min-endpoints: 2
      max-endpoints: 8
    timeout:
      connect: 10s
      key-value: 2500ms
      query: 30s

logging:
  level:
    com.vgs.kvpoc.embedded: DEBUG
    com.couchbase: INFO
EOF

# Create prod configuration
cat > vgs-application/embedded-document/src/main/resources/application-prod.yml << 'EOF'
spring:
  couchbase:
    connection-string: ${COUCHBASE_PROD_CONNECTION_STRING}
    username: ${COUCHBASE_PROD_USERNAME}
    password: ${COUCHBASE_PROD_PASSWORD}
    bucket-name: ${COUCHBASE_PROD_BUCKET_NAME:vgs-gaming}
    io:
      min-endpoints: 8
      max-endpoints: 32
    timeout:
      connect: 5s
      key-value: 1000ms
      query: 15s

logging:
  level:
    com.vgs.kvpoc.embedded: WARN
    com.couchbase: WARN
EOF

echo "✅ Created environment-specific configurations"

# 3. Add input validation dependencies
echo "🔒 Adding security and validation dependencies..."

# Update Embedded Document pom.xml
if check_file "vgs-application/embedded-document/pom.xml"; then
    backup_file "vgs-application/embedded-document/pom.xml"
    
    # Add Spring Security dependency
    sed -i '/<dependency>/a\
        <!-- Spring Security -->\
        <dependency>\
            <groupId>org.springframework.boot</groupId>\
            <artifactId>spring-boot-starter-security</artifactId>\
        </dependency>\
        <!-- OpenAPI Documentation -->\
        <dependency>\
            <groupId>org.springdoc</groupId>\
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>\
            <version>2.2.0</version>\
        </dependency>' vgs-application/embedded-document/pom.xml
    
    echo "✅ Added security dependencies to Embedded Document pom.xml"
fi

# Update Transaction Index pom.xml
if check_file "vgs-application/transaction-index/pom.xml"; then
    backup_file "vgs-application/transaction-index/pom.xml"
    
    # Add Spring Security dependency
    sed -i '/<dependency>/a\
        <!-- Spring Security -->\
        <dependency>\
            <groupId>org.springframework.boot</groupId>\
            <artifactId>spring-boot-starter-security</artifactId>\
        </dependency>\
        <!-- OpenAPI Documentation -->\
        <dependency>\
            <groupId>org.springdoc</groupId>\
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>\
            <version>2.2.0</version>\
        </dependency>' vgs-application/transaction-index/pom.xml
    
    echo "✅ Added security dependencies to Transaction Index pom.xml"
fi

# 4. Create enhanced monitoring configuration
echo "📊 Creating enhanced monitoring configuration..."

cat > monitoring/enhanced-prometheus.yml << 'EOF'
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "vgs-alerts.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

scrape_configs:
  - job_name: 'vgs-embedded-document'
    static_configs:
      - targets: ['localhost:5100']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    scrape_timeout: 5s

  - job_name: 'vgs-transaction-index'
    static_configs:
      - targets: ['localhost:5300']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    scrape_timeout: 5s

  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
EOF

echo "✅ Created enhanced Prometheus configuration"

# 5. Create Docker configuration
echo "🐳 Creating Docker configuration..."

cat > vgs-application/Dockerfile << 'EOF'
FROM openjdk:21-jdk-slim

WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy application JAR
COPY target/*.jar app.jar

# Create non-root user
RUN addgroup --system appgroup && \
    adduser --system --ingroup appgroup appuser

# Set ownership
RUN chown -R appuser:appgroup /app

USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:5100/actuator/health || exit 1

EXPOSE 5100

ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

echo "✅ Created Dockerfile"

# 6. Create Kubernetes deployment
echo "☸️ Creating Kubernetes deployment configuration..."

cat > vgs-application/k8s-deployment.yaml << 'EOF'
apiVersion: apps/v1
kind: Deployment
metadata:
  name: vgs-embedded-document
  labels:
    app: vgs-embedded-document
spec:
  replicas: 3
  selector:
    matchLabels:
      app: vgs-embedded-document
  template:
    metadata:
      labels:
        app: vgs-embedded-document
    spec:
      containers:
      - name: vgs-app
        image: vgs-embedded-document:latest
        ports:
        - containerPort: 5100
        env:
        - name: COUCHBASE_CONNECTION_STRING
          valueFrom:
            secretKeyRef:
              name: couchbase-secret
              key: connection-string
        - name: COUCHBASE_USERNAME
          valueFrom:
            secretKeyRef:
              name: couchbase-secret
              key: username
        - name: COUCHBASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: couchbase-secret
              key: password
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 5100
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 5100
          initialDelaySeconds: 30
          periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: vgs-embedded-document-service
spec:
  selector:
    app: vgs-embedded-document
  ports:
    - protocol: TCP
      port: 80
      targetPort: 5100
  type: LoadBalancer
EOF

echo "✅ Created Kubernetes deployment configuration"

# 7. Create enhanced health check script
echo "🏥 Creating enhanced health check script..."

cat > vgs-application/scripts/enhanced-health-check.sh << 'EOF'
#!/bin/bash

echo "🔍 VGS Application Enhanced Health Check"
echo "========================================"
echo "Timestamp: $(date)"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check service health with detailed metrics
check_service_health() {
    local port=$1
    local service_name=$2
    
    echo "Checking $service_name on port $port..."
    
    # Check if port is open
    if ! nc -z 0.0.0.0 "$port" 2>/dev/null; then
        echo -e "${RED}❌ $service_name: Port $port not accessible${NC}"
        return 1
    fi
    
    # Check health endpoint
    local health_response=$(curl -s "http://0.0.0.0:$port/actuator/health" 2>/dev/null)
    
    if echo "$health_response" | grep -q '"status":"UP"'; then
        echo -e "${GREEN}✅ $service_name: Healthy${NC}"
        
        # Extract additional health details
        if command -v jq >/dev/null 2>&1; then
            local components=$(echo "$health_response" | jq -r '.components | keys[]' | tr '\n' ', ' | sed 's/,$//')
            echo "   Components: $components"
        fi
        
        # Check metrics endpoint
        local metrics_response=$(curl -s "http://0.0.0.0:$port/actuator/metrics" 2>/dev/null)
        if [ $? -eq 0 ]; then
            echo "   Metrics endpoint: Available"
        fi
        
        return 0
    else
        echo -e "${RED}❌ $service_name: Unhealthy${NC}"
        echo "   Response: $health_response"
        return 1
    fi
}

# Function to check performance metrics
check_performance_metrics() {
    local port=$1
    local service_name=$2
    
    echo "Checking performance metrics for $service_name..."
    
    # Check HTTP server requests
    local http_metrics=$(curl -s "http://0.0.0.0:$port/actuator/metrics/http.server.requests" 2>/dev/null)
    if [ $? -eq 0 ] && command -v jq >/dev/null 2>&1; then
        local count=$(echo "$http_metrics" | jq -r '.measurements[] | select(.statistic=="COUNT") | .value')
        local max=$(echo "$http_metrics" | jq -r '.measurements[] | select(.statistic=="MAX") | .value')
        echo "   HTTP Requests: $count total, ${max}ms max response time"
    fi
    
    # Check JVM metrics
    local jvm_metrics=$(curl -s "http://0.0.0.0:$port/actuator/metrics/jvm.memory.used" 2>/dev/null)
    if [ $? -eq 0 ] && command -v jq >/dev/null 2>&1; then
        local memory_used=$(echo "$jvm_metrics" | jq -r '.measurements[0].value')
        local memory_mb=$((memory_used / 1024 / 1024))
        echo "   Memory Used: ${memory_mb}MB"
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

# Check services
echo "Checking service health..."
echo ""

check_service_health 5100 "Embedded Document Service"
check_performance_metrics 5100 "Embedded Document Service"
echo ""

check_service_health 5300 "Transaction Index Service"
check_performance_metrics 5300 "Transaction Index Service"
echo ""

# Check monitoring stack
echo "Checking monitoring stack..."
check_service_health 9090 "Prometheus"
check_service_health 3000 "Grafana"
echo ""

echo "Health check completed at $(date)"
EOF

chmod +x vgs-application/scripts/enhanced-health-check.sh
echo "✅ Created enhanced health check script"

echo ""
echo "🎉 Quick Optimizations Completed!"
echo "================================"
echo ""
echo "✅ What was implemented:"
echo "   • Optimized Couchbase connection settings"
echo "   • Created environment-specific configurations"
echo "   • Added security dependencies"
echo "   • Enhanced monitoring configuration"
echo "   • Created Docker and Kubernetes configurations"
echo "   • Enhanced health check script"
echo ""
echo "📋 Next steps:"
echo "   1. Update your Couchbase connection details in the environment files"
echo "   2. Build the applications: mvn clean package -DskipTests"
echo "   3. Test the enhanced health check: ./vgs-application/scripts/enhanced-health-check.sh"
echo "   4. Run the improved benchmark tests"
echo ""
echo "📚 For detailed implementation guide, see: VGS-KV_Codebase_Analysis_and_Recommendations.md"
