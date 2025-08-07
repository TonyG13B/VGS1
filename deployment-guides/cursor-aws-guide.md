# Installing Cursor IDE on AWS EC2 Instance (Complete Guide for Non-Technical Users)

Hello! This guide will help you install Cursor IDE (a powerful code editor) directly on an AWS cloud server. This solves the file creation and GitHub pushing issues we've been experiencing. Think of it like having a full development computer in the cloud that you can access from anywhere through your web browser.

**Total Time**: 45-60 minutes (most is waiting for things to install and download).

**What This Guide Does**: Sets up Cursor IDE on an AWS Ubuntu server with a graphical desktop environment, so you can edit code, create files, and push to GitHub directly from the cloud. This eliminates the Windows PowerShell issues we've been having.

**Important Note on Costs**: This uses a larger AWS instance (t3.large or t3.xlarge) to support the graphical interface, costing about $0.08-0.16 per hour. Turn it off when not using to save money.

### Prerequisites (What You Need Before Starting)
These are the things you need ready. If you don't have them, set them up first—it's straightforward.

1. **AWS Account**: If you don't have one, go to https://aws.amazon.com in your web browser. Click "Create an AWS Account" (orange button, top right). Enter your email, create a password, and add a credit card for verification (they won't charge for free tier stuff). It takes 5-10 minutes.

2. **A Computer with Internet**: Any Windows or Mac is fine. You'll need to open a "terminal" or "command prompt" (a black window for typing commands—we'll tell you exactly what to type).

3. **No Other Tools Needed Yet**: We'll install everything during the guide.

Tip: Write down your AWS login and any passwords on a note—you'll need them.

### Step 1: Launch the Cursor Development Server in AWS (Create Your EC2 Instance)
We'll rent a virtual computer from Amazon with enough power to run Cursor IDE and a graphical desktop.

1. Open your web browser (like Chrome) and go to https://console.aws.amazon.com.

2. Sign in with your AWS email and password. (Text "screenshot": Login page with Amazon logo, email field, "Continue" button. Then password page.)

3. On the AWS home page (dashboard), there's a search bar at the top. Type "EC2" and press Enter. Click the "EC2" result in the list. (Text "screenshot": Dashboard with services grid; search shows "EC2" as top result with description "Virtual servers in the cloud".)

4. On the EC2 page, look for the orange "Launch instance" button on the right and click it. (Text "screenshot": Page with left menu (Instances, etc.), main area with "Launch an instance" section and orange button.)

5. In the launch form, under "Name and tags", type "Cursor-Development-Server" in the "Name" field (this labels your server so you can find it easily later).

6. Scroll to "Application and OS Images (Amazon Machine Image)". In the search box, type "Ubuntu" and select "Ubuntu Server 22.04 LTS - HVM" from the quick start list (it's free and easy to use—Ubuntu is a type of operating system, like Windows but for servers).

7. Under "Instance type", click the dropdown and choose "t3.xlarge" (this gives your server enough power for graphics—it's like choosing a computer's specs; t3.xlarge has 4 "brains" (CPUs) and 16GB "memory" (RAM), good for running Cursor IDE and desktop environment).

8. For "Key pair (login)", click "Create new key pair". In the form, name it "cursor-dev-key", leave "Key pair type" as RSA, "Private key file format" as .pem, and click "Create key pair". Your browser will download a file called "cursor-dev-key.pem"—save it in your Downloads folder (this file is like a digital key to "unlock" your server later; keep it safe and don't share it).

9. Scroll to "Network settings". Click "Edit". Check the boxes for "Allow SSH traffic from" > "Anywhere-IPv4" (this lets you connect from your home computer). Also check "Allow HTTP traffic from the internet" and "Allow HTTPS traffic from the internet" (for the desktop environment to communicate).

10. Still in Network settings, click "Add security group rule" to open more "doors" for the tools. For the new rules:
    - Type: Custom TCP
    - Port range: 6080 (for VNC viewer)
    - Source: Anywhere-IPv4
    - Description: "VNC Desktop Access"
    - Add another for port 8080: Repeat, set port 8080, description "Web Access".
    (Text "screenshot": A table with "Security group rule ID", "Type", "Protocol", "Port range", "Source", "Description". Your new rows will be at the bottom.)

11. Leave "Configure storage" as default (1 x 8 GiB gp3 volume—this is like the server's hard drive space, 8GB is plenty).

12. At the bottom right, click the orange "Launch instance" button. (Text "screenshot": A summary sidebar on the right shows your choices; the button is at the bottom.)

13. The next page says "Launch status". Wait 2-3 minutes (refresh if needed). Click "View all instances" to see your new server. Click its name, and in the details pane, note the "Public IPv4 address" (e.g., 3.123.45.67)—this is your server's "phone number" for connecting. Also note the "Instance ID" (like i-0abc123) for reference.

Tip: If you see an error like "Insufficient capacity", try a different "region" (top right of AWS page, change to "US East (Ohio)" or another).

Congrats! Your Cursor development server is ready.

### Step 2: Connect to Your Development Server (Using SSH to "Log In" Remotely)
SSH is a safe way to "remote control" your cloud server from your local computer.

1. On your local computer, open Command Prompt (Windows: press Windows key, type "cmd", open it) or Terminal (Mac: Spotlight search "Terminal").

2. In the window, type `cd Downloads` and press Enter (this goes to the folder where your key file is).

3. Type `ssh -i "cursor-dev-key.pem" ubuntu@<your-public-ip>` (replace <your-public-ip> with the address from Step 1) and press Enter. (Explanation: SSH is a secure connection tool; "ubuntu" is the default username for the server.)

4. If it says "The authenticity of host can't be established... Are you sure you want to continue connecting (yes/no)?", type "yes" and press Enter (this is a one-time security check).

5. You're in! The screen will change to a prompt like "ubuntu@ip-172-31-45-26:~$" (the "~$" is where you type commands). (Text "screenshot": Black window with text like "Welcome to Ubuntu 22.04" followed by the prompt.)

Tip: If "command not found", make sure you're in the right folder (use `dir` to list files—cursor-dev-key.pem should be there). If "Permission denied", the key file might need permissions fixed (on Mac/Linux: `chmod 400 cursor-dev-key.pem`; on Windows, it's usually OK).

### Step 3: Install Desktop Environment and Required Software
We need to install a graphical desktop environment so you can use Cursor IDE with a visual interface.

1. In the SSH terminal, type `sudo apt update` and press Enter (updates the server's software list; "sudo" means "do this as the boss"—it might ask for a password, but the default is none, just press Enter. Takes 1 minute.)

2. Install desktop environment: Type `sudo apt install ubuntu-desktop xfce4 xfce4-goodies -y` and press Enter (installs Ubuntu's graphical desktop environment. The "-y" says "yes to all questions". Takes 10-15 minutes—this is the longest step.)

3. Install VNC server for remote desktop access: Type `sudo apt install tightvncserver -y` and press Enter (installs VNC server to access the desktop remotely. Takes 2-3 minutes.)

4. Install additional tools: Type `sudo apt install curl wget git unzip -y` and press Enter (installs useful tools. Takes 1 minute.)

5. Install Java (needed for some development tools): Type `sudo apt install openjdk-21-jdk -y` and press Enter (installs Java 21. Takes 2-3 minutes.)

6. Set Java environment: Type `echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64' >> ~/.bashrc` and press Enter. Then `source ~/.bashrc` and press Enter (this tells the system where Java is installed).

Tip: If it says "E: Unable to locate package", run `sudo apt update` again—it's just refreshing the list. If "permission denied", add "sudo" to the check commands.

### Step 4: Set Up VNC Server for Remote Desktop Access
VNC allows you to see and control the graphical desktop from your local computer.

1. In the SSH terminal, type `vncserver` and press Enter (starts the VNC server. It will ask for a password—create a simple password like "cursor123" and remember it. It will also ask if you want a view-only password—type "n" for no.)

2. Note the display number: You'll see output like "New 'X' desktop is ip-172-31-45-26:1" (the ":1" is the display number—remember this).

3. Stop the VNC server: Type `vncserver -kill :1` and press Enter (stops the server so we can configure it properly).

4. Create VNC configuration: Type `mkdir -p ~/.vnc` and press Enter (creates a configuration folder).

5. Type `nano ~/.vnc/xstartup` and press Enter (opens a configuration file for VNC).

6. In nano, copy and paste this content:
   ```bash
   #!/bin/bash
   xrdb $HOME/.Xresources
   startxfce4 &
   ```

7. Save: Press Ctrl+O, press Enter to confirm file name, then Ctrl+X to exit.

8. Make the script executable: Type `chmod +x ~/.vnc/xstartup` and press Enter (makes the script runnable).

9. Start VNC server again: Type `vncserver -geometry 1920x1080 -depth 24` and press Enter (starts the server with a full HD resolution. Enter your password when prompted.)

10. Note the VNC address: You'll see output like "New 'X' desktop is ip-172-31-45-26:1" (remember this for the next step).

Tip: If VNC fails to start, try `vncserver -kill :1` then `vncserver` again. If you forget your password, you can reset it by deleting `~/.vnc/passwd` and running `vncserver` again.

### Step 5: Download and Install Cursor IDE
Now we'll download and install Cursor IDE on the server.

1. In the SSH terminal, download Cursor IDE: Type `wget https://download.cursor.sh/linux/cursor_latest_amd64.deb` and press Enter (downloads the latest Cursor IDE for Linux. Takes 2-3 minutes depending on your internet speed.)

2. Install Cursor: Type `sudo dpkg -i cursor_latest_amd64.deb` and press Enter (installs Cursor IDE. Takes 1 minute.)

3. Fix any dependency issues: Type `sudo apt --fix-broken install -y` and press Enter (fixes any missing dependencies. Takes 1 minute.)

4. Verify Cursor is installed: Type `cursor --version` and press Enter (should show the Cursor version number).

5. Create a desktop shortcut: Type `echo "[Desktop Entry]
   Name=Cursor
   Comment=Code Editor
   Exec=cursor
   Icon=cursor
   Terminal=false
   Type=Application
   Categories=Development;" > ~/Desktop/cursor.desktop` and press Enter (creates a desktop icon for Cursor).

6. Make the shortcut executable: Type `chmod +x ~/Desktop/cursor.desktop` and press Enter.

Tip: If the download fails with "wget: command not found", install it with `sudo apt install wget -y`. If the installation fails, try `sudo apt update && sudo apt install -f` to fix broken packages.

### Step 6: Set Up Git and GitHub Access
We need to configure Git so you can push changes to your GitHub repository.

1. In the SSH terminal, configure Git: Type `git config --global user.name "Your Name"` and press Enter (replace "Your Name" with your actual name).

2. Type `git config --global user.email "your-email@example.com"` and press Enter (replace with your actual email).

3. Generate SSH key for GitHub: Type `ssh-keygen -t rsa -b 4096 -C "your-email@example.com"` and press Enter (creates a secure key for GitHub access. Press Enter for all prompts to use default settings.)

4. Display the public key: Type `cat ~/.ssh/id_rsa.pub` and press Enter (shows your public key. Copy this entire output—you'll need it for GitHub.)

5. Add the key to GitHub:
   - Open your web browser and go to https://github.com
   - Sign in to your GitHub account
   - Click your profile picture (top right) > "Settings"
   - In the left sidebar, click "SSH and GPG keys"
   - Click "New SSH key"
   - Give it a title like "Cursor AWS Server"
   - Paste the public key you copied from step 4 into the "Key" field
   - Click "Add SSH key"

6. Test the connection: Type `ssh -T git@github.com` and press Enter (tests if GitHub recognizes your key. You should see "Hi username! You've successfully authenticated...")

Tip: If you get "Permission denied (publickey)", make sure you copied the entire public key (it should start with "ssh-rsa" and end with your email). If you get "Host key verification failed", type `ssh-keygen -R github.com` and try again.

### Step 7: Clone Your Repository and Set Up the Project
Now we'll get your VGS-KV repository and set up the development environment.

1. In the SSH terminal, clone your repository: Type `git clone git@github.com:TonyG13B/VGS-KV.git` and press Enter (downloads your repository from GitHub. Takes 1 minute.)

2. Navigate to the project: Type `cd VGS-KV` and press Enter (enters the project folder).

3. Check the contents: Type `ls -la` and press Enter (lists all files and folders. You should see your project structure.)

4. Set up the project structure: Type `mkdir -p vgs-application benchmark-testing monitoring deployment-guides` and press Enter (creates the folders we need if they don't exist).

5. Check if folders were created: Type `ls -la` and press Enter (should show the new folders).

Tip: If the git clone fails with "Permission denied", make sure you added the SSH key to GitHub correctly in step 6. If folders already exist, that's fine—the mkdir command will just skip them.

### Step 8: Access the Desktop Environment and Launch Cursor
Now we'll connect to the graphical desktop and start using Cursor IDE.

1. **Install a VNC Viewer on your local computer**:
   - **Windows**: Download "TightVNC Viewer" from https://www.tightvnc.com/download.php
   - **Mac**: Download "VNC Viewer" from https://www.realvnc.com/en/connect/download/viewer/
   - **Linux**: Install with `sudo apt install xtightvncviewer` or download from the TightVNC website

2. **Connect to your server's desktop**:
   - Open your VNC Viewer application
   - In the connection field, enter: `<your-public-ip>:1` (replace with your server's IP from Step 1)
   - Click "Connect"
   - Enter the VNC password you created in Step 4 (e.g., "cursor123")

3. **You should now see the Ubuntu desktop!** (Text "screenshot": A full Ubuntu desktop with icons, taskbar, and menu bar—looks like a normal computer desktop.)

4. **Launch Cursor IDE**:
   - Double-click the "Cursor" icon on the desktop, OR
   - Click the Applications menu (top left) > Development > Cursor, OR
   - Right-click on the desktop > "Open Terminal" and type `cursor` and press Enter

5. **Cursor IDE will open!** (Text "screenshot": Cursor IDE window with file browser on the left, main editing area, and various panels.)

6. **Open your project**:
   - In Cursor, click "File" > "Open Folder"
   - Navigate to `/home/ubuntu/VGS-KV`
   - Click "Open"
   - You should now see your project files in the file browser on the left

Tip: If the VNC connection fails, check that port 6080 is open in your AWS security group (Step 1, item 10). If Cursor doesn't start, try opening a terminal and running `cursor --no-sandbox`.

### Step 9: Test File Creation and GitHub Operations
Let's verify that everything works correctly by creating a test file and pushing it to GitHub.

1. **In Cursor IDE, create a test file**:
   - Right-click on the "deployment-guides" folder in the file browser
   - Select "New File"
   - Name it "test-file.md"
   - Add some content like:
     ```markdown
     # Test File
     This is a test file created in Cursor IDE on AWS.
     Created on: $(date)
     ```

2. **Save the file**: Press Ctrl+S or click "File" > "Save"

3. **Open the integrated terminal in Cursor**:
   - Press Ctrl+` (backtick) OR
   - Click "Terminal" > "New Terminal"
   - A terminal panel will open at the bottom of Cursor

4. **Stage and commit the file**:
   - In the terminal, type `git add deployment-guides/test-file.md` and press Enter
   - Type `git commit -m "Test: Created file in Cursor AWS environment"` and press Enter
   - Type `git push origin main` and press Enter

5. **Verify the push worked**:
   - Open your web browser and go to https://github.com/TonyG13B/VGS-KV
   - You should see the new "test-file.md" in the deployment-guides folder
   - Click on it to see the content

6. **Success!** If you can see the file on GitHub, everything is working correctly.

Tip: If the git push fails, make sure you're in the right directory. In the terminal, type `pwd` to check your current directory—it should show `/home/ubuntu/VGS-KV`. If not, type `cd /home/ubuntu/VGS-KV` first.

### Step 10: Create the Detailed Deployment Guides
Now you can create the comprehensive guides we've been trying to create. Let's do this step by step.

1. **In Cursor IDE, create the VGS Application Guide**:
   - Right-click on "deployment-guides" folder
   - Select "New File"
   - Name it "vgs-app-guide.md"
   - Copy and paste the complete VGS application guide content I provided earlier

2. **Create the Benchmark Testing Guide**:
   - Right-click on "deployment-guides" folder
   - Select "New File"
   - Name it "benchmark-guide.md"
   - Copy and paste the complete benchmark testing guide content I provided earlier

3. **Create the Monitoring Guide**:
   - Right-click on "deployment-guides" folder
   - Select "New File"
   - Name it "monitoring-guide.md"
   - Copy and paste the complete monitoring guide content I provided earlier

4. **Save all files**: Press Ctrl+S for each file

5. **Commit and push all guides**:
   - In the terminal, type `git add deployment-guides/*.md` and press Enter
   - Type `git commit -m "Add comprehensive deployment guides with detailed GUI steps"` and press Enter
   - Type `git push origin main` and press Enter

6. **Verify on GitHub**: Go to https://github.com/TonyG13B/VGS-KV and check that all three guides are now in the deployment-guides folder with complete content.

### Step 11: Set Up Auto-Save and Backup
Let's configure Cursor to automatically save your work and set up regular backups.

1. **Configure auto-save in Cursor**:
   - Click "File" > "Preferences" > "Settings" (or press Ctrl+,)
   - Search for "auto save"
   - Set "Files: Auto Save" to "afterDelay"
   - Set "Files: Auto Save Delay" to 1000 (1 second)

2. **Set up automatic git commits** (optional):
   - In the terminal, create a backup script: Type `nano ~/backup-script.sh` and press Enter
   - Add this content:
     ```bash
     #!/bin/bash
     cd /home/ubuntu/VGS-KV
     git add .
     git commit -m "Auto-backup: $(date)"
     git push origin main
     ```
   - Save: Press Ctrl+O, Enter, then Ctrl+X
   - Make it executable: Type `chmod +x ~/backup-script.sh` and press Enter

3. **Set up automatic backup** (optional):
   - Type `crontab -e` and press Enter
   - Add this line: `0 */2 * * * ~/backup-script.sh` (backs up every 2 hours)
   - Save: Press Ctrl+O, Enter, then Ctrl+X

### Step 12: Optimize Performance and Security
Let's make sure your development environment is secure and performs well.

1. **Set up a firewall** (optional but recommended):
   - Type `sudo ufw enable` and press Enter
   - Type `sudo ufw allow ssh` and press Enter
   - Type `sudo ufw allow 6080` and press Enter (for VNC)
   - Type `sudo ufw status` and press Enter (should show the rules)

2. **Install additional development tools** (optional):
   - Type `sudo apt install build-essential python3-pip nodejs npm -y` and press Enter (installs common development tools)

3. **Set up screen saver and power management**:
   - Right-click on the desktop > "Display Settings"
   - Set "Blank screen" to "Never" (so your session doesn't timeout)
   - Set "Automatic suspend" to "Off"

4. **Configure Cursor for better performance**:
   - In Cursor, go to "File" > "Preferences" > "Settings"
   - Search for "memory" and set "Memory: Max" to 4096 (4GB)
   - Search for "telemetry" and disable it if you prefer

### Step 13: Create a Startup Script for Easy Access
Let's create a script to easily start your development environment.

1. **In the terminal, create a startup script**:
   - Type `nano ~/start-dev-environment.sh` and press Enter
   - Add this content:
     ```bash
     #!/bin/bash
     echo "Starting VGS Development Environment..."
     
     # Start VNC server if not running
     if ! pgrep -x "vncserver" > /dev/null; then
         echo "Starting VNC server..."
         vncserver -geometry 1920x1080 -depth 24
     fi
     
     # Navigate to project
     cd /home/ubuntu/VGS-KV
     
     echo "Development environment ready!"
     echo "Connect to VNC at: $(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):1"
     echo "VNC Password: cursor123"
     ```

2. **Save and make executable**:
   - Press Ctrl+O, Enter, then Ctrl+X
   - Type `chmod +x ~/start-dev-environment.sh` and press Enter

3. **Test the script**:
   - Type `~/start-dev-environment.sh` and press Enter
   - It should show the connection details

### Scaling and Maintenance (Making It Bigger or Fixing Issues)
- **If It's Slow**: In AWS Console, go to EC2 > Instances > select "Cursor-Development-Server" > Actions > Instance settings > Change instance type > choose "t3.2xlarge" > Apply (this upgrades the server; costs more, so downgrade when done).
- **Stop to Save Money**: Select instance > Actions > Instance state > Stop (turns off, no hourly cost). To start again, select > Start.
- **Update Cursor**: Type `sudo apt update && sudo apt upgrade cursor` (gets the latest version).
- **Backup Your Work**: Your code is safe in GitHub, but you can also create AWS snapshots of your server for extra safety.

### Troubleshooting (Common Problems and Easy Fixes)
Don't worry if something goes wrong—these are the most common issues with simple solutions.

- **Can't Launch Instance in Step 1?** Error like "No default VPC"? Change your region (top right in AWS, pick "US East (N. Virginia)"). Or search "VPC" in AWS and use the wizard to create one (click "Create VPC", choose "VPC only", name it, create).

- **SSH Connection Fails in Step 2?** "Connection timed out"—the server might not be running (check EC2 dashboard, start if stopped). "Permission denied (publickey)"—wrong key file; ensure it's the .pem you downloaded and in Downloads. On Windows, try running Command Prompt as administrator. "Host key verification failed"—type `ssh-keygen -R <ip>` then reconnect.

- **Desktop Installation Fails in Step 3?** "Package not found"—your internet might be blocked; check AWS security group allows outbound traffic (default does). Re-run `sudo apt update`. If "insufficient disk space", the server might need more storage—contact AWS support or create a new instance with larger storage.

- **VNC Connection Fails in Step 4?** "Connection refused"—check port 6080 is open in AWS security group (Step 1, item 10). "Authentication failed"—wrong password; restart VNC server with `vncserver -kill :1` then `vncserver` and set a new password.

- **Cursor Won't Start in Step 5?** "Cursor: command not found"—reinstall with `sudo dpkg -i cursor_latest_amd64.deb`. "Segmentation fault"—try `cursor --no-sandbox`. "Permission denied"—run `sudo chown -R $USER:$USER ~/.config/Cursor`.

- **GitHub Access Fails in Step 6?** "Permission denied (publickey)"—make sure you added the SSH key to GitHub correctly. "Host key verification failed"—type `ssh-keygen -R github.com` then try again. "Repository not found"—check the repository URL is correct.

- **VNC Viewer Won't Connect in Step 8?** "Connection timed out"—check your server's public IP hasn't changed (AWS sometimes assigns new IPs). "Authentication failed"—wrong VNC password; restart VNC server and set a new password.

- **Cursor is Slow or Unresponsive?** The t3.xlarge instance should be fast enough, but if it's slow: 1) Close other applications in the desktop, 2) Restart Cursor, 3) Consider upgrading to t3.2xlarge for more memory.

- **Files Not Saving or Git Not Working?** Check disk space with `df -h`. If it's full, clean up with `sudo apt autoremove` or upgrade to a larger instance.

- **Desktop Session Disconnects?** This can happen if the server is idle. To prevent this: 1) Set screen saver to "Never", 2) Use the startup script to reconnect, 3) Consider using AWS Session Manager for more stable connections.

- **General Tip**: Copy any error message (e.g., "Error: connection timeout") into Google with "AWS [error]" for more help, or use AWS free chat support (bottom of the Console page, click the chat icon).

### Cost Optimization Tips
Since this uses a larger instance, here are ways to save money:

1. **Stop the server when not using**: In AWS Console, select your instance > Actions > Instance state > Stop (no hourly cost when stopped).

2. **Use Spot Instances**: For development, you can use spot instances which cost 60-90% less (but can be terminated with 2 minutes notice).

3. **Schedule automatic shutdown**: Set up a cron job to stop the server at night: `crontab -e` and add `0 22 * * * sudo shutdown -h now` (stops at 10 PM).

4. **Monitor costs**: In AWS Console, go to "Billing" > "Cost Explorer" to track your spending.

Congratulations! You now have a full development environment in the cloud with Cursor IDE. You can create files, edit code, and push to GitHub without any of the Windows PowerShell issues we were experiencing. The environment is powerful enough to handle all your VGS development needs, and you can access it from anywhere with an internet connection. If you need help with any specific development tasks or want to optimize the setup further, just let me know!

