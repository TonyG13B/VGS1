
#!/bin/bash

echo "Setting up VGS Environment..."

# Create necessary directories
echo "Creating directory structure..."
mkdir -p logs
mkdir -p temp
mkdir -p backups
mkdir -p test_results

# Set proper permissions
chmod +x deployment-guides/startup-scripts/*.sh
chmod +x deployment-guides/scripts/*.sh

# Install system dependencies if needed
echo "Checking Python dependencies..."
if ! command -v python3 &> /dev/null; then
    echo "Python3 not found. Please install Python3."
    exit 1
fi

if ! command -v pip3 &> /dev/null; then
    echo "pip3 not found. Please install pip3."
    exit 1
fi

# Install benchmark testing requirements
echo "Installing benchmark testing requirements..."
cd benchmark-testing
pip3 install -r requirements.txt
cd ..

# Verify Java installation
echo "Checking Java installation..."
if ! command -v java &> /dev/null; then
    echo "Java not found. Please install Java 21."
    exit 1
fi

java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
echo "Java version: $java_version"

# Verify Maven installation
echo "Checking Maven installation..."
if ! command -v mvn &> /dev/null; then
    echo "Maven not found. Please install Maven."
    exit 1
fi

mvn_version=$(mvn -version | head -n 1)
echo "Maven version: $mvn_version"

# Set environment variables
echo "Setting up environment variables..."
export JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}
export SPRING_PROFILES_ACTIVE=replit

# Create environment file
cat > .env << EOF
# VGS Environment Configuration
JAVA_HOME=${JAVA_HOME}
SPRING_PROFILES_ACTIVE=replit
VGS_APP_HOST=http://0.0.0.0:5000
VGS_HEALTH_PORT=5001
BENCHMARK_PORT=8000
LOG_LEVEL=INFO
EOF

echo "Environment setup complete!"
echo "Don't forget to set your Couchbase connection details in Secrets:"
echo "- COUCHBASE_CONNECTION_STRING"
echo "- COUCHBASE_USERNAME"
echo "- COUCHBASE_PASSWORD"
echo "- COUCHBASE_BUCKET_NAME"
