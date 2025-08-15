# VGS Multi-Instance Deployment Guide
## Complete Setup for 4 AWS Instances with GUI-Focused Instructions

> **🎯 Goal**: Deploy the VGS gaming transaction system across 4 separate AWS instances that can communicate with each other.

> **⏱️ Estimated Time**: 2-3 hours (including waiting time for installations)

> **👥 Target Audience**: Non-technical users with basic computer skills

---

## 📋 Prerequisites Checklist

Before starting, ensure you have:

- ✅ AWS Account with access to EC2 instances
- ✅ EC2 Instance Connect access to all 4 instances
- ✅ Couchbase Capella 7.6 cluster details (connection string, username, password)
- ✅ Basic familiarity with web browsers and copy/paste operations
- ✅ Patience for installations (some steps take 5-10 minutes)

---

## 🏗️ Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   vgs-embedded  │    │ vgs-transaction │    │ vgs-benchmark   │    │  vgs-monitoring │
│   -document     │    │     -index      │    │   -testing      │    │                 │
│                 │    │                 │    │                 │    │                 │
│ IP: 13.115.27.45│    │ IP: 57.180.14.130│   │ IP: 13.231.68.228│   │ IP: 13.115.170.132│
│ Port: 5100      │    │ Port: 5300      │    │ Port: 8089      │    │ Ports: 9090,3000 │
└─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │                       │
         └───────────────────────┼───────────────────────┼───────────────────────┘
                                 │                       │
                    ┌─────────────┴─────────────┐        │
                    │     Couchbase Capella     │        │
                    │      (Cloud Database)     │        │
                    └───────────────────────────┘        │
                                                         │
                    ┌─────────────────────────────────────┘
                    │
                    ▼
            ┌─────────────────┐
            │   Load Testing  │
            │   & Monitoring  │
            └─────────────────┘
```

---

## 🚀 Step-by-Step Deployment Guide

### Phase 1: Prepare All Instances (30 minutes)

#### Step 1.1: Access All 4 Instances via EC2 Instance Connect

**For each instance, follow these steps:**

1. **Open AWS Console**
   - Go to https://console.aws.amazon.com
   - Sign in with your AWS credentials

2. **Navigate to EC2**
   - In the search bar at the top, type "EC2" and click on "EC2"
   - You'll see a dashboard with your instances

3. **Connect to Each Instance**
   - Find your instance in the list (look for the name or IP address)
   - Click the checkbox next to the instance
   - Click the "Connect" button (blue button at the top)
   - In the "Connect to instance" dialog, click "EC2 Instance Connect"
   - Click "Connect" (this opens a terminal in your browser)

4. **Repeat for All 4 Instances**
   - Open 4 browser tabs, one for each instance
   - Keep all terminals open - you'll need them

**Instance Details:**
- **vgs-embedded-document**: 13.115.27.45
- **vgs-transaction-index**: 57.180.14.130  
- **vgs-benchmark-testing**: 13.231.68.228
- **vgs-monitoring**: 13.115.170.132

#### Step 1.2: Update All Instances

**In each terminal, run these commands one by one:**

```bash
# Update package lists
sudo apt update

# Upgrade existing packages
sudo apt upgrade -y

# Install essential tools
sudo apt install -y git wget curl unzip software-properties-common apt-transport-https ca-certificates gnupg lsb-release
```

**⏰ Wait Time**: 5-10 minutes per instance (run this on all 4 instances simultaneously)

---

### Phase 2: Install Java and Maven on Application Instances (20 minutes)

**Only on these 2 instances:**
- vgs-embedded-document (13.115.27.45)
- vgs-transaction-index (57.180.14.130)

#### Step 2.1: Install Java 21

**In each application instance terminal, run:**

```bash
# Add Java repository
sudo add-apt-repository ppa:openjdk-r/ppa -y

# Update package list
sudo apt update

# Install Java 21
sudo apt install -y openjdk-21-jdk

# Verify installation
java -version
```

**Expected Output:**
```
openjdk version "21.0.x" 
OpenJDK Runtime Environment (build 21.0.x+x)
OpenJDK 64-Bit Server VM (build 21.0.x+x, mixed mode, sharing)
```

#### Step 2.2: Install Maven

```bash
# Install Maven
sudo apt install -y maven

# Verify installation
mvn -version
```

**Expected Output:**
```
Apache Maven 3.x.x
Maven home: /usr/share/maven
Java version: 21.0.x
```

#### Step 2.3: Set Java Environment

```bash
# Set JAVA_HOME
echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64' >> ~/.bashrc
source ~/.bashrc

# Verify JAVA_HOME
echo $JAVA_HOME
```

---

### Phase 3: Install Python on Benchmark Instance (10 minutes)

**Only on this instance:**
- vgs-benchmark-testing (13.231.68.228)

#### Step 3.1: Install Python and Dependencies

```bash
# Install Python 3.12 and pip
sudo apt install -y python3.12 python3.12-venv python3-pip

# Verify installation
python3 --version
pip3 --version
```

#### Step 3.2: Install Additional Tools

```bash
# Install additional tools for benchmarking
sudo apt install -y jmeter netcat-openbsd

# Verify JMeter installation
jmeter -v
```

---

### Phase 4: Install Docker on Monitoring Instance (10 minutes)

**Only on this instance:**
- vgs-monitoring (13.115.170.132)

#### Step 4.1: Install Docker

```bash
# Install Docker
sudo apt install -y docker.io docker-compose

# Start Docker service
sudo systemctl start docker
sudo systemctl enable docker

# Add current user to docker group
sudo usermod -aG docker $USER

# Verify installation
docker --version
docker-compose --version
```

**⚠️ Important**: After adding user to docker group, you'll need to reconnect to the instance for changes to take effect.

---

### Phase 5: Download and Configure VGS Code (15 minutes)

**On ALL 4 instances, run these commands:**

#### Step 5.1: Download VGS Code

```bash
# Create project directory
mkdir -p ~/VGS1
cd ~/VGS1

# Download VGS code from GitHub
git clone https://github.com/TonyG13B/VGS1.git .

# Verify download
ls -la
```

**Expected Output:**
```
total XX
drwxr-xr-x  XX user user  XXXX date .
drwxr-xr-x  XX user user  XXXX date ..
drwxr-xr-x  XX user user  XXXX date benchmark-testing
drwxr-xr-x  XX user user  XXXX date deployment-guides
drwxr-xr-x  XX user user  XXXX date monitoring
drwxr-xr-x  XX user user  XXXX date vgs-application
-rw-r--r--   1 user user  XXXX date README.md
-rw-r--r--   1 user user  XXXX date VGS-KV_Codebase_Analysis_and_Recommendations.md
-rw-r--r--   1 user user  XXXX date quick-optimizations.sh
```

#### Step 5.2: Set Up Environment Variables

**Create a configuration file on each instance:**

```bash
# Create configuration file
nano ~/VGS1/.env
```

**Add the following content with your specific Couchbase details:**

```bash
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
```

**Save the file:**
- Press `Ctrl + X`
- Press `Y` to confirm
- Press `Enter` to save

#### Step 5.3: Set Up Couchbase Configuration (Automated)

**🚀 Quick Setup Option (Recommended):**

**On ALL 4 instances, run the automated configuration script:**

```bash
# Download and run the configuration script
cd ~/VGS1
chmod +x setup-couchbase-config.sh
./setup-couchbase-config.sh
```

**This script will automatically:**
- ✅ Create the Couchbase certificate file
- ✅ Set up environment variables with your specific details
- ✅ Configure application properties for both services
- ✅ Update monitoring configuration with instance IPs

**Expected Output:**
```
🔧 VGS Couchbase Configuration Setup
====================================
📋 Setting up Couchbase certificate...
✅ Couchbase certificate created successfully
📝 Creating environment configuration...
✅ Environment configuration created successfully
⚙️ Creating application configurations...
✅ Application configurations created successfully
📊 Creating monitoring configuration...
✅ Monitoring configuration created successfully

🎉 Couchbase Configuration Setup Complete!
==========================================
✅ What was configured:
   • Couchbase certificate installed at /etc/couchbase/certs/couchbase-cert.pem
   • Environment variables set in ~/VGS1/.env
   • Application configurations created for both services
   • Monitoring configuration updated with instance IPs
```

#### Step 5.4: Manual Couchbase Certificate Setup (Alternative)

**If you prefer manual setup, on ALL 4 instances:**

```bash
# Create certificate directory
sudo mkdir -p /etc/couchbase/certs

# Create certificate file
sudo nano /etc/couchbase/certs/couchbase-cert.pem
```

**Add the following certificate content:**

```
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
```

**Save the certificate file:**
- Press `Ctrl + X`
- Press `Y` to confirm
- Press `Enter` to save

**Set proper permissions:**
```bash
sudo chmod 644 /etc/couchbase/certs/couchbase-cert.pem
```

---

### Phase 6: Configure Security Groups (10 minutes)

**In AWS Console:**

1. **Navigate to Security Groups**
   - Go to EC2 Dashboard
   - Click "Security Groups" in the left sidebar

2. **Configure Each Instance's Security Group**

**For vgs-embedded-document (13.115.27.45):**
- Find the security group attached to this instance
- Click "Edit inbound rules"
- Add these rules:
  - Type: Custom TCP, Port: 5100, Source: 0.0.0.0/0, Description: "VGS Embedded Service"
  - Type: Custom TCP, Port: 5101, Source: 0.0.0.0/0, Description: "VGS Embedded Management"

**For vgs-transaction-index (57.180.14.130):**
- Find the security group attached to this instance
- Click "Edit inbound rules"
- Add these rules:
  - Type: Custom TCP, Port: 5300, Source: 0.0.0.0/0, Description: "VGS Transaction Service"
  - Type: Custom TCP, Port: 5301, Source: 0.0.0.0/0, Description: "VGS Transaction Management"

**For vgs-benchmark-testing (13.231.68.228):**
- Find the security group attached to this instance
- Click "Edit inbound rules"
- Add these rules:
  - Type: Custom TCP, Port: 8089, Source: 0.0.0.0/0, Description: "Benchmark Web Interface"

**For vgs-monitoring (13.115.170.132):**
- Find the security group attached to this instance
- Click "Edit inbound rules"
- Add these rules:
  - Type: Custom TCP, Port: 9090, Source: 0.0.0.0/0, Description: "Prometheus"
  - Type: Custom TCP, Port: 3000, Source: 0.0.0.0/0, Description: "Grafana"

---

### Phase 7: Build and Deploy Applications (30 minutes)

#### Step 7.1: Build Embedded Document Service

**On vgs-embedded-document (13.115.27.45):**

```bash
# Navigate to application directory
cd ~/VGS1/vgs-application/embedded-document

# Build the application
mvn clean package -DskipTests

# Verify build success
ls -la target/*.jar
```

**Expected Output:**
```
-rw-r--r-- 1 user user XXXXXXX date embedded-document-pattern-1.0.0.jar
```

#### Step 7.2: Build Transaction Index Service

**On vgs-transaction-index (57.180.14.130):**

```bash
# Navigate to application directory
cd ~/VGS1/vgs-application/transaction-index

# Build the application
mvn clean package -DskipTests

# Verify build success
ls -la target/*.jar
```

**Expected Output:**
```
-rw-r--r-- 1 user user XXXXXXX date transaction-index-pattern-1.0.0.jar
```

#### Step 7.3: Configure Application Properties

**On both application instances, update the configuration:**

**For Embedded Document (13.115.27.45):**
```bash
# Edit application configuration
nano ~/VGS1/vgs-application/embedded-document/src/main/resources/application.yml
```

**Update the Couchbase connection string and other settings:**

```yaml
spring:
  couchbase:
    connection-string: ${COUCHBASE_CONNECTION_STRING}
    username: ${COUCHBASE_USERNAME}
    password: ${COUCHBASE_PASSWORD}
    bucket-name: ${COUCHBASE_EMBEDDED_BUCKET_NAME}
    io:
      min-endpoints: 4
      max-endpoints: 16
    timeout:
      connect: 5s
      key-value: 1000ms
      query: 15s

server:
  port: 5100
  address: 0.0.0.0
```

**For Transaction Index (57.180.14.130):**
```bash
# Edit application configuration
nano ~/VGS1/vgs-application/transaction-index/src/main/resources/application.yml
```

**Update the configuration:**

```yaml
spring:
  couchbase:
    connection-string: ${COUCHBASE_CONNECTION_STRING}
    username: ${COUCHBASE_USERNAME}
    password: ${COUCHBASE_PASSWORD}
    bucket-name: ${COUCHBASE_TRANSACTION_BUCKET_NAME}
    io:
      min-endpoints: 4
      max-endpoints: 16
    timeout:
      connect: 5s
      key-value: 1000ms
      query: 15s

server:
  port: 5300
  address: 0.0.0.0
```

---

### Phase 8: Set Up Monitoring Stack (15 minutes)

**On vgs-monitoring (13.115.170.132):**

#### Step 8.1: Configure Prometheus

```bash
# Navigate to monitoring directory
cd ~/VGS1/monitoring

# Edit Prometheus configuration
nano prometheus.yml
```

**Update the configuration with your instance IPs:**

```yaml
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
```

#### Step 8.2: Start Monitoring Stack

```bash
# Start the monitoring stack
sudo docker-compose up -d

# Verify containers are running
sudo docker ps
```

**Expected Output:**
```
CONTAINER ID   IMAGE                    PORTS                    NAMES
xxxxxxxxxxxx   prom/prometheus:latest   0.0.0.0:9090->9090/tcp   monitoring_prometheus_1
xxxxxxxxxxxx   grafana/grafana:latest   0.0.0.0:3000->3000/tcp   monitoring_grafana_1
xxxxxxxxxxxx   prom/alertmanager:latest 0.0.0.0:9093->9093/tcp   monitoring_alertmanager_1
```

---

### Phase 9: Start Application Services (10 minutes)

#### Step 9.1: Start Embedded Document Service

**On vgs-embedded-document (13.115.27.45):**

```bash
# Navigate to application directory
cd ~/VGS1/vgs-application/embedded-document

# Start the service
nohup java -jar target/embedded-document-pattern-1.0.0.jar > app.log 2>&1 &

# Check if service is running
sleep 10
curl http://localhost:5100/actuator/health
```

**Expected Output:**
```json
{"status":"UP","components":{"couchbase":{"status":"UP"},"diskSpace":{"status":"UP"}}}
```

#### Step 9.2: Start Transaction Index Service

**On vgs-transaction-index (57.180.14.130):**

```bash
# Navigate to application directory
cd ~/VGS1/vgs-application/transaction-index

# Start the service
nohup java -jar target/transaction-index-pattern-1.0.0.jar > app.log 2>&1 &

# Check if service is running
sleep 10
curl http://localhost:5300/actuator/health
```

**Expected Output:**
```json
{"status":"UP","components":{"couchbase":{"status":"UP"},"diskSpace":{"status":"UP"}}}
```

---

### Phase 10: Set Up Benchmark Testing (15 minutes)

**On vgs-benchmark-testing (13.231.68.228):**

#### Step 10.1: Set Up Python Environment

```bash
# Navigate to benchmark directory
cd ~/VGS1/benchmark-testing

# Create virtual environment
python3 -m venv venv

# Activate virtual environment
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt
```

#### Step 10.2: Configure Benchmark Tests

```bash
# Edit Locust configuration
nano locustfile.py
```

**Update the host URLs in the file:**

```python
# Update these lines in the file
class VGSUser(HttpUser):
    # ... existing code ...
    
    def on_start(self):
        # Update the host URL to point to your embedded service
        self.host = "http://13.115.27.45:5100"
        # ... rest of the code ...
```

#### Step 10.3: Test Connectivity

```bash
# Test connection to embedded service
curl http://13.115.27.45:5100/actuator/health

# Test connection to transaction service
curl http://57.180.14.130:5300/actuator/health

# Test connection to monitoring
curl http://13.115.170.132:9090/api/v1/status/config
```

---

### Phase 11: Verification and Testing (10 minutes)

#### Step 11.1: Verify All Services

**Open these URLs in your web browser:**

1. **Embedded Document Service**: http://13.115.27.45:5100/actuator/health
2. **Transaction Index Service**: http://57.180.14.130:5300/actuator/health
3. **Prometheus**: http://13.115.170.132:9090
4. **Grafana**: http://13.115.170.132:3000 (admin/admin)

#### Step 11.2: Test Couchbase Connection

**On both application instances, test the Couchbase connection:**

**On vgs-embedded-document (13.115.27.45):**
```bash
# Test Couchbase connection
curl -X GET "http://localhost:5100/actuator/health" | jq '.components.couchbase'
```

**On vgs-transaction-index (57.180.14.130):**
```bash
# Test Couchbase connection
curl -X GET "http://localhost:5300/actuator/health" | jq '.components.couchbase'
```

**Expected Output:**
```json
{
  "status": "UP",
  "details": {
    "cluster": "UP",
    "bucket": "UP"
  }
}
```

#### Step 11.3: Run a Quick Test

**On vgs-benchmark-testing (13.231.68.228):**

```bash
# Activate virtual environment
cd ~/VGS1/benchmark-testing
source venv/bin/activate

# Run a quick test
locust -f locustfile.py --host=http://13.115.27.45:5100 --users=10 --spawn-rate=2 --run-time=1m --headless
```

---

## 🎯 Success Criteria

Your deployment is successful when:

✅ **All 4 instances are running and accessible**
✅ **Health checks return "UP" status**
✅ **Prometheus can scrape metrics from both services**
✅ **Grafana dashboard shows data**
✅ **Benchmark tests can connect and run**

---

## 🔧 Troubleshooting

### Common Issues and Solutions

#### Issue 1: "Connection Refused" Errors
**Solution**: Check security groups and ensure ports are open

#### Issue 2: "Couchbase Connection Failed"
**Solution**: Verify connection string, username, and password in .env file

#### Issue 3: "Port Already in Use"
**Solution**: Stop existing services and restart

#### Issue 4: "Permission Denied"
**Solution**: Use `sudo` for Docker commands and ensure proper file permissions

---

## 📞 Support

If you encounter issues:

1. **Check the logs**: `tail -f app.log`
2. **Verify connectivity**: Use `curl` commands to test endpoints
3. **Review security groups**: Ensure all required ports are open
4. **Check Couchbase**: Verify database connectivity

---

## 🎉 Congratulations!

You have successfully deployed the VGS gaming transaction system across 4 AWS instances! The system is now ready for:

- **High-performance gaming transactions**
- **Real-time monitoring and alerting**
- **Load testing and performance validation**
- **Production gaming operations**

**Next Steps:**
- Configure Grafana dashboards for monitoring
- Set up automated alerts
- Run comprehensive benchmark tests
- Monitor system performance under load
