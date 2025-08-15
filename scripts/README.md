# VGS Scripts

This folder contains utility scripts for the VGS gaming transaction system.

## Available Scripts

### `setup-couchbase-config.sh`
**Purpose**: Automated Couchbase Capella configuration setup
**Usage**: Run on all 4 AWS instances to configure Couchbase connection details
**What it does**:
- Creates Couchbase certificate file at `/etc/couchbase/certs/couchbase-cert.pem`
- Sets up environment variables in `~/VGS1/.env`
- Configures application properties for both embedded and transaction services
- Updates monitoring configuration with instance IPs

**Run with**:
```bash
chmod +x scripts/setup-couchbase-config.sh
./scripts/setup-couchbase-config.sh
```

### `quick-optimizations.sh`
**Purpose**: Apply critical performance optimizations to the VGS system
**Usage**: Run on application instances to optimize Couchbase connections and application settings
**What it does**:
- Optimizes Couchbase connection settings in `application.yml`
- Creates environment-specific configuration files
- Adds Spring Security and OpenAPI dependencies
- Creates enhanced monitoring configurations
- Sets up Docker and Kubernetes deployment files

**Run with**:
```bash
chmod +x scripts/quick-optimizations.sh
./scripts/quick-optimizations.sh
```

## Script Requirements

- **Operating System**: Ubuntu/Debian Linux
- **Permissions**: Some scripts require `sudo` access
- **Dependencies**: 
  - `bash` shell
  - `curl` for HTTP requests
  - `jq` for JSON parsing (optional but recommended)

## Usage Notes

1. **Always review scripts** before running them
2. **Backup configurations** before applying changes
3. **Test in development** before running in production
4. **Check logs** after running scripts for any errors

## Script Locations in Deployment

When following the deployment guide, scripts are referenced from the `scripts/` folder:

```bash
# Example from deployment guide
cd ~/VGS1
./scripts/setup-couchbase-config.sh
```
