
#!/bin/bash

echo "Creating VGS Configuration Backup..."

BACKUP_DIR="backups/config-backup-$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"

# Backup application configurations
echo "Backing up application configurations..."
cp -r vgs-application/embedded-document/src/main/resources/ "$BACKUP_DIR/embedded-resources/"
cp -r vgs-application/transaction-index/src/main/resources/ "$BACKUP_DIR/index-resources/"

# Backup monitoring configurations
echo "Backing up monitoring configurations..."
cp -r monitoring/ "$BACKUP_DIR/monitoring/"

# Backup benchmark configurations
echo "Backing up benchmark configurations..."
cp -r benchmark-testing/ "$BACKUP_DIR/benchmark-testing/"

# Create backup manifest
echo "Creating backup manifest..."
cat > "$BACKUP_DIR/backup-manifest.txt" << EOF
VGS Configuration Backup
Created: $(date)
Environment: Replit
Version: $(git rev-parse --short HEAD 2>/dev/null || echo "unknown")

Contents:
- Application configurations (embedded-document, transaction-index)
- Monitoring configurations (Prometheus, Grafana)
- Benchmark testing configurations (Locust)
- Deployment scripts and guides

Restore instructions:
1. Extract backup to project root
2. Copy configurations to appropriate locations
3. Restart services
EOF

echo "Backup created: $BACKUP_DIR"
echo "Total size: $(du -sh "$BACKUP_DIR" | cut -f1)"
