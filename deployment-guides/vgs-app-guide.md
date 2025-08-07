# VGS Application Deployment Guide

**Overview**: Deploys the optimized VGS app (pure KV, no N1QL) on a dedicated AWS/Ubuntu EC2 instance.

**Prerequisites**:
- AWS EC2 t3.medium instance (Ubuntu 22.04).
- Java 21, Maven 3.8+ installed (`sudo apt update; sudo apt install openjdk-21-jdk maven`).
- Couchbase Capella 7.6 cluster (KV-only bucket).

**Steps**:
1. Launch EC2: Use AWS Console, select Ubuntu 22.04, t3.medium, open ports 5100/5300 (app), 22 (SSH).
2. SSH in: `ssh ubuntu@<instance-ip>`.
3. Install dependencies: `sudo apt install git unzip`.
4. Clone optimized repo: `git clone https://github.com/TonyG13B/VGS-KV.git` (or copy from your workspace).
5. Navigate: `cd VGS-KV/vgs-application`.
6. Configure: Edit `application.yml` with Capella details (connection string, credentialsno query params).
7. Build: `mvn clean package`.
8. Run: `./compile_and_start.sh` (starts embedded and index services).
9. Verify: `curl http://localhost:5100/actuator/health` (should return \"UP\").
10. Monitoring: Expose `/actuator/prometheus` for scraping.
11. Success Criteria Check: Run internal tests to ensure 20ms requests (use included scripts).

**Scaling/Troubleshooting**: Use auto-scaling groups for concurrency. Logs in `/logs/`. If CAS conflicts, adjust retries.
