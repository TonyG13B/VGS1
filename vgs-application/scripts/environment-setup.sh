
#!/bin/bash

echo "Setting up VGS Application Environment..."
echo "Timestamp: $(date)"

# Update system packages
echo "Updating system packages..."
sudo apt update && sudo apt upgrade -y

# Install Java 21
echo "Installing Java 21..."
sudo apt install -y openjdk-21-jdk

# Install Maven
echo "Installing Maven..."
sudo apt install -y maven

# Verify installations
echo "Verifying installations..."
java -version
mvn -version

# Set JAVA_HOME
echo "Setting JAVA_HOME..."
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64' >> ~/.bashrc

# Create application directories
echo "Creating application directories..."
mkdir -p logs config backup

# Set up application configuration
echo "Setting up configuration..."
cat > config/application-production.yml << 'EOF'
server:
  port: 5000
  address: 0.0.0.0

spring:
  profiles:
    active: production
  application:
    name: vgs-embedded-production

couchbase:
  connection-string: ${COUCHBASE_CONNECTION_STRING}
  username: ${COUCHBASE_USERNAME}
  password: ${COUCHBASE_PASSWORD}
  bucket-name: ${COUCHBASE_BUCKET_NAME:vgs-gaming}
  pool:
    min-endpoints: 6
    max-endpoints: 12

management:
  server:
    port: 5001
    address: 0.0.0.0
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics

logging:
  level:
    root: INFO
    com.vgs: DEBUG
EOF

# Make scripts executable
echo "Making scripts executable..."
chmod +x scripts/*.sh

echo "VGS Application environment setup complete!"
echo "Don't forget to set your Couchbase environment variables!"
