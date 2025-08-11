
# VGS Application Deployment Guide for Replit

## Overview

This guide will help you deploy the VGS (Video Game System) application on Replit. The VGS application is a high-performance gaming transaction system that handles real-time game transactions with sub-20ms response times.

## What You'll Deploy

- **Embedded Document Service**: Stores transactions within game round documents
- **Transaction Index Service**: Maintains separate transaction indexes for complex queries
- Both services are optimized for gaming industry standards (<20ms response times)

## Prerequisites

1. **Replit Account**: Sign up at https://replit.com if you don't have one
2. **Couchbase Capella Cluster**: You'll need connection details (we'll help you set this up)
3. **Basic Understanding**: No coding required, just following instructions

## Step 1: Fork and Set Up the Project

1. **Fork the Repository**:
   - Go to the VGS-KV Replit project
   - Click "Fork" to create your own copy
   - Name it "VGS-Gaming-App"

2. **Open Your Fork**:
   - Click on your forked project to open it
   - You'll see the file explorer on the left with folders like `vgs-application`, `benchmark-testing`, etc.

## Step 2: Configure Database Connection

1. **Set Up Environment Variables**:
   - Click the "Secrets" tool in the left sidebar (looks like a lock icon)
   - Add these secrets (click "New Secret" for each):

   ```
   COUCHBASE_CONNECTION_STRING=couchbase://your-cluster.cloud.couchbase.com
   COUCHBASE_USERNAME=your-username  
   COUCHBASE_PASSWORD=your-password
   COUCHBASE_BUCKET_NAME=vgs-gaming
   ```

   Replace the values with your actual Couchbase Capella details.

2. **Don't Have Couchbase Capella?**
   - Go to https://cloud.couchbase.com
   - Sign up for free trial
   - Create a cluster (takes 10-15 minutes)
   - Create a bucket named "vgs-gaming"
   - Note down the connection details

## Step 3: Configure the Application

The application configuration files are already set up to work with Replit's environment. The key configuration file `application-replit.yml` is configured to:

- Use environment variables from Secrets
- Listen on port 5000 (Replit's standard)
- Enable health checks and monitoring
- Optimize for Replit's container environment

## Step 4: Deploy the Embedded Document Service

1. **Start the Service**:
   - Click the "Run" button (big green play button)
   - This will start the "Start Embedded Service" workflow
   - You'll see Maven building the project (takes 2-3 minutes first time)
   - Wait for the message: "Started EmbeddedDocumentApplication"

2. **Verify It's Working**:
   - Once started, you'll see a web preview or URL
   - Click the URL or open `https://your-repl-name.your-username.repl.co`
   - Add `/actuator/health` to the URL to check health status
   - You should see: `{"status":"UP"}`

## Step 5: Test the Application

1. **Test Transaction Creation**:
   - Open a new browser tab with your app URL
   - Add `/swagger-ui.html` to access the API documentation
   - Try the POST `/api/v1/transactions` endpoint
   - Use this test data:
   ```json
   {
     "playerId": "player123",
     "gameId": "game456", 
     "amount": 100,
     "transactionType": "BET"
   }
   ```

2. **Test Data Retrieval**:
   - Try the GET `/api/v1/rounds/{playerId}` endpoint
   - Use "player123" as the playerId
   - You should see the transaction data

## Step 6: Deploy Transaction Index Service (Optional)

If you want to run both services:

1. **Stop Current Service**:
   - Click the "Stop" button in the console

2. **Switch to Transaction Index**:
   - In the workflow dropdown, select "Start Transaction Index Service"
   - Click "Run"
   - This will start the alternative service on port 5001

3. **Access Both Services**:
   - Embedded Document: `https://your-repl-name.your-username.repl.co:5000`
   - Transaction Index: `https://your-repl-name.your-username.repl.co:5001`

## Step 7: Monitor Performance

1. **Check Metrics**:
   - Go to `your-app-url/actuator/metrics`
   - Look for metrics like:
     - `http.server.requests` (response times)
     - `jvm.memory.used` (memory usage)
     - `couchbase.operations` (database operations)

2. **Health Checks**:
   - `your-app-url/actuator/health` shows overall health
   - `your-app-url/actuator/health/couchbase` shows database health

## Step 8: Performance Optimization

The application is pre-configured for optimal performance:

- **JVM Settings**: G1GC garbage collector for low latency
- **Connection Pooling**: Optimized Couchbase connection pool
- **Caching**: Built-in caching for frequently accessed data
- **Async Processing**: Non-blocking I/O for better throughput

## Troubleshooting

### Common Issues and Solutions

1. **"Connection refused" to Couchbase**:
   - Check your Secrets are correctly set
   - Verify Couchbase cluster is running
   - Check your IP is whitelisted in Capella

2. **Application won't start**:
   - Check the console for error messages
   - Ensure Java 21 is being used (should be automatic)
   - Try restarting the Repl

3. **Slow response times**:
   - Check Couchbase cluster location (should be same region)
   - Monitor memory usage in metrics
   - Consider upgrading to Replit Core for better performance

4. **Port binding errors**:
   - The app is configured for port 5000 (Replit standard)
   - Don't change port configurations

## Success Criteria

Your deployment is successful when:
- ✅ Application starts without errors
- ✅ Health check returns "UP" status
- ✅ Can create transactions via API
- ✅ Can retrieve round data
- ✅ Response times < 20ms (check metrics)
- ✅ 100% success rate for writes

## Next Steps

1. **Load Testing**: Use the benchmark testing guide to verify performance
2. **Monitoring**: Set up the monitoring dashboard
3. **Custom Domain**: Configure a custom domain in Replit settings
4. **Scaling**: Consider Replit's Autoscale deployment for production traffic

## Support

If you encounter issues:
1. Check the console logs for error messages
2. Verify all Secrets are correctly configured
3. Test Couchbase connection independently
4. Contact Replit support for platform-specific issues

Your VGS application is now ready to handle gaming transactions with industry-standard performance!
