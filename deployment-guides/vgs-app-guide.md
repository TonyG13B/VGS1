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

10. Still in Network settings, click "Add security group rule" to open more "doors" for the application. For the new rule:
    - Type: Custom TCP
    - Port range: 5100 (for VGS application)
    - Source: Anywhere-IPv4
    - Description: "VGS Application Port"
    - Add another for port 8080 (for health checks): Repeat, set port 8080, description "Health Check Port".
    (Text "screenshot": A table with "Security group rule ID", "Type", "Protocol", "Port range", "Source", "Description". Your new rows will be at the bottom.)

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

1. In the SSH terminal, type `sudo apt update` and press Enter (updates the server's software list; "sudo" means "do this as the boss"—it might ask for a password, but the default is none, just press Enter. Takes 1 minute.)

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

6. Test the connection: Type `cd ../embedded-document && java -jar target/embedded-document-0.0.1-SNAPSHOT.jar --spring.profiles.active=test` and press Enter (runs a quick test to make sure it can connect to Couchbase. You should see Spring Boot startup messages.)

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
   cd embedded-document
   nohup java -Xms2g -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+OptimizeStringConcat -jar target/embedded-document-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod > embedded.log 2>&1 &
   echo "Embedded Document service started on port 5100"
   ```

12. Save: Press Ctrl+O, Enter, then Ctrl+X.

13. Type `nano start-transaction.sh` and press Enter.

14. Copy this content into nano:
   ```bash
   #!/bin/bash
   cd transaction-index
   nohup java -Xms2g -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+OptimizeStringConcat -jar target/transaction-index-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod > transaction.log 2>&1 &
   echo "Transaction Index service started on port 5101"
   ```

15. Save: Press Ctrl+O, Enter, then Ctrl+X.

16. Make scripts executable: Type `chmod +x start-embedded.sh start-transaction.sh` and press Enter.

Tip: The JVM settings (`-Xms2g -Xmx2g`) allocate 2GB of memory to each service. If your server has less RAM, reduce to 1g. The G1GC garbage collector is optimized for low latency.

### Step 7: Start the VGS Application Services
Now we'll start both microservices and make sure they're running properly.

1. In the SSH terminal, make sure you're in the right folder: Type `cd VGS-KV/vgs-application` and press Enter.

2. Start the Embedded Document service: Type `./start-embedded.sh` and press Enter (starts the first service in the background. You should see "Embedded Document service started on port 5100".)

3. Wait 30 seconds for it to start up, then check if it's running: Type `curl http://localhost:5100/actuator/health` and press Enter (this checks if the service is healthy. You should see JSON response like `{"status":"UP"}`.)

4. Start the Transaction Index service: Type `./start-transaction.sh` and press Enter (starts the second service in the background. You should see "Transaction Index service started on port 5101".)

5. Wait 30 seconds, then check if it's running: Type `curl http://localhost:5101/actuator/health` and press Enter (should see similar health response.)

6. Check both services are running: Type `ps aux | grep java` and press Enter (shows running Java processes. You should see two processes with your jar files.)

7. Check the logs: Type `tail -f embedded.log` and press Enter (shows real-time logs from the embedded service. Press Ctrl+C to stop watching.)

8. Check the other service logs: Type `tail -f transaction.log` and press Enter (shows logs from the transaction service. Press Ctrl+C to stop watching.)

Tip: If the health check fails with "Connection refused", the service might still be starting up—wait another 30 seconds and try again. If it still fails, check the logs for error messages.

### Step 8: Test the Application Endpoints
Let's verify the application is working correctly by testing the game transaction endpoints.

1. In the SSH terminal, test the Embedded Document endpoint: Type `curl -X POST http://localhost:5100/game/transaction -H "Content-Type: application/json" -d '{"playerId":"test123","amount":100,"gameId":"game1"}'` and press Enter (this simulates a game transaction. You should see a JSON response with transaction details.)

2. Test the Transaction Index endpoint: Type `curl -X POST http://localhost:5101/game/transaction -H "Content-Type: application/json" -d '{"playerId":"test456","amount":200,"gameId":"game2"}'` and press Enter (tests the second service. Should see similar response.)

3. Test round data retrieval: Type `curl http://localhost:5100/game/round/test123` and press Enter (retrieves round data for the player. Should show transaction history.)

4. Test the other service: Type `curl http://localhost:5101/game/round/test456` and press Enter (retrieves round data from the second service.)

5. Check response times: Type `time curl -X POST htt
