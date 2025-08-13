
#!/bin/bash

echo "Setting up VGS Benchmark Testing Environment..."
echo "Timestamp: $(date)"

# Update system packages
echo "Updating system packages..."
sudo apt update && sudo apt upgrade -y

# Install Python 3 and pip
echo "Installing Python 3 and pip..."
sudo apt install -y python3 python3-pip python3-venv

# Install required Python packages
echo "Installing Python requirements..."
pip3 install -r requirements.txt

# Install additional monitoring tools
echo "Installing system monitoring tools..."
sudo apt install -y htop iotop nethogs

# Create logs directory
echo "Creating logs directory..."
mkdir -p logs

# Set up environment variables
echo "Setting up environment variables..."
cat >> ~/.bashrc << EOF

# VGS Benchmark Testing Environment
export VGS_TEST_HOST=http://0.0.0.0:5000
export VGS_USERS=100
export VGS_SPAWN_RATE=10
export VGS_RUN_TIME=2m
EOF

# Make scripts executable
echo "Making scripts executable..."
chmod +x scripts/*.sh

echo "Environment setup complete!"
echo "Please restart your terminal or run 'source ~/.bashrc' to load environment variables"
