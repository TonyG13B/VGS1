#!/bin/bash

echo "🔧 VGS Couchbase Configuration Setup"
echo "===================================="
echo "This script will configure Couchbase connection details for all VGS instances."
echo ""

# Create certificate directory and file
echo "📋 Setting up Couchbase certificate..."
sudo mkdir -p /etc/couchbase/certs

# Create certificate file
sudo tee /etc/couchbase/certs/couchbase-cert.pem > /dev/null << 'EOF'
-----BEGIN CERTIFICATE-----
MIIDFTCCAf2gAwIBAgIRANLVkgOvtaXiQJi0V6qeNtswDQYJKoZIhvcNAQELBQAw
JDESMBAGA1UECgwJQ291Y2hiYXNlMQ4wDAYDVQQLDAVDbG91ZDAeFw0xOTEyMDYy
MjEyNTlaFw0yOTEyMDYyMzEyNTlaMCQxEjAQBgNVBAoMCUNvdWNoYmFzZTEOMAwG
A1UECwwFQ2xvdWQwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCfvOIi
enG4Dp+hJu9asdxEMRmH70hDyMXv5ZjBhbo39a42QwR59y/rC/sahLLQuNwqif85
Fod1DkqgO6Ng3vecSAwyYVkj5NKdycQu5tzsZkghlpSDAyI0xlIPSQjoORA/pCOU
WOpymA9dOjC1bo6rDyw0yWP2nFAI/KA4Z806XeqLREuB7292UnSsgFs4/5lqeil6
rL3ooAw/i0uxr/TQSaxi1l8t4iMt4/gU+W52+8Yol0JbXBTFX6itg62ppb/Eugmn
mQRMgL67ccZs7cJ9/A0wlXencX2ohZQOR3mtknfol3FH4+glQFn27Q4xBCzVkY9j
KQ20T1LgmGSngBInAgMBAAGjQjBAMA8GA1UdEwEB/wQFMAMBAf8wHQYDVR0OBBYE
FJQOBPvrkU2In1Sjoxt97Xy8+cKNMA4GA1UdDwEB/wQEAwIBhjANBgkqhkiG9w0B
AQsFAAOCAQEARgM6XwcXPLSpFdSf0w8PtpNGehmdWijPM3wHb7WZiS47iNen3oq8
m2mm6V3Z57wbboPpfI+VEzbhiDcFfVnK1CXMC0tkF3fnOG1BDDvwt4jU95vBiNjY
xdzlTP/Z+qr0cnVbGBSZ+fbXstSiRaaAVcqQyv3BRvBadKBkCyPwo+7svQnScQ5P
Js7HEHKVms5tZTgKIw1fbmgR2XHleah1AcANB+MAPBCcTgqurqr5G7W2aPSBLLGA
fRIiVzm7VFLc7kWbp7ENH39HVG6TZzKnfl9zJYeiklo5vQQhGSMhzBsO70z4RRzi
DPFAN/4qZAgD5q3AFNIq2WWADFQGSwVJhg==
-----END CERTIFICATE-----
EOF

# Set proper permissions
sudo chmod 644 /etc/couchbase/certs/couchbase-cert.pem
echo "✅ Couchbase certificate created successfully"

# Create environment configuration file
echo "📝 Creating environment configuration..."
mkdir -p ~/VGS1

cat > ~/VGS1/.env << 'EOF'
# Couchbase Configuration
COUCHBASE_CONNECTION_STRING=couchbases://cb.dwmbjb1o-vduuwae.cloud.couchbase.com
COUCHBASE_USERNAME=vgs
COUCHBASE_PASSWORD=Password1234$
COUCHBASE_EMBEDDED_BUCKET_NAME=embedded_document
COUCHBASE_TRANSACTION_BUCKET_NAME=transaction_index

# Application Configuration
VGS_EMBEDDED_IP=13.115.27.45
VGS_TRANSACTION_IP=57.180.14.130
VGS_BENCHMARK_IP=13.231.68.228
VGS_MONITORING_IP=13.115.170.132

# Ports
VGS_EMBEDDED_PORT=5100
VGS_TRANSACTION_PORT=5300
VGS_BENCHMARK_PORT=8089
PROMETHEUS_PORT=9090
GRAFANA_PORT=3000

# Couchbase Collections
COUCHBASE_SCOPE=_default
COUCHBASE_GAME_ROUNDS_COLLECTION=game_rounds
COUCHBASE_GAME_TRANSACTIONS_COLLECTION=game_transactions
EOF

echo "✅ Environment configuration created successfully"

# Create application-specific configuration files
echo "⚙️ Creating application configurations..."

# Embedded Document Configuration
mkdir -p ~/VGS1/vgs-application/embedded-document/src/main/resources
cat > ~/VGS1/vgs-application/embedded-document/src/main/resources/application.yml << 'EOF'
server:
  port: 5100
  address: 0.0.0.0
  tomcat:
    threads:
      max: 200
      min-spare: 50
    max-connections: 8192
    accept-count: 100
    connection-timeout: 20000
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain

spring:
  application:
    name: vgs-embedded-document
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=5m
  task:
    execution:
      pool:
        core-size: 8
        max-size: 16
        queue-capacity: 100
    scheduling:
      pool:
        size: 4
  couchbase:
    connection-string: ${COUCHBASE_CONNECTION_STRING}
    username: ${COUCHBASE_USERNAME}
    password: ${COUCHBASE_PASSWORD}
    bucket-name: ${COUCHBASE_EMBEDDED_BUCKET_NAME}
    io:
      min-endpoints: 4
      max-endpoints: 16
      idle-http-connection-timeout: 60s
    timeout:
      connect: 5s
      key-value: 1000ms
      query: 15s
    compression:
      enabled: true
      min-ratio: 0.83

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics,caches
  endpoint:
    health:
      show-details: always
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  level:
    com.vgs.kvpoc.embedded: INFO
    com.couchbase: WARN
    org.springframework.web: WARN
    org.springframework.boot: WARN
    org.apache.tomcat: WARN
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{20} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{20} - %msg%n"
  file:
    name: target/spring.log
    max-size: 50MB
    max-history: 3
EOF

# Transaction Index Configuration
mkdir -p ~/VGS1/vgs-application/transaction-index/src/main/resources
cat > ~/VGS1/vgs-application/transaction-index/src/main/resources/application.yml << 'EOF'
server:
  port: 5300
  address: 0.0.0.0
  tomcat:
    threads:
      max: 200
      min-spare: 50
    max-connections: 8192
    accept-count: 100
    connection-timeout: 20000
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain

spring:
  application:
    name: vgs-transaction-index
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=5m
  task:
    execution:
      pool:
        core-size: 8
        max-size: 16
        queue-capacity: 100
    scheduling:
      pool:
        size: 4
  couchbase:
    connection-string: ${COUCHBASE_CONNECTION_STRING}
    username: ${COUCHBASE_USERNAME}
    password: ${COUCHBASE_PASSWORD}
    bucket-name: ${COUCHBASE_TRANSACTION_BUCKET_NAME}
    io:
      min-endpoints: 4
      max-endpoints: 16
      idle-http-connection-timeout: 60s
    timeout:
      connect: 5s
      key-value: 1000ms
      query: 15s
    compression:
      enabled: true
      min-ratio: 0.83

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics,caches
  endpoint:
    health:
      show-details: always
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  level:
    com.vgs.kvpoc.index: INFO
    com.couchbase: WARN
    org.springframework.web: WARN
    org.springframework.boot: WARN
    org.apache.tomcat: WARN
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{20} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{20} - %msg%n"
  file:
    name: target/spring.log
    max-size: 50MB
    max-history: 3
EOF

echo "✅ Application configurations created successfully"

# Create monitoring configuration
echo "📊 Creating monitoring configuration..."
mkdir -p ~/VGS1/monitoring

cat > ~/VGS1/monitoring/prometheus.yml << 'EOF'
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
      - targets: ['13.115.27.45:5100']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    scrape_timeout: 5s

  - job_name: 'vgs-transaction-index'
    static_configs:
      - targets: ['57.180.14.130:5300']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    scrape_timeout: 5s

  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
EOF

echo "✅ Monitoring configuration created successfully"

echo ""
echo "🎉 Couchbase Configuration Setup Complete!"
echo "=========================================="
echo ""
echo "✅ What was configured:"
echo "   • Couchbase certificate installed at /etc/couchbase/certs/couchbase-cert.pem"
echo "   • Environment variables set in ~/VGS1/.env"
echo "   • Application configurations created for both services"
echo "   • Monitoring configuration updated with instance IPs"
echo ""
echo "📋 Next steps:"
echo "   1. Run this script on all 4 AWS instances"
echo "   2. Follow the deployment guide for building and starting services"
echo "   3. Test connectivity to Couchbase Capella"
echo ""
echo "🔗 Couchbase Connection Details:"
echo "   • Connection String: couchbases://cb.dwmbjb1o-vduuwae.cloud.couchbase.com"
echo "   • Username: vgs"
echo "   • Buckets: embedded_document, transaction_index"
echo "   • Collections: game_rounds, game_transactions"
echo ""
