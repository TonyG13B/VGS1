# VGS Application Deployment Guide for AWS/Ubuntu

## Overview

This guide will help you deploy the VGS (Video Game System) application on AWS Ubuntu instances. The VGS application is a high-performance gaming transaction system that handles real-time game transactions with sub-20ms response times.

## What You'll Deploy

- **Embedded Document Service**: Stores transactions within game round documents
- **Transaction Index Service**: Maintains separate transaction indexes for complex queries
- Both services are optimized for gaming industry standards (<20ms response times)

## Prerequisites

1. **AWS Account**: Active AWS account with EC2 access
2. **Ubuntu Server**: 22.04 LTS (recommended)
3. **Couchbase Capella Cluster**: Connection details required
4. **SSH Client**: PuTTY (Windows) or Terminal (Mac/Linux)

## Step 1: Launch AWS EC2 Instance

1. **Log into AWS Console**:
   - Go to https://aws.amazon.com/console/
   - Navigate to EC2 Dashboard
   - Click "Launch Instance"

2. **Configure Instance**:
   - **Name**: "VGS-Gaming-App"
   - **AMI**: Ubuntu Server 22.04 LTS (Free tier eligible)
   - **Instance Type**: t3.medium (minimum recommended)
   - **Key Pair**: Create new or select existing
   - **Security Group**: Create with these rules:
     - SSH (22): Your IP only
     - HTTP (80): 0.0.0.0/0
     - HTTPS (443): 0.0.0.0/0
     - Custom TCP (5100): 0.0.0.0/0 (Embedded Document Service)
     - Custom TCP (5101): 0.0.0.0/0 (Embedded Document Management)
     - Custom TCP (5300): 0.0.0.0/0 (Transaction Index Service)
     - Custom TCP (5301): 0.0.0.0/0 (Transaction Index Management)

3. **Launch Instance**:
   - Review settings and click "Launch"
   - Note the Public IP address

## Step 2: Connect to Your Instance

1. **SSH Connection**:
   ```bash
   chmod 400 your-key.pem
   ssh -i your-key.pem ubuntu@YOUR-PUBLIC-IP
   ```

2. **Update System**:
   ```bash
   sudo apt update && sudo apt upgrade -y
   ```

## Step 3: Install Required Software

1. **Install Java 21**:
   ```bash
   sudo apt install openjdk-21-jdk -y
   export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
   echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64' >> ~/.bashrc
   ```

2. **Install Maven**:
   ```bash
   sudo apt install maven -y
   ```

3. **Install Git**:
   ```bash
   sudo apt install git -y
   ```

4. **Verify Installations**:
   ```bash
   java -version
   mvn -version
   git --version
   ```

## Step 4: Download and Setup Application

1. **Clone Repository**:
   ```bash
   git clone https://github.com/TonyG13B/VGS-KV.git
   cd VGS-KV
   ```

2. **Create Environment Configuration**:
   ```bash
   sudo nano /etc/environment
   ```
   Add these lines:
   ```
   COUCHBASE_CONNECTION_STRING=couchbase://your-cluster.cloud.couchbase.com
   COUCHBASE_USERNAME=your-username
   COUCHBASE_PASSWORD=your-password
   COUCHBASE_BUCKET_NAME=vgs-gaming
   ```

3. **Source Environment**:
   ```bash
   source /etc/environment
   ```

## Step 5: Configure Application for Production

1. **Create Production Configuration**:
   ```bash
   sudo nano vgs-application/embedded-document/src/main/resources/application-prod.yml
   ```

   Update configuration:
   ```yaml
   # AWS Production Configuration
   server:
     port: 5100
     address: 0.0.0.0

   spring:
     profiles:
       active: prod
     application:
       name: vgs-embedded-production

   # Optimized for AWS environment
   couchbase:
     connection-string: ${COUCHBASE_CONNECTION_STRING}
     username: ${COUCHBASE_USERNAME}
     password: ${COUCHBASE_PASSWORD}
     bucket-name: ${COUCHBASE_BUCKET_NAME}
     pool:
       min-endpoints: 8
       max-endpoints: 16
     timeout:
       connect: 10s
       kv: 3s
       query: 30s

   # Threading and async configuration
   async:
     pool:
       core-size: 10
       max-size: 50
       queue-capacity: 100
       thread-name-prefix: "vgs-async-"

   # Circuit breaker configuration
   circuit-breaker:
     failure-rate-threshold: 50
     wait-duration-in-open-state: 30s
     sliding-window-size: 100
     minimum-number-of-calls: 10

   # Cache configuration
   cache:
     type: caffeine
     caffeine:
       spec: maximumSize=10000,expireAfterAccess=5m

   management:
     server:
       port: 5101
       address: 0.0.0.0
     endpoints:
       web:
         exposure:
           include: health,info,prometheus,metrics
         base-path: /actuator
     endpoint:
       health:
         show-details: always

   logging:
     level:
       root: INFO
       com.vgs: DEBUG
       com.couchbase: WARN
     pattern:
       console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
       file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
     file:
       name: /var/log/vgs-application.log
   ```

## Step 6: Build and Deploy Services Using Startup Scripts

1. **Build Both Applications**:
   ```bash
   cd vgs-application/embedded-document
   mvn clean package -DskipTests
   cd ../transaction-index
   mvn clean package -DskipTests
   ```

2. **Make Scripts Executable**:
   ```bash
   cd ../scripts
   chmod +x *.sh
   ```

3. **Start Embedded Document Service**:
   ```bash
   ./start-embedded.sh
   ```
   
   This script will:
   - Check and close port 5100 if in use
   - Start the Embedded Document service on port 5100
   - Display startup logs

4. **Start Transaction Index Service** (in a new terminal):
   ```bash
   ./start-transaction.sh
   ```
   
   This script will:
   - Check and close port 5300 if in use
   - Start the Transaction Index service on port 5300
   - Display startup logs

5. **Alternative: Start All Services at Once**:
   ```bash
   ./start-all.sh
   ```
   
   This will start both services in the background.

6. **Check Service Status**:
   ```bash
   ps aux | grep java
   # You should see both services running on ports 5100 and 5300
   ```

## Step 7: Verify Deployment

1. **Test Embedded Document Service Health**:
   ```bash
   curl http://localhost:5100/actuator/health
   ```
   Should return: `{"status":"UP"}`

2. **Test Transaction Index Service Health**:
   ```bash
   curl http://localhost:5300/actuator/health
   ```
   Should return: `{"status":"UP"}`

3. **Test from External (Embedded Document)**:
   ```bash
   curl http://YOUR-PUBLIC-IP:5100/actuator/health
   ```

4. **Test from External (Transaction Index)**:
   ```bash
   curl http://YOUR-PUBLIC-IP:5300/actuator/health
   ```

5. **Test API Endpoint (Embedded Document)**:
   ```bash
   curl -X POST http://YOUR-PUBLIC-IP:5100/api/v1/transactions \
   -H "Content-Type: application/json" \
   -d '{
     "playerId": "player123",
     "gameId": "game456",
     "amount": 100,
     "transactionType": "BET"
   }'
   ```

## Step 8: Monitor Services

1. **Check Running Services**:
   ```bash
   cd vgs-application/scripts
   ./health-check.sh
   ```
   
   This will show the status of both services and their health endpoints.

2. **View Service Logs**:
   ```bash
   # View embedded document logs
   tail -f /tmp/vgs-embedded.log
   
   # View transaction index logs  
   tail -f /tmp/vgs-transaction.log
   ```

3. **Stop Services When Needed**:
   ```bash
   # Find and stop processes
   pkill -f "embedded-document-pattern"
   pkill -f "transaction-index-pattern"
   ```

## Step 9: Setup Load Balancer (Optional)

1. **Install Nginx**:
   ```bash
   sudo apt install nginx -y
   ```

2. **Configure Load Balancer**:
   ```bash
   sudo nano /etc/nginx/sites-available/vgs-app
   ```

   Add configuration:
   ```nginx
   upstream vgs_embedded {
       server 127.0.0.1:5100;
   }

   upstream vgs_transaction {
       server 127.0.0.1:5300;
   }

   server {
       listen 80;
       server_name YOUR-PUBLIC-IP;

       location /embedded/ {
           proxy_pass http://vgs_embedded/;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
       }

       location /transaction/ {
           proxy_pass http://vgs_transaction/;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
       }

       location /actuator {
           proxy_pass http://vgs_embedded;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
       }
   }
   ```

3. **Enable Configuration**:
   ```bash
   sudo ln -s /etc/nginx/sites-available/vgs-app /etc/nginx/sites-enabled/
   sudo nginx -t
   sudo systemctl restart nginx
   ```

## Step 10: Setup SSL Certificate (Optional)

1. **Install Certbot**:
   ```bash
   sudo apt install certbot python3-certbot-nginx -y
   ```

2. **Obtain Certificate**:
   ```bash
   sudo certbot --nginx -d your-domain.com
   ```

## Step 11: Monitor Performance

1. **Check Application Metrics**:
   ```bash
   # Embedded Document metrics
   curl http://YOUR-PUBLIC-IP:5100/actuator/metrics
   
   # Transaction Index metrics
   curl http://YOUR-PUBLIC-IP:5300/actuator/metrics
   ```

2. **Monitor System Resources**:
   ```bash
   htop
   free -h
   df -h
   ```

3. **Check Service Logs**:
   ```bash
   # Embedded Document logs
   tail -f /tmp/vgs-embedded.log
   
   # Transaction Index logs
   tail -f /tmp/vgs-transaction.log
   ```

4. **Use Health Check Script**:
   ```bash
   cd vgs-application/scripts
   ./health-check.sh
   ```

## Step 12: Performance Optimization

1. **JVM Tuning**:
   Update service files with optimized JVM flags:
   ```
   -Xms4g -Xmx8g
   -XX:+UseG1GC
   -XX:MaxGCPauseMillis=20
   -XX:+UseStringDeduplication
   -XX:+UseCompressedOops
   ```

2. **System Tuning**:
   ```bash
   echo 'vm.swappiness=10' | sudo tee -a /etc/sysctl.conf
   echo 'net.core.rmem_max=268435456' | sudo tee -a /etc/sysctl.conf
   echo 'net.core.wmem_max=268435456' | sudo tee -a /etc/sysctl.conf
   sudo sysctl -p
   ```

## Troubleshooting

### Common Issues and Solutions

1. **Service Won't Start**:
   ```bash
   sudo journalctl -u vgs-embedded.service -n 50
   # Check for port conflicts, memory issues, or configuration errors
   ```

2. **Connection to Couchbase Fails**:
   - Verify environment variables are set correctly
   - Check Couchbase Capella cluster status
   - Ensure IP is whitelisted in Capella

3. **High Response Times**:
   - Check system resources: `htop`, `iostat`
   - Monitor JVM metrics via actuator endpoints
   - Consider scaling to larger instance type

4. **Memory Issues**:
   ```bash
   # Adjust JVM heap size in service configuration
   sudo systemctl edit vgs-embedded.service
   ```

## Success Criteria

Your deployment is successful when:
- ✅ Service starts without errors
- ✅ Health check returns "UP" status
- ✅ Can create transactions via API
- ✅ Can retrieve round data
- ✅ Response times < 20ms
- ✅ 100% success rate for writes
- ✅ Service survives instance restart

## Maintenance

1. **Regular Updates**:
   ```bash
   cd VGS-KV
   git pull origin main
   cd vgs-application/embedded-document
   mvn clean package -DskipTests
   sudo systemctl restart vgs-embedded.service
   ```

2. **Log Rotation**:
   ```bash
   sudo nano /etc/logrotate.d/vgs-application
   ```

3. **Backup Strategy**:
   - Database backups via Couchbase Capella
   - Application configuration backups
   - Regular AMI snapshots

Your VGS application is now ready to handle gaming transactions with industry-standard performance on AWS!