
# Monitoring System Deployment Guide for Replit

## Overview

This guide will help you set up a comprehensive monitoring system for your VGS application using Grafana and Prometheus directly in Replit. You'll be able to visualize performance metrics, set up alerts, and ensure your gaming application maintains industry-standard performance (<20ms response times, 100% write success).

## What You'll Deploy

- **Prometheus**: Metrics collection and storage system
- **Grafana**: Visual dashboards and alerting
- **Custom Dashboards**: Pre-built VGS gaming metrics dashboards
- **Real-time Monitoring**: Live performance tracking
- **Alerting System**: Notifications when performance degrades

## Prerequisites

1. **Replit Account**: Signed up and logged in
2. **VGS Application**: Running and exposing metrics
3. **Basic Understanding**: No coding required, just following instructions

## Step 1: Set Up the Monitoring Environment

1. **Create New Repl**:
   - Click "Create Repl"
   - Choose "Blank Repl" template
   - Name it "VGS-Monitoring-System"
   - Click "Create Repl"

2. **Import Monitoring Configuration**:
   - Open the Shell (bottom panel)
   - Run: `git clone https://github.com/TonyG13B/VGS-KV.git`
   - Run: `cp -r VGS-KV/monitoring/* .`
   - You'll see monitoring configuration files

## Step 2: Configure Environment Variables

1. **Set Up Monitoring Targets**:
   - Click the "Secrets" tool in the left sidebar
   - Add these secrets:

   ```
   VGS_APP_URL=https://your-vgs-app.repl.co
   VGS_APP_PORT=443
   PROMETHEUS_PORT=9090
   GRAFANA_PORT=3000
   GRAFANA_ADMIN_PASSWORD=vgs-monitor123
   ```

2. **Configure Additional Targets** (Optional):
   ```
   BENCHMARK_URL=https://your-benchmark.repl.co
   COUCHBASE_EXPORTER_URL=http://your-capella-exporter:9191
   ```

## Step 3: Install Dependencies

1. **Install Python Requirements**:
   - Run: `pip install prometheus-client grafana-api requests`
   - This installs tools for metrics and dashboard management

2. **Download Monitoring Tools**:
   - Run: `wget https://github.com/prometheus/prometheus/releases/download/v2.45.0/prometheus-2.45.0.linux-amd64.tar.gz`
   - Run: `tar -xzf prometheus-2.45.0.linux-amd64.tar.gz`
   - Run: `wget https://dl.grafana.com/oss/release/grafana-10.0.0.linux-amd64.tar.gz`
   - Run: `tar -xzf grafana-10.0.0.linux-amd64.tar.gz`

## Step 4: Configure Prometheus

1. **Review Prometheus Configuration**:
   - Open `prometheus.yml` in the editor
   - This file defines what metrics to collect:

   ```yaml
   global:
     scrape_interval: 15s
     evaluation_interval: 15s

   scrape_configs:
     - job_name: 'vgs-application'
       static_configs:
         - targets: ['your-vgs-app.repl.co:443']
       metrics_path: '/actuator/prometheus'
       scheme: 'https'
       
     - job_name: 'benchmark-testing'
       static_configs:
         - targets: ['your-benchmark.repl.co:443']
       metrics_path: '/metrics'
       scheme: 'https'
   ```

2. **Update Target URLs**:
   - Replace placeholder URLs with your actual Replit app URLs
   - Ensure metrics endpoints are accessible
   - Save the file

## Step 5: Start Prometheus

1. **Create Prometheus Startup Script**:
   - Create file `start-prometheus.sh`:
   ```bash
   #!/bin/bash
   cd prometheus-2.45.0.linux-amd64
   ./prometheus --config.file=../prometheus.yml --storage.tsdb.path=./data --web.listen-address=0.0.0.0:9090 --web.enable-lifecycle &
   echo "Prometheus started on port 9090"
   ```

2. **Start Prometheus**:
   - Run: `chmod +x start-prometheus.sh`
   - Run: `./start-prometheus.sh`
   - You should see: "Server is ready to receive web requests"

3. **Verify Prometheus**:
   - Replit should show a webview on port 9090
   - Or open your repl URL with `:9090` at the end
   - You should see the Prometheus web interface

4. **Test Metrics Collection**:
   - In Prometheus web interface, go to Status > Targets
   - Your VGS application should show as "UP"
   - If "DOWN", check the target URL and metrics endpoint

## Step 6: Configure Grafana

1. **Create Grafana Configuration**:
   - Create `grafana.ini`:
   ```ini
   [server]
   http_addr = 0.0.0.0
   http_port = 3000
   
   [security]
   admin_user = admin
   admin_password = vgs-monitor123
   
   [auth.anonymous]
   enabled = true
   org_role = Viewer
   ```

2. **Create Grafana Data Directory**:
   - Run: `mkdir -p grafana-data/dashboards`
   - Run: `chmod 777 grafana-data`

## Step 7: Start Grafana

1. **Create Grafana Startup Script**:
   - Create file `start-grafana.sh`:
   ```bash
   #!/bin/bash
   cd grafana-10.0.0
   ./bin/grafana-server --config=../grafana.ini --homepath=. &
   echo "Grafana started on port 3000"
   ```

2. **Start Grafana**:
   - Run: `chmod +x start-grafana.sh`
   - Run: `./start-grafana.sh`
   - Wait for: "HTTP Server Listen"

3. **Access Grafana**:
   - Replit should show a webview on port 3000
   - Or open your repl URL with `:3000` at the end
   - Login with admin/vgs-monitor123

## Step 8: Set Up Data Source

1. **Add Prometheus Data Source**:
   - In Grafana, click "Data Sources" in the left menu
   - Click "Add data source"
   - Select "Prometheus"
   - URL: `http://0.0.0.0:9090`
   - Click "Save & Test"
   - Should show green "Data source is working"

## Step 9: Import VGS Dashboard

1. **Import Pre-built Dashboard**:
   - Click "Dashboards" in left menu
   - Click "Import"
   - Click "Upload JSON file"
   - Select `vgs-dashboard.json` from your files
   - Click "Import"

2. **Dashboard Overview**:
   The dashboard includes panels for:
   - **Response Time**: Average, P95, P99 percentiles
   - **Throughput**: Requests per second
   - **Error Rate**: Percentage of failed requests
   - **Success Criteria**: Visual indicators for <20ms, 100% success
   - **System Metrics**: CPU, memory, JVM stats
   - **Couchbase Metrics**: Database operations and latency

## Step 10: Configure Alerts

1. **Set Up Critical Alerts**:
   - Click on "Response Time" panel
   - Click panel title > Edit
   - Go to "Alert" tab
   - Create alert rule:
     - Name: "High Response Time"
     - Condition: "WHEN avg() IS ABOVE 0.02" (20ms)
     - Evaluation: Every 1m for 2m

2. **Set Up Success Rate Alert**:
   - On "Success Rate" panel
   - Alert when success rate < 100%
   - Critical for gaming applications

3. **Configure Notification Channels**:
   - Go to Alerting > Notification Channels
   - Add email, Slack, or webhook notifications
   - Test the notifications

## Step 11: Create Custom Dashboards

1. **Gaming-Specific Metrics Dashboard**:
   - Click "Create" > "Dashboard"
   - Add panels for:
     - Active players
     - Transaction volume by game
     - Revenue metrics
     - Geographic distribution

2. **Performance Monitoring Dashboard**:
   - Response time trends
   - Throughput patterns
   - Error rate tracking
   - Resource utilization

## Step 12: Set Up Automated Startup

1. **Create Master Startup Script**:
   - Create `start-monitoring.sh`:
   ```bash
   #!/bin/bash
   echo "Starting VGS Monitoring System..."
   ./start-prometheus.sh
   sleep 10
   ./start-grafana.sh
   echo "Monitoring system ready!"
   echo "Prometheus: http://0.0.0.0:9090"
   echo "Grafana: http://0.0.0.0:3000"
   ```

2. **Make Script Executable**:
   - Run: `chmod +x start-monitoring.sh`

3. **Create Replit Run Configuration**:
   - Update `.replit` file to run the monitoring system
   - This ensures it starts when you click "Run"

## Step 13: Monitor Success Criteria

### Key Metrics to Watch

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

### Dashboard Interpretation

**Green Indicators** = Meeting targets
**Yellow Indicators** = Approaching limits
**Red Indicators** = Action required

## Step 14: Advanced Monitoring Features

1. **Custom Metrics Collection**:
   - Add business metrics (player actions, revenue)
   - Gaming-specific KPIs
   - Real-time leaderboards

2. **Historical Analysis**:
   - Performance trends over time
   - Capacity planning data
   - Usage pattern analysis

3. **Multi-Environment Monitoring**:
   - Development vs Production
   - A/B testing comparison
   - Regional performance differences

## Troubleshooting

### Common Issues and Solutions

1. **Prometheus Not Collecting Data**:
   - Check target URLs in prometheus.yml
   - Verify metrics endpoints are accessible
   - Check firewall/CORS settings

2. **Grafana Login Issues**:
   - Verify admin password in grafana.ini
   - Clear browser cache
   - Check Grafana logs for errors

3. **Dashboard Not Showing Data**:
   - Verify Prometheus data source connection
   - Check metric names in queries
   - Ensure time range is appropriate

4. **Memory Issues**:
   - Reduce Prometheus retention period
   - Limit dashboard refresh rate
   - Consider Replit resource limits

### Performance Optimization

1. **Reduce Resource Usage**:
   - Increase scrape intervals
   - Limit metric retention
   - Use recording rules for complex queries

2. **Improve Dashboard Performance**:
   - Use appropriate time ranges
   - Limit concurrent panel queries
   - Cache frequently accessed data

## Step 15: Maintenance and Scaling

1. **Regular Maintenance**:
   - Monitor disk usage
   - Review alert effectiveness
   - Update dashboards based on needs

2. **Scaling Considerations**:
   - More targets = higher resource usage
   - Consider external Prometheus for production
   - Use Grafana Cloud for enterprise features

## Success Validation

Your monitoring system is successful when:

- ✅ Prometheus successfully collects metrics from all targets
- ✅ Grafana displays real-time performance data
- ✅ Alerts trigger appropriately for threshold breaches
- ✅ Dashboards provide clear visibility into system health
- ✅ Success criteria metrics are tracked accurately
- ✅ Historical data retention works properly

## Next Steps

1. **Expand Monitoring**: Add more applications and services
2. **Advanced Alerting**: Set up escalation policies
3. **Integration**: Connect with incident management tools
4. **Automation**: Use monitoring data for auto-scaling
5. **Reporting**: Generate regular performance reports

Your monitoring system is now ready to ensure your VGS application maintains gaming industry performance standards!
