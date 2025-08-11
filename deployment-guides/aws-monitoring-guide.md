
# Monitoring System Deployment Guide for AWS/Ubuntu

## Overview

This guide will help you set up a comprehensive monitoring system for your VGS application using Grafana and Prometheus on AWS Ubuntu instances. You'll be able to visualize performance metrics, set up alerts, and ensure your gaming application maintains industry-standard performance (<20ms response times, 100% write success).

## What You'll Deploy

- **Prometheus**: Metrics collection and storage system
- **Grafana**: Visual dashboards and alerting
- **Custom Dashboards**: Pre-built VGS gaming metrics dashboards
- **Real-time Monitoring**: Live performance tracking
- **Alerting System**: Notifications when performance degrades

## Prerequisites

1. **AWS Account**: Active AWS account with EC2 access
2. **VGS Application**: Running and exposing metrics endpoints
3. **Ubuntu Server**: 22.04 LTS (recommended)
4. **SSH Client**: PuTTY (Windows) or Terminal (Mac/Linux)

## Step 1: Launch AWS EC2 Instance

1. **Log into AWS Console**:
   - Navigate to EC2 Dashboard
   - Click "Launch Instance"

2. **Configure Instance**:
   - **Name**: "VGS-Monitoring-System"
   - **AMI**: Ubuntu Server 22.04 LTS
   - **Instance Type**: t3.medium (recommended for monitoring)
   - **Key Pair**: Create new or select existing
   - **Security Group**: Create with these rules:
     - SSH (22): Your IP only
     - HTTP (80): Your IP only
     - Custom TCP (3000): Your IP only (Grafana)
     - Custom TCP (9090): Your IP only (Prometheus)

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

1. **Install Basic Tools**:
   ```bash
   sudo apt install curl wget git htop -y
   ```

2. **Create Monitoring User**:
   ```bash
   sudo useradd --no-create-home --shell /bin/false prometheus
   sudo useradd --no-create-home --shell /bin/false grafana
   ```

## Step 4: Install and Configure Prometheus

1. **Download Prometheus**:
   ```bash
   cd /tmp
   wget https://github.com/prometheus/prometheus/releases/download/v2.45.0/prometheus-2.45.0.linux-amd64.tar.gz
   tar -xzf prometheus-2.45.0.linux-amd64.tar.gz
   ```

2. **Install Prometheus**:
   ```bash
   sudo mkdir -p /etc/prometheus /var/lib/prometheus
   sudo cp prometheus-2.45.0.linux-amd64/prometheus /usr/local/bin/
   sudo cp prometheus-2.45.0.linux-amd64/promtool /usr/local/bin/
   sudo cp -r prometheus-2.45.0.linux-amd64/consoles /etc/prometheus/
   sudo cp -r prometheus-2.45.0.linux-amd64/console_libraries /etc/prometheus/
   sudo chown -R prometheus:prometheus /etc/prometheus /var/lib/prometheus
   sudo chmod -R 755 /etc/prometheus /var/lib/prometheus
   ```

3. **Create Prometheus Configuration**:
   ```bash
   sudo nano /etc/prometheus/prometheus.yml
   ```

   Add configuration:
   ```yaml
   global:
     scrape_interval: 15s
     evaluation_interval: 15s

   rule_files:
     - "vgs-alerts.yml"

   scrape_configs:
     - job_name: 'prometheus'
       static_configs:
         - targets: ['localhost:9090']

     - job_name: 'vgs-application'
       static_configs:
         - targets: ['YOUR-VGS-APP-IP:5000']
       metrics_path: '/actuator/prometheus'
       scrape_interval: 5s

     - job_name: 'vgs-application-health'
       static_configs:
         - targets: ['YOUR-VGS-APP-IP:5001']
       metrics_path: '/actuator/prometheus'
       scrape_interval: 10s

     - job_name: 'node-exporter'
       static_configs:
         - targets: ['localhost:9100']

     - job_name: 'benchmark-testing'
       static_configs:
         - targets: ['YOUR-BENCHMARK-IP:8000']
       metrics_path: '/metrics'
       scrape_interval: 30s
   ```

4. **Set Ownership**:
   ```bash
   sudo chown prometheus:prometheus /etc/prometheus/prometheus.yml
   ```

## Step 5: Create Prometheus Service

1. **Create Service File**:
   ```bash
   sudo nano /etc/systemd/system/prometheus.service
   ```

   Add configuration:
   ```ini
   [Unit]
   Description=Prometheus
   Wants=network-online.target
   After=network-online.target

   [Service]
   User=prometheus
   Group=prometheus
   Type=simple
   ExecStart=/usr/local/bin/prometheus \
       --config.file /etc/prometheus/prometheus.yml \
       --storage.tsdb.path /var/lib/prometheus/ \
       --web.console.templates=/etc/prometheus/consoles \
       --web.console.libraries=/etc/prometheus/console_libraries \
       --web.listen-address=0.0.0.0:9090 \
       --web.enable-lifecycle

   [Install]
   WantedBy=multi-user.target
   ```

2. **Start Prometheus Service**:
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable prometheus
   sudo systemctl start prometheus
   ```

3. **Verify Prometheus**:
   ```bash
   sudo systemctl status prometheus
   curl http://localhost:9090/api/v1/targets
   ```

## Step 6: Install Node Exporter

1. **Download Node Exporter**:
   ```bash
   cd /tmp
   wget https://github.com/prometheus/node_exporter/releases/download/v1.6.0/node_exporter-1.6.0.linux-amd64.tar.gz
   tar -xzf node_exporter-1.6.0.linux-amd64.tar.gz
   ```

2. **Install Node Exporter**:
   ```bash
   sudo cp node_exporter-1.6.0.linux-amd64/node_exporter /usr/local/bin/
   sudo chown prometheus:prometheus /usr/local/bin/node_exporter
   ```

3. **Create Node Exporter Service**:
   ```bash
   sudo nano /etc/systemd/system/node_exporter.service
   ```

   Add configuration:
   ```ini
   [Unit]
   Description=Node Exporter
   Wants=network-online.target
   After=network-online.target

   [Service]
   User=prometheus
   Group=prometheus
   Type=simple
   ExecStart=/usr/local/bin/node_exporter

   [Install]
   WantedBy=multi-user.target
   ```

4. **Start Node Exporter**:
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable node_exporter
   sudo systemctl start node_exporter
   ```

## Step 7: Install and Configure Grafana

1. **Add Grafana Repository**:
   ```bash
   wget -q -O - https://packages.grafana.com/gpg.key | sudo apt-key add -
   echo "deb https://packages.grafana.com/oss/deb stable main" | sudo tee -a /etc/apt/sources.list.d/grafana.list
   sudo apt update
   ```

2. **Install Grafana**:
   ```bash
   sudo apt install grafana -y
   ```

3. **Configure Grafana**:
   ```bash
   sudo nano /etc/grafana/grafana.ini
   ```

   Update configuration:
   ```ini
   [server]
   http_addr = 0.0.0.0
   http_port = 3000

   [security]
   admin_user = admin
   admin_password = vgs-monitor123

   [auth.anonymous]
   enabled = false

   [alerting]
   enabled = true
   ```

4. **Start Grafana Service**:
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable grafana-server
   sudo systemctl start grafana-server
   ```

## Step 8: Setup Monitoring Configuration

1. **Download VGS Monitoring Configuration**:
   ```bash
   cd /home/ubuntu
   git clone https://github.com/TonyG13B/VGS-KV.git
   cd VGS-KV/monitoring
   ```

2. **Copy Alert Rules**:
   ```bash
   sudo cp vgs-alerts.yml /etc/prometheus/
   sudo chown prometheus:prometheus /etc/prometheus/vgs-alerts.yml
   ```

3. **Restart Prometheus**:
   ```bash
   sudo systemctl restart prometheus
   ```

## Step 9: Configure Grafana Data Sources and Dashboards

1. **Access Grafana**:
   - Open browser to `http://YOUR-PUBLIC-IP:3000`
   - Login with admin/vgs-monitor123

2. **Add Prometheus Data Source**:
   - Go to Configuration > Data Sources
   - Click "Add data source"
   - Select "Prometheus"
   - URL: `http://localhost:9090`
   - Click "Save & Test"

3. **Import VGS Dashboard**:
   - Go to Create > Import
   - Upload `vgs-dashboard.json`
   - Select Prometheus as data source
   - Click "Import"

## Step 10: Create Alert Rules

1. **Update Prometheus Alert Rules**:
   ```bash
   sudo nano /etc/prometheus/vgs-alerts.yml
   ```

   Add VGS-specific alerts:
   ```yaml
   groups:
   - name: vgs-application
     rules:
     - alert: HighResponseTime
       expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 0.02
       for: 2m
       labels:
         severity: critical
       annotations:
         summary: "VGS Application high response time"
         description: "95th percentile response time is {{ $value }}s"

     - alert: LowSuccessRate
       expr: rate(http_requests_total{status!~"2.."}[5m]) / rate(http_requests_total[5m]) > 0.01
       for: 1m
       labels:
         severity: critical
       annotations:
         summary: "VGS Application low success rate"
         description: "Success rate is below 99%"

     - alert: HighCPUUsage
       expr: 100 - (avg by(instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 80
       for: 5m
       labels:
         severity: warning
       annotations:
         summary: "High CPU usage detected"
         description: "CPU usage is above 80%"

     - alert: HighMemoryUsage
       expr: (node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes > 0.9
       for: 5m
       labels:
         severity: warning
       annotations:
         summary: "High memory usage detected"
         description: "Memory usage is above 90%"

     - alert: VGSApplicationDown
       expr: up{job="vgs-application"} == 0
       for: 1m
       labels:
         severity: critical
       annotations:
         summary: "VGS Application is down"
         description: "VGS Application has been down for more than 1 minute"
   ```

2. **Restart Prometheus**:
   ```bash
   sudo systemctl restart prometheus
   ```

## Step 11: Setup Notification Channels

1. **Configure Email Notifications** (Optional):
   ```bash
   sudo nano /etc/grafana/grafana.ini
   ```

   Add SMTP configuration:
   ```ini
   [smtp]
   enabled = true
   host = smtp.gmail.com:587
   user = your-email@gmail.com
   password = your-app-password
   from_address = your-email@gmail.com
   from_name = VGS Monitoring
   ```

2. **Setup Webhook Notifications**:
   - In Grafana, go to Alerting > Notification Channels
   - Add Slack, Discord, or custom webhook endpoints

## Step 12: Create Custom Dashboards

1. **VGS Performance Dashboard**:
   - Create new dashboard
   - Add panels for:
     - Response time trends
     - Request throughput
     - Error rates
     - Success criteria indicators

2. **System Health Dashboard**:
   - CPU, Memory, Disk usage
   - Network traffic
   - Service status

3. **Gaming Metrics Dashboard**:
   - Active players
   - Transaction volumes
   - Revenue tracking
   - Geographic distribution

## Step 13: Automate Startup and Monitoring

1. **Create Startup Script**:
   ```bash
   nano /home/ubuntu/start-monitoring.sh
   ```

   Add content:
   ```bash
   #!/bin/bash
   
   echo "Starting VGS Monitoring System..."
   
   # Check and start services
   sudo systemctl start prometheus
   sudo systemctl start node_exporter
   sudo systemctl start grafana-server
   
   # Wait for services to start
   sleep 10
   
   # Check service status
   echo "Service Status:"
   sudo systemctl is-active prometheus
   sudo systemctl is-active node_exporter
   sudo systemctl is-active grafana-server
   
   echo "Monitoring system ready!"
   echo "Prometheus: http://$(curl -s checkip.amazonaws.com):9090"
   echo "Grafana: http://$(curl -s checkip.amazonaws.com):3000"
   ```

2. **Make Script Executable**:
   ```bash
   chmod +x /home/ubuntu/start-monitoring.sh
   ```

## Step 14: Backup and Maintenance

1. **Setup Configuration Backup**:
   ```bash
   nano /home/ubuntu/backup-monitoring.sh
   ```

   Add script:
   ```bash
   #!/bin/bash
   
   BACKUP_DIR="/home/ubuntu/monitoring-backup-$(date +%Y%m%d)"
   mkdir -p $BACKUP_DIR
   
   # Backup Prometheus config
   sudo cp -r /etc/prometheus/ $BACKUP_DIR/
   
   # Backup Grafana config
   sudo cp -r /etc/grafana/ $BACKUP_DIR/
   
   # Export Grafana dashboards
   curl -X GET http://admin:vgs-monitor123@localhost:3000/api/search > $BACKUP_DIR/dashboards.json
   
   echo "Backup completed: $BACKUP_DIR"
   ```

2. **Setup Log Rotation**:
   ```bash
   sudo nano /etc/logrotate.d/vgs-monitoring
   ```

   Add configuration:
   ```
   /var/log/grafana/grafana.log {
       daily
       rotate 30
       compress
       delaycompress
       missingok
       notifempty
   }
   ```

## Step 15: Success Criteria Monitoring

### Key Metrics to Track

1. **Response Time Targets**:
   - Average: <20ms ✓
   - 95th percentile: <50ms ✓
   - 99th percentile: <100ms ✓

2. **Reliability Targets**:
   - Write success rate: 100% ✓
   - Error rate: <0.1% ✓
   - Uptime: >99.9% ✓

3. **Performance Targets**:
   - Throughput: >100 TPS ✓
   - Peak handling: >1000 TPS ✓
   - Recovery time: <5 minutes ✓

### Dashboard Validation

Access your dashboards at:
- Prometheus: `http://YOUR-PUBLIC-IP:9090`
- Grafana: `http://YOUR-PUBLIC-IP:3000`

## Troubleshooting

### Common Issues and Solutions

1. **Prometheus Not Starting**:
   ```bash
   sudo journalctl -u prometheus -f
   # Check configuration syntax
   /usr/local/bin/promtool check config /etc/prometheus/prometheus.yml
   ```

2. **Grafana Login Issues**:
   ```bash
   sudo systemctl restart grafana-server
   # Reset admin password
   sudo grafana-cli admin reset-admin-password newpassword
   ```

3. **No Data in Dashboards**:
   ```bash
   # Check Prometheus targets
   curl http://localhost:9090/api/v1/targets
   
   # Verify VGS app metrics endpoint
   curl http://YOUR-VGS-APP-IP:5000/actuator/prometheus
   ```

4. **High Resource Usage**:
   ```bash
   # Monitor system resources
   htop
   
   # Adjust Prometheus retention
   sudo nano /etc/systemd/system/prometheus.service
   # Add: --storage.tsdb.retention.time=15d
   ```

## Success Validation

Your monitoring system is successful when:

- ✅ Prometheus successfully scrapes all configured targets
- ✅ Grafana displays real-time VGS application metrics
- ✅ Alerts trigger appropriately for performance thresholds
- ✅ Dashboards show comprehensive system health
- ✅ Historical data retention works properly
- ✅ All services survive system restart

## Next Steps

1. **Advanced Alerting**: Setup escalation policies and on-call rotations
2. **Distributed Monitoring**: Monitor multiple VGS instances
3. **Capacity Planning**: Use metrics for infrastructure scaling
4. **Integration**: Connect with incident management tools
5. **Custom Metrics**: Add business-specific gaming KPIs

Your monitoring system is now ready to ensure your VGS application maintains gaming industry performance standards on AWS!
