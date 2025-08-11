
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
     - Custom TCP (5000): 0.0.0.0/0
     - Custom TCP (5001): 0.0.0.0/0

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
   sudo nano vgs-application/embedded-document/src/main/resources/application-aws.yml
   ```

   Add configuration:
   ```yaml
   # AWS Production Configuration
   server:
     port: 5000
     address: 0.0.0.0

   spring:
     profiles:
       active: aws
     application:
       name: vgs-embedded-aws

   # Optimized for AWS environment
   couchbase:
     pool:
       min-endpoints: 6
       max-endpoints: 12
     timeout:
       connect: 5s
       kv: 2s

   management:
     server:
       port: 5001
       address: 0.0.0.0
     endpoints:
       web:
         exposure:
           include: health,info,prometheus
         base-path: /actuator

   logging:
     level:
       root: INFO
       com.vgs: DEBUG
   ```

## Step 6: Build and Deploy Embedded Document Service

1. **Build Application**:
   ```bash
   cd vgs-application/embedded-document
   mvn clean package -DskipTests
   ```

2. **Create Service Script**:
   ```bash
   sudo nano /etc/systemd/system/vgs-embedded.service
   ```

   Add service configuration:
   ```ini
   [Unit]
   Description=VGS Embedded Document Service
   After=network.target

   [Service]
   Type=simple
   User=ubuntu
   WorkingDirectory=/home/ubuntu/VGS-KV/vgs-application/embedded-document
   ExecStart=/usr/bin/java -Xms2g -Xmx4g -XX:+UseG1GC -XX:+UseStringDeduplication -jar target/embedded-document-pattern-1.0.0.jar --spring.profiles.active=aws
   Restart=always
   RestartSec=10
   Environment=JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

   [Install]
   WantedBy=multi-user.target
   ```

3. **Start Service**:
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable vgs-embedded.service
   sudo systemctl start vgs-embedded.service
   ```

4. **Check Service Status**:
   ```bash
   sudo systemctl status vgs-embedded.service
   sudo journalctl -u vgs-embedded.service -f
   ```

## Step 7: Verify Deployment

1. **Test Health Endpoint**:
   ```bash
   curl http://localhost:5000/actuator/health
   ```
   Should return: `{"status":"UP"}`

2. **Test from External**:
   ```bash
   curl http://YOUR-PUBLIC-IP:5000/actuator/health
   ```

3. **Test API Endpoint**:
   ```bash
   curl -X POST http://YOUR-PUBLIC-IP:5000/api/v1/transactions \
   -H "Content-Type: application/json" \
   -d '{
     "playerId": "player123",
     "gameId": "game456",
     "amount": 100,
     "transactionType": "BET"
   }'
   ```

## Step 8: Deploy Transaction Index Service (Optional)

1. **Build Transaction Index Service**:
   ```bash
   cd ../transaction-index
   mvn clean package -DskipTests
   ```

2. **Create Service Script**:
   ```bash
   sudo nano /etc/systemd/system/vgs-transaction-index.service
   ```

   Service configuration:
   ```ini
   [Unit]
   Description=VGS Transaction Index Service
   After=network.target

   [Service]
   Type=simple
   User=ubuntu
   WorkingDirectory=/home/ubuntu/VGS-KV/vgs-application/transaction-index
   ExecStart=/usr/bin/java -Xms2g -Xmx4g -XX:+UseG1GC -XX:+UseStringDeduplication -jar target/transaction-index-pattern-1.0.0.jar --spring.profiles.active=aws
   Restart=always
   RestartSec=10
   Environment=JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

   [Install]
   WantedBy=multi-user.target
   ```

3. **Start Service on Different Port**:
   Update application configuration to use port 5001, then:
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable vgs-transaction-index.service
   sudo systemctl start vgs-transaction-index.service
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
   upstream vgs_backend {
       server 127.0.0.1:5000;
       server 127.0.0.1:5001;
   }

   server {
       listen 80;
       server_name YOUR-PUBLIC-IP;

       location / {
           proxy_pass http://vgs_backend;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
       }

       location /actuator {
           proxy_pass http://127.0.0.1:5000;
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
   curl http://YOUR-PUBLIC-IP:5000/actuator/metrics
   ```

2. **Monitor System Resources**:
   ```bash
   htop
   free -h
   df -h
   ```

3. **Check Service Logs**:
   ```bash
   sudo journalctl -u vgs-embedded.service --since "1 hour ago"
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
