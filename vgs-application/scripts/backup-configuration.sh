
#!/bin/bash

echo "VGS Configuration Backup..."
echo "Timestamp: $(date)"

# Create backup directory with timestamp
BACKUP_DIR="backup/$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"

echo "Creating backup in: $BACKUP_DIR"

# Backup application configurations
echo "Backing up application configurations..."
cp -r embedded-document/src/main/resources/*.yml "$BACKUP_DIR/" 2>/dev/null || true
cp -r transaction-index/src/main/resources/*.yml "$BACKUP_DIR/" 2>/dev/null || true

# Backup Maven configurations
echo "Backing up Maven configurations..."
cp embedded-document/pom.xml "$BACKUP_DIR/embedded-pom.xml" 2>/dev/null || true
cp transaction-index/pom.xml "$BACKUP_DIR/transaction-index-pom.xml" 2>/dev/null || true

# Backup environment configs
echo "Backing up environment configurations..."
cp config/*.yml "$BACKUP_DIR/" 2>/dev/null || true

# Backup logs (last 7 days)
echo "Backing up recent logs..."
mkdir -p "$BACKUP_DIR/logs"
find logs/ -name "*.log" -mtime -7 -exec cp {} "$BACKUP_DIR/logs/" \; 2>/dev/null || true

# Create backup manifest
echo "Creating backup manifest..."
cat > "$BACKUP_DIR/backup_manifest.txt" << EOF
VGS Application Backup
=====================
Date: $(date)
Host: $(hostname)
User: $(whoami)

Files included:
$(find "$BACKUP_DIR" -type f | sort)

Application Status:
$(curl -s http://0.0.0.0:5001/actuator/health 2>/dev/null || echo "Service not running")
EOF

# Compress backup
echo "Compressing backup..."
tar -czf "${BACKUP_DIR}.tar.gz" -C backup "$(basename "$BACKUP_DIR")"

# Clean up old backups (keep last 10)
echo "Cleaning up old backups..."
ls -t backup/*.tar.gz 2>/dev/null | tail -n +11 | xargs rm -f 2>/dev/null || true

echo "Backup complete!"
echo "Backup file: ${BACKUP_DIR}.tar.gz"
echo "Backup size: $(du -h "${BACKUP_DIR}.tar.gz" | cut -f1)"
