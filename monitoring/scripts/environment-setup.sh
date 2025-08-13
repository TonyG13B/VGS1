
#!/bin/bash

echo "Setting up VGS Monitoring Environment..."
echo "Timestamp: $(date)"

# Update system packages
echo "Updating system packages..."
sudo apt update && sudo apt upgrade -y

# Install Docker and Docker Compose
echo "Installing Docker..."
sudo apt install -y docker.io docker-compose
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker $USER

# Install Python for metrics server
echo "Installing Python..."
sudo apt install -y python3 python3-pip

# Install monitoring dependencies
echo "Installing monitoring dependencies..."
pip3 install prometheus_client flask requests

# Create necessary directories
echo "Creating monitoring directories..."
mkdir -p logs data/prometheus data/grafana

# Set up permissions
echo "Setting up permissions..."
sudo chown -R $USER:$USER data/
chmod 755 logs data

# Make scripts executable
echo "Making scripts executable..."
chmod +x scripts/*.sh

echo "Monitoring environment setup complete!"
echo "Note: You may need to log out and back in for Docker group membership to take effect"
