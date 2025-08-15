# VGS1: High-Performance Gaming Transaction System

> **Note**: This repository contains the optimized and enhanced version of the VGS gaming transaction system, originally from the VGS-KV project. It includes comprehensive performance improvements, security enhancements, and deployment configurations.

## 🎮 Overview

VGS1 is a high-performance gaming transaction system designed for real-time gaming applications. It implements two architectural patterns for gaming transaction processing with Couchbase Capella, optimized for gaming industry standards (<20ms response times, 100% write success).

## 🏗️ Architecture

### Two Implementation Patterns

1. **Embedded Document Pattern** (`vgs-application/embedded-document/`)
   - Stores all transactions within game round documents
   - Optimized for single-document operations
   - Faster reads, simpler consistency model

2. **Transaction Index Pattern** (`vgs-application/transaction-index/`)
   - Maintains separate transaction index documents
   - Better for complex queries and analytics
   - More complex but provides better query flexibility

### Key Components

- **Spring Boot 3.2.5** with Java 21
- **Couchbase Capella 7.6** for data storage
- **Prometheus/Grafana** for monitoring
- **JMeter & Locust** for load testing
- **Docker Compose** for monitoring stack

## 🚀 Quick Start

### Prerequisites
- AWS Account
- Couchbase Capella 7.6 cluster
- Java 21
- Maven 3.8+

### 1. Deploy Application
```bash
# Follow the detailed guide
deployment-guides/vgs-app-guide.md
```

### 2. Run Benchmark Tests
```bash
# Follow the benchmark guide
deployment-guides/benchmark-guide.md
```

### 3. Monitor Performance
```bash
# Start monitoring stack
cd monitoring
docker-compose up -d
```

## 📊 Performance Targets

- **Response Time**: ≤20ms average
- **Write Success**: 100% under concurrent load
- **Round Retrieval**: ≤50ms
- **Throughput**: 2000+ TPS

## 📁 Project Structure

```
VGS1/
├── vgs-application/           # Main application code
│   ├── embedded-document/     # Embedded document pattern
│   └── transaction-index/     # Transaction index pattern
├── benchmark-testing/         # Load testing tools
├── monitoring/               # Prometheus/Grafana setup
├── deployment-guides/        # Detailed deployment guides
└── VGS/                     # Legacy code (excluded from analysis)
```

## 🔧 Configuration

### Environment Variables
```bash
COUCHBASE_CONNECTION_STRING=couchbase://your-cluster.cloud.couchbase.com
COUCHBASE_USERNAME=your-username
COUCHBASE_PASSWORD=your-password
COUCHBASE_BUCKET_NAME=vgs-gaming
```

### Application Ports
- **Application**: 5100
- **Prometheus**: 9090
- **Grafana**: 3000

## 📈 Monitoring

Access monitoring dashboards:
- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Application Health**: http://localhost:5100/actuator/health

## 🧪 Testing

### Load Testing Commands
```bash
# JMeter test
jmeter -n -t benchmark-testing/vgs-load.jmx -l results.log

# Locust test
locust -f benchmark-testing/locustfile.py --host=http://your-app:5100 --users=500 --run-time=5m --headless
```

## 📚 Documentation

- **Application Deployment**: `deployment-guides/vgs-app-guide.md`
- **Benchmark Testing**: `deployment-guides/benchmark-guide.md`
- **Monitoring Setup**: `deployment-guides/monitoring-guide.md`
- **Codebase Analysis**: `VGS-KV_Codebase_Analysis_and_Recommendations.md`

## 🆕 Recent Enhancements

This version includes comprehensive optimizations and improvements:

- **Performance Fixes**: Optimized Couchbase connection settings and benchmark configurations
- **Security Enhancements**: Added authentication, authorization, and input validation
- **Monitoring Improvements**: Enhanced metrics collection and health checks
- **Deployment Automation**: Docker and Kubernetes configurations
- **Code Quality**: Comprehensive testing and documentation

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License.

## 🆘 Support

For issues and questions:
1. Check the troubleshooting sections in the deployment guides
2. Review the monitoring dashboards for performance issues
3. Check application logs at `target/spring.log`
4. Review the comprehensive analysis document: `VGS-KV_Codebase_Analysis_and_Recommendations.md`
