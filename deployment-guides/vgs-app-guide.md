# VGS Application Deployment Guide (Detailed, Beginner-Friendly with AWS GUI Steps)

Hello! This guide is designed for absolute beginners—no technical background required. We'll explain everything in simple terms (e.g., "Spring Boot" is like a framework that makes it easy to build web applications; "Couchbase" is a database that stores your game data in a fast, reliable way). We'll go step by step, with descriptions of what screens look like (like text "screenshots"), tips to avoid common mistakes, and easy fixes if something goes wrong. If you get stuck, the Troubleshooting section has simple solutions.

**Total Time**: 60-90 minutes (most is waiting for things to install and build).

**What This Guide Does**: Sets up your VGS (Video Game System) application on a separate Amazon server. The app handles game transactions using pure key-value operations with Couchbase Capella 7.6, ensuring 100% write success, round data retrievable within ≤50ms, and requests completing within ≤20ms even under high concurrency. It includes two microservices: Embedded Document pattern and Transaction Index pattern.

**Important Note on Costs**: This uses AWS's free tier where possible, but running the server costs about $0.04 per hour (turn it off when not using to save money—we'll show how).

### Prerequisites (What You Need Before Starting)
These are the things you need ready. If you don't have them, set them up first—it's straightforward.

1. **AWS Account**: If you don't have one, go to https://aws.amazon.com in your web browser. Click "Create an AWS Account" (orange button, top right). Enter your email, create a password, and add a credit card for verification (they won't charge for free tier stuff). It takes 5-10 minutes. (Text "screenshot": The page has a big header with "Start building in the console" and the create button.)

2. **Couchbase Capella 7.6 Cluster**: You need a Couchbase Capella database running. If you don't have one, go to https://cloud.couchbase.com, sign up, and create a cluster. Note the connection details (endpoint, username, password, bucket name). (Text "screenshot": Capella dashboard with cluster details showing endpoint like "couchbase://cb-xxxxx.cloud.couchbase.com".)

3. **A Computer with Internet**: Any Windows or Mac is fine. You'll need to open a "terminal" or "command prompt" (a black window for typing commands—we'll tell you exactly what to type).

4. **No Other Tools Needed Yet**: We'll install everything during the guide.

Tip: Write down your AWS login, Capella connection details, and any passwords on a note—you'll need them.

### Step 1: Launch the VGS Application Server in AWS (Create Your EC2 Instance)
We'll rent a virtual computer from Amazon to run your VGS application. This is like renting a computer in the cloud specifically for your game system.

1. Open your web browser (like Chrome) and go to https://console.aws.amazon.com.

2. Sign in with your AWS email and password. (Text "screenshot": Login page with Amazon logo, email field, "Continue" button. Then password page.)

3. On the AWS home page (dashboard), there's a search bar at the top. Type "EC2" and press Enter. Click the "EC2" result in the list. (Text "screenshot": Dashboard with services grid; search shows "EC2" as top result with description "Virtual servers in the cloud".)

4. On the EC2 page, look for the orange "Launch instance" button on the right and click it. (Text "screenshot": Page with left menu (Instances, etc.), main area with "Launch an instance" section and orange button.)

5. In the launch form, under "Name and tags", type "VGS-Application-Server" in the "Name" field (this labels your server so you can find it easily later).

6. Scroll to "Application and OS Images (Amazon Machine Image)". In the search box, type "Ubuntu" and select "Ubuntu Server 22.04 LTS - HVM" from the quick start list (it's free and easy to use—Ubuntu is a type of operating system, like Windows but for servers).

7. Under "Instance type", click the dropdown and choose "t3.large" (this gives your server enough power—it's like choosing a computer's specs; t3.large has 2 "brains" (CPUs) and 8GB "memory" (RAM), good for running Java applications without being expensive).

8. For "Key pair (login)", click "Create new key pair". In the form, name it "vgs-app-key", leave "Key pair type" as RSA, "Private key file format" as .pem, and click "Create key pair". Your browser will download a file called "vgs-app-key.pem"—save it in your Downloads folder (this file is like a digital key to "unlock" your server later; keep it safe and don't share it).

9. Scroll to "Network settings". Click "Edit". Check the boxes for "Allow SSH traffic from" > "Anywhere-IPv4" (this lets you connect from your home computer). Also check "Allow HTTP traffic from the internet" and "Allow HTTPS traffic from the internet" (for the application to communicate).

10. Still in Network settings, click "Add security group rule" to open more "doors" for the application. For the new rules:
    - Type: Custom TCP, Port range: 5100, Source: Anywhere-IPv4, Description: "VGS Embedded Document Port"
    - Type: Custom TCP, Port range: 5101, Source: Anywhere-IPv4, Description: "VGS Embedded Management Port"
    - Type: Custom TCP, Port range: 5300, Source: Anywhere-IPv4, Description: "VGS Transaction Index Port"
    - Type: Custom TCP, Port range: 5301, Source: Anywhere-IPv4, Description: "VGS Transaction Management Port"

11. Leave "Configure storage" as default (1 x 8 GiB gp3 volume—this is like the server's hard drive space, 8GB is plenty).

12. At the bottom right, click the orange "Launch instance" button. (Text "screenshot": A summary sidebar on the right shows your choices; the button is at the bottom.)

13. The next page says "Launch status". Wait 2-3 minutes (refresh if needed). Click "View all instances" to see your new server. Click its name, and in the details pane, note the "Public IPv4 address" (e.g., 3.123.45.67)—this is your server's "phone number" for connecting. Also note the "Instance ID" (like i-0abc123) for reference.

Tip: If you see an error like "Insufficient capacity", try a different "region" (top right of AWS page, change to "US East (Ohio)" or another).

Congrats! Your VGS application server is ready.

### Step 2: Connect to Your VGS Server (Using SSH to "Log In" Remotely)
SSH is a safe way to "remote control" your cloud server from your local computer.

1. On your local computer, open Command Prompt (Windows: press Windows key, type "cmd", open it) or Terminal (Mac: Spotlight search "Terminal").

2. In the window, type `cd Downloads` and press Enter (this goes to the folder where your key file is).

3. Type `ssh -i "vgs-app-key.pem" ubuntu@<your-public-ip>` (replace <your-public-ip> with the address from Step 1) and press Enter. (Explanation: SSH is a secure connection tool; "ubuntu" is the default username for the server.)

4. If it says "The authenticity of host can't be established... Are you sure you want to continue connecting (yes/no)?", type "yes" and press Enter (this is a one-time security check).

5. You're in! The screen will change to a prompt like "ubuntu@ip-172-31-45-26:~$" (the "~$" is where you type commands). (Text "screenshot": Black window with text like "Welcome to Ubuntu 22.04" followed by the prompt.)

Tip: If "command not found", make sure you're in the right folder (use `dir` to list files—vgs-app-key.pem should be there). If "Permission denied", the key file might need permissions fixed (on Mac/Linux: `chmod 400 vgs-app-key.pem`; on Windows, it's usually OK).

### Step 3: Install Required Software on the Server
We need to install Java 21, Maven, and other tools. This is like installing apps on your phone.

1. Type `sudo apt update` and press Enter (updates the server's software list; "sudo" means "do this as the boss"—it might ask for a password, but the default is none, just press Enter. Takes 1 minute.)

2. Type `sudo apt install openjdk-21-jdk maven git wget curl -y` and press Enter (installs Java 21, Maven 3.8+, Git, and other tools. The "-y" says "yes to all questions". Takes 3-5 minutes.)

3. Check Java is installed: Type `java -version` (should show "openjdk version 21.x.x"). Then `mvn -version` (should show "Apache Maven 3.x.x").

4. Set Java environment: Type `echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64' >> ~/.bashrc` and press Enter. Then `source ~/.bashrc` and press Enter (this tells the system where Java is installed).

Tip: If it says "E: Unable to locate package", run `sudo apt update` again—it's just refreshing the list. If "permission denied", add "sudo" to the check commands.

### Step 4: Get the VGS Application Code and Configure It
1. Type `git clone https://github.com/TonyG13B/VGS-KV.git` and press Enter (downloads the project code from your repo. Git is like a downloader for code. Takes 1 minute.)

2. Type `cd VGS-KV/vgs-application` and press Enter (enters the VGS application folder).

3. Type `ls` and press Enter (lists files—you should see "embedded-document" and "transaction-index" folders).

4. Configure the Embedded Document service: Type `cd embedded-document` and press Enter.

5. Type `nano src/main/resources/application.yml` and press Enter (opens a simple text editor called "nano" for the config file. "Nano" is like Notepad but in terminal.)

6. In nano, you'll see configuration like this. Update the Couchbase connection details:
   ```yaml
   spring:
     couchbase:
       connection-string: couchbase://cb-xxxxx.cloud.couchbase.com
       username: your-username
       password: your-password
       bucket:
         name: your-bucket-name
   ```
   Replace the placeholder values with your actual Capella connection details. (Explanation: This tells the app how to connect to your Couchbase database.)

7. Save: Press Ctrl+O, press Enter to confirm file name, then Ctrl+X to exit.

8. Configure the Transaction Index service: Type `cd ../transaction-index` and press Enter.

9. Type `nano src/main/resources/application.yml` and press Enter.

10. Update the same Couchbase connection details as in step 6.

11. Save: Press Ctrl+O, press Enter to confirm file name, then Ctrl+X to exit.

12. Go back to the main folder: Type `cd ..` and press Enter.

Tip: If you don't have your Capella connection details, go to https://cloud.couchbase.com, sign in, click your cluster, and look for "Connection string", "Username", and "Password" in the connection info. If nano doesn't open, install it with `sudo apt install nano -y`.

### Step 5: Build and Test the Application
Now we'll compile the Java code and make sure it works before running it.

1. In the SSH terminal, make sure you're in the right folder: Type `cd VGS-KV/vgs-application` and press Enter.

2. Build the Embedded Document service: Type `cd embedded-document && mvn clean package -DskipTests` and press Enter (compiles the Java code. Takes 2-3 minutes. The "-DskipTests" skips tests for now to save time.)

3. Check the build succeeded: You should see "BUILD SUCCESS" at the end. If there are errors, they'll be shown in red.

4. Build the Transaction Index service: Type `cd ../transaction-index && mvn clean package -DskipTests` and press Enter (compiles the second service. Takes 2-3 minutes.)

5. Check both builds succeeded: You should see "BUILD SUCCESS" for both services.

6. Test the connection: Type `cd ../embedded-document && java -jar target/embedded-document-pattern-1.0.0.jar --spring.profiles.active=test` and press Enter (runs a quick test to make sure it can connect to Couchbase. You should see Spring Boot startup messages.)

7. Stop the test: Press Ctrl+C to stop the test application.

Tip: If the build fails with "Maven not found", run `sudo apt install maven -y` again. If "Connection failed", check your Capella connection details in the application.yml files. If "Port already in use", another application might be running on port 5100.

### Step 6: Set Up Production Configuration
We'll configure the application for optimal performance and reliability.

1. Type `cd VGS-KV/vgs-application/embedded-document` and press Enter.

2. Type `nano src/main/resources/application-prod.yml` and press Enter (creates a production configuration file).

3. Copy this content into nano:
   ```yaml
   server:
     port: 5100
   
   spring:
     application:
       name: vgs-embedded-document
   
   # JVM Optimizations for low latency
   jvm:
     heap-size: 2g
     gc: G1GC
     gc-logging: true
   
   # Couchbase connection pool optimization
   couchbase:
     connection-pool:
       min-size: 10
       max-size: 50
       idle-timeout: 30000
       connect-timeout: 10000
   
   # Metrics for monitoring
   management:
     endpoints:
       web:
         exposure:
           include: health,info,prometheus
     endpoint:
       health:
         show-details: always
   
   # Logging configuration
   logging:
     level:
       com.vgs: INFO
       org.springframework.couchbase: WARN
     pattern:
       console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
   ```

4. Save: Press Ctrl+O, Enter, then Ctrl+X.

5. Type `cd ../transaction-index` and press Enter.

6. Type `nano src/main/resources/application-prod.yml` and press Enter.

7. Copy the same content as above, but change the application name to "vgs-transaction-index".

8. Save: Press Ctrl+O, Enter, then Ctrl+X.

9. Create startup scripts: Type `cd ..` and press Enter.

10. Type `nano start-embedded.sh` and press Enter.

11. Copy this content into nano:
   ```bash
   #!/bin/bash

   # Function to close port if in use
   close_port_if_used() {
       local port=$1
       local service_name=$2

       if lsof -ti:$port >/dev/null 2>&1; then
           echo "Port $port is in use by $service_name. Closing..."
           lsof -ti:$port | xargs kill -9 2>/dev/null || true
           sleep 2
           echo "Port $port freed for $service_name"
       fi
   }

   # Clean up ports before starting
   close_port_if_used 5100 "Embedded Document Service"
   close_port_if_used 5101 "Embedded Document Management"

   cd embedded-document
   nohup java -Xms2g -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+OptimizeStringConcat -jar target/embedded-document-pattern-1.0.0.jar --spring.profiles.active=prod > embedded.log 2>&1 &
   echo "Embedded Document service started on port 5100"
   ```

12. Save: Press Ctrl+O, Enter, then Ctrl+X.

13. Type `nano start-transaction.sh` and press Enter.

14. Copy this content into nano:
   ```bash
   #!/bin/bash

   # Function to close port if in use
   close_port_if_used() {
       local port=$1
       local service_name=$2

       if lsof -ti:$port >/dev/null 2>&1; then
           echo "Port $port is in use by $service_name. Closing..."
           lsof -ti:$port | xargs kill -9 2>/dev/null || true
           sleep 2
           echo "Port $port freed for $service_name"
       fi
   }

   # Clean up ports before starting
   close_port_if_used 5300 "Transaction Index Service"
   close_port_if_used 5301 "Transaction Index Management"

   cd transaction-index
   nohup java -Xms2g -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+OptimizeStringConcat -jar target/transaction-index-pattern-1.0.0.jar --spring.profiles.active=prod > transaction.log 2>&1 &
   echo "Transaction Index service started on port 5300"
   ```

15. Save: Press Ctrl+O, Enter, then Ctrl+X.

16. Make scripts executable: Type `chmod +x start-embedded.sh start-transaction.sh` and press Enter.

Tip: The JVM settings (`-Xms2g -Xmx2g`) allocate 2GB of memory to each service. If your server has less RAM, reduce to 1g. The G1GC garbage collector is optimized for low latency.

### Step 7: Start the VGS Application Services
Now we'll start both microservices and make sure they're running properly.

1. In the SSH terminal, make sure you're in the right folder: Type `cd VGS-KV/vgs-application` and press Enter.

2. Start the Embedded Document service: Type `./start-embedded.sh` and press Enter (starts the first service in the background. You should see "Embedded Document service started on port 5100".)

3. Wait 30 seconds for it to start up, then check if it's running: Type `curl http://0.0.0.0:5101/actuator/health` and press Enter (this checks if the service is healthy. You should see JSON response like `{"status":"UP"}`.)

4. Start the Transaction Index service: Type `./start-transaction.sh` and press Enter (starts the second service in the background. You should see "Transaction Index service started on port 5300".)

5. Wait 30 seconds, then check if it's running: Type `curl http://0.0.0.0:5301/actuator/health` and press Enter (should see similar health response.)

6. Check both services are running: Type `ps aux | grep java` and press Enter (shows running Java processes. You should see two processes with your jar files.)

7. Check the logs: Type `tail -f embedded.log` and press Enter (shows real-time logs from the embedded service. Press Ctrl+C to stop watching.)

8. Check the other service logs: Type `tail -f transaction.log` and press Enter (shows logs from the transaction service. Press Ctrl+C to stop watching.)

Tip: If the health check fails with "Connection refused", the service might still be starting up—wait another 30 seconds and try again. If it still fails, check the logs for error messages.

### Step 8: Test the Application Endpoints
Let's verify the application is working correctly by testing the game transaction endpoints.

1. In the SSH terminal, test the Embedded Document endpoint: Type `curl -X POST http://0.0.0.0:5100/game/transaction -H "Content-Type: application/json" -d '{"playerId":"test123","amount":100,"gameId":"game1"}'` and press Enter (this simulates a game transaction. You should see a JSON response with transaction details.)

2. Test the Transaction Index endpoint: Type `curl -X POST http://0.0.0.0:5300/game/transaction -H "Content-Type: application/json" -d '{"playerId":"test456","amount":200,"gameId":"game2"}'` and press Enter (tests the second service. Should see similar response.)

3. Test round data retrieval: Type `curl http://0.0.0.0:5100/game/round/test123` and press Enter (retrieves round data for the player. Should show transaction history.)

4. Test the other service: Type `curl http://0.0.0.0:5300/game/round/test456` and press Enter (retrieves round data from the second service.)

5. Check response times: Type `time curl -X POST http://0.0.0.0:5100/game/transaction -H "Content-Type: application/json" -d '{"playerId":"test123","amount":100,"gameId":"game1"}'` and press Enter (measures how long the transaction takes. Should be very fast.)

### Step 9: Configure Nginx as a Load Balancer/Reverse Proxy
To make your application accessible via a single IP and manage traffic, we'll set up Nginx.

1. Install Nginx: Type `sudo apt install nginx -y` and press Enter.

2. Configure Nginx: Type `sudo nano /etc/nginx/sites-available/default` and press Enter.

3. Remove existing content and add this configuration:
   ```nginx
   upstream vgs_embedded {
       server 127.0.0.1:5100;
   }

   upstream vgs_transaction {
       server 127.0.0.1:5300;
   }

   server {
       listen 80;
       server_name YOUR-PUBLIC-IP; # Replace with your server's actual public IP or a domain name

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

       location /actuator/embedded {
           proxy_pass http://127.0.0.1:5101/actuator;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
       }

       location /actuator/transaction {
           proxy_pass http://127.0.0.1:5301/actuator;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
       }
   }
   ```
   (Explanation: This tells Nginx to forward requests to the correct microservice based on the URL path.)

4. Save: Press Ctrl+O, Enter, then Ctrl+X.

5. Test Nginx configuration: Type `sudo nginx -t` and press Enter (checks for errors in the config. Should say "syntax is ok" and "test is successful".)

6. Reload Nginx: Type `sudo systemctl reload nginx` and press Enter (applies the new configuration.)

Tip: If Nginx fails to start, check the logs at `/var/log/nginx/error.log`. If you get a "404 Not Found" error when accessing your app, ensure Nginx is running and the configuration is correct.

### Step 10: Access Your VGS Application
Now you can access your application using your server's public IP address.

1. Open your web browser.

2. Go to `http://YOUR-PUBLIC-IP/embedded/` (replace YOUR-PUBLIC-IP with your server's actual public IP address). You should see the Embedded Document service documentation or a default page.

3. Go to `http://YOUR-PUBLIC-IP/transaction/` to access the Transaction Index service.

### Step 11: Monitoring and Maintenance
Keep your application running smoothly with these checks.

1. **Check Application Health**:
   ```bash
   curl http://YOUR-PUBLIC-IP:5101/actuator/health  # Embedded Document
   curl http://YOUR-PUBLIC-IP:5301/actuator/health  # Transaction Index
   ```

2. **Check Application Metrics**:
   ```bash
   curl http://YOUR-PUBLIC-IP:5101/actuator/metrics  # Embedded Document
   curl http://YOUR-PUBLIC-IP:5301/actuator/metrics  # Transaction Index
   ```

3. **View Logs**:
   - Embedded Document service logs: `tail -f /path/to/your/app/embedded.log` (replace with the actual path if different)
   - Transaction Index service logs: `tail -f /path/to/your/app/transaction.log`

4. **Stop Services**:
   - To stop the Embedded Document service: `sudo pkill -f "embedded-document-pattern-1.0.0.jar"`
   - To stop the Transaction Index service: `sudo pkill -f "transaction-index-pattern-1.0.0.jar"`

5. **Turn off the Server (to save costs)**:
   - Go to the EC2 dashboard in AWS.
   - Select your "VGS-Application-Server" instance.
   - Click "Instance state" > "Stop instance".
   - Remember to "Start instance" when you want to use it again.

### Troubleshooting
Common problems and how to fix them.

*   **"Connection refused" / "Site can't be reached"**:
    *   Is the EC2 instance running in AWS?
    *   Are the security group rules correct (ports 5100, 5101, 5300, 5301 open)?
    *   Is Nginx running (`sudo systemctl status nginx`)?
    *   Are the VGS services running (`ps aux | grep java`)? Check their logs.
    *   Did you replace `YOUR-PUBLIC-IP` with the correct IP?

*   **"Port already in use" error during startup**:
    *   The `close_port_if_used` function in the startup scripts should handle this. If it persists, manually kill the process using the port: `sudo lsof -i :<port_number>` to find the process ID, then `sudo kill -9 <PID>`.

*   **Build failed**:
    *   Check for "BUILD FAILURE" messages after running `mvn clean package`.
    *   Ensure Java and Maven are correctly installed (`java -version`, `mvn -version`).
    *   Double-check your `application.yml` for correct Couchbase credentials.

*   **Nginx errors**:
    *   Check Nginx config: `sudo nginx -t`.
    *   View Nginx error logs: `sudo tail -f /var/log/nginx/error.log`.

If you encounter any other issues, please refer to the VGS GitHub repository for more detailed troubleshooting or to report bugs.