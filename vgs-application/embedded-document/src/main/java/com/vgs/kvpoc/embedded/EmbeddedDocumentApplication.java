package com.vgs.kvpoc.embedded;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * =========================================================================
 * VGS GAMING PLATFORM - EMBEDDED DOCUMENT PATTERN MAIN APPLICATION
 * =========================================================================
 * 
 * WHAT THIS APPLICATION DOES:
 * This is the main entry point for the Embedded Document Pattern service.
 * Think of this as the "master control program" that starts up the entire
 * gaming transaction system using the embedded document approach.
 * 
 * WHY WE NEED THIS:
 * In the gaming industry, we need to handle thousands of transactions per second
 * from players betting, winning, and losing money. This application provides
 * one of two different approaches (patterns) for storing this data efficiently.
 * 
 * THE EMBEDDED DOCUMENT APPROACH EXPLAINED:
 * Instead of splitting gaming data across multiple database tables, this pattern
 * stores all transactions for a gaming round inside a single document. It's like
 * keeping all related paperwork in one folder instead of filing it separately.
 * 
 * REAL-WORLD EXAMPLE:
 * When a player plays a slot machine game:
 * 1. A "game round" document gets created (like opening a new file folder)
 * 2. Each spin, bet, win, or bonus gets stored as transactions INSIDE that document
 * 3. Everything related to that gaming session stays together
 * 4. This makes reading all the data very fast (one database lookup)
 * 
 * BUSINESS BENEFITS:
 * - Lightning-fast data retrieval (everything in one place)
 * - Perfect for real-time gaming where speed is critical
 * - Simpler data structure (easier to understand and maintain)
 * - Atomic operations (all changes happen together or not at all)
 * - Lower infrastructure costs (fewer database calls)
 * 
 * WHEN TO USE THIS PATTERN:
 * - High-frequency trading or gaming applications
 * - Real-time betting systems where speed matters most
 * - Mobile gaming apps with limited network bandwidth
 * - Live casino systems with immediate response requirements
 * 
 * TECHNICAL IMPLEMENTATION:
 * - Runs on port 5100 (you can access it at http://localhost:5100)
 * - Connects to Couchbase Capella cloud database
 * - Uses Spring Boot framework for easy deployment and management
 * - Provides REST APIs that other applications can call
 * - Supports thousands of concurrent users
 */
@SpringBootApplication
public class EmbeddedDocumentApplication {
    
    /**
     * MAIN PROGRAM STARTUP METHOD
     * 
     * This is the "power button" for the entire Embedded Document Pattern application.
     * Think of this as starting up a specialized computer program that handles
     * gaming transactions for thousands of players simultaneously.
     * 
     * WHAT HAPPENS WHEN THIS RUNS:
     * 
     * STEP 1 - INITIALIZATION:
     * - Prints startup information so you know what's happening
     * - Shows which pattern is starting (Embedded Document)
     * - Displays the port number where the service will be available
     * 
     * STEP 2 - SYSTEM STARTUP:
     * - Spring Boot framework takes over and configures everything automatically
     * - Database connections are established to Couchbase Capella cloud
     * - Security certificates are loaded for secure communication
     * - Memory pools are allocated for high-performance processing
     * 
     * STEP 3 - SERVICE ACTIVATION:
     * - Web endpoints become available for other applications to call
     * - Health monitoring systems are activated
     * - Load balancing and error handling are enabled
     * - The system is ready to process gaming transactions
     * 
     * STEP 4 - CONFIRMATION:
     * - Success messages are displayed to confirm everything is working
     * - URLs are provided so you can check the system status
     * - The application enters a "listening" state waiting for requests
     * 
     * FOR BUSINESS USERS:
     * Once this application starts successfully, it can handle:
     * - Thousands of gaming transactions per second
     * - Real-time player betting and winning
     * - Instant transaction processing
     * - Comprehensive audit trails for regulatory compliance
     * 
     * FOR TECHNICAL USERS:
     * - Access the API at http://localhost:5100
     * - Check health status at http://localhost:5100/actuator/health
     * - Run performance tests using the automation tools
     * - Monitor database operations in Couchbase Capella dashboard
     * 
     * @param args Command line arguments (not used in this gaming application)
     */
    public static void main(String[] args) {
        
        // STEP 1: DISPLAY STARTUP INFORMATION
        // These messages help administrators understand what's starting up
        // and provide key information about the service configuration
        System.out.println("🎮 Starting VGS KV POC - Embedded Document Pattern");
        System.out.println("📊 Pattern: GameRound with embedded GameTransaction array");
        System.out.println("🚀 Port: 5100");
        System.out.println("📈 Optimized for: High-performance gaming with atomic updates");
        
        // STEP 2: START THE SPRING BOOT APPLICATION ENGINE
        // This single line does an enormous amount of work behind the scenes:
        // - Scans for all Java classes in this application
        // - Configures the database connections to Couchbase
        // - Sets up the web server to handle incoming requests
        // - Initializes security, logging, and monitoring systems
        // - Makes the application ready to process gaming transactions
        // Think of this as the "ignition switch" that starts the entire system
        SpringApplication.run(EmbeddedDocumentApplication.class, args);
        
        // STEP 3: CONFIRM SUCCESSFUL STARTUP
        // These messages only appear if everything started correctly
        // If you see these messages, the system is ready to handle gaming transactions
        System.out.println("✅ Embedded Document Pattern server started successfully!");
        System.out.println("🌐 API available at: http://localhost:5100");
        System.out.println("📖 Health check: http://localhost:5100/actuator/health");
    }
}