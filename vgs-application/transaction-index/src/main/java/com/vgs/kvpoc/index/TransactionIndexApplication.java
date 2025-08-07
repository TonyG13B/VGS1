package com.vgs.kvpoc.index;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ====================================================================
 * VGS Key-Value POC - Transaction Index Pattern Application
 * ====================================================================
 * 
 * WHAT THIS APPLICATION DOES:
 * This is the main entry point for the "Transaction Index" architecture pattern.
 * Think of it like a gaming system that keeps a master list of all transactions
 * separate from the game rounds, with cross-references between them.
 * 
 * WHY THIS PATTERN EXISTS:
 * - Imagine a casino that needs to quickly find any transaction across all games
 * - Instead of storing all transaction details in each game round, we keep them separate
 * - Game rounds have "reference numbers" pointing to the detailed transactions
 * - This makes searching and auditing much easier and faster
 * 
 * HOW IT WORKS:
 * 1. GameRound documents contain lightweight references to transactions
 * 2. TransactionIndex documents store complete transaction details separately
 * 3. When you need transaction details, you look them up by reference
 * 4. This creates two "filing cabinets" - one for rounds, one for transaction details
 * 
 * BENEFITS FOR GAMING:
 * - Lightning-fast searches across all transactions (even from different rounds)
 * - Perfect for regulatory compliance and audit requirements
 * - Can handle millions of transactions without slowing down game rounds
 * - Easy to generate reports across all player activity
 * 
 * TRADE-OFFS:
 * - Requires more database calls to get complete information
 * - Slightly more complex to manage the relationships between documents
 * 
 * TECHNICAL DETAILS:
 * - Runs on port 5300 (you can access it at http://localhost:5300)
 * - Uses Spring Boot framework to handle web requests
 * - Connects to Couchbase database for data storage
 */
@SpringBootApplication
public class TransactionIndexApplication {
    
    /**
     * MAIN PROGRAM ENTRY POINT
     * 
     * This is the "start button" for the entire Transaction Index Pattern application.
     * When you run this program, it will:
     * 
     * 1. PRINT INFORMATION: Shows what pattern is starting and where to find it
     * 2. START THE SERVER: Launches the web server that handles gaming requests
     * 3. CONFIRM SUCCESS: Tells you the server started correctly
     * 
     * WHAT HAPPENS BEHIND THE SCENES:
     * - Spring Boot framework takes over and sets up the entire application
     * - Database connections are established to Couchbase
     * - Web endpoints are made available for other systems to call
     * - Health monitoring is activated so you can check if the system is working
     * 
     * HOW TO USE THIS:
     * - Run this program and wait for "started successfully" message
     * - Open your web browser to http://localhost:5300 to interact with the API
     * - Use the dashboard tools to run performance tests and compare with the embedded pattern
     * 
     * @param args Command line arguments (not used in this application)
     */
    public static void main(String[] args) {
        // Print startup information so you know what's happening
        System.out.println("🎮 Starting VGS KV POC - Transaction Index Pattern");
        System.out.println("📊 Pattern: GameRound with separate TransactionIndex for fast lookups");
        System.out.println("🚀 Port: 5300");
        System.out.println("📈 Optimized for: High-volume systems, audit trails, and compliance");
        
        // This line actually starts the entire Spring Boot application
        // Think of it as pressing the "power on" button for the server
        SpringApplication.run(TransactionIndexApplication.class, args);
        
        // These messages confirm everything started correctly
        System.out.println("✅ Transaction Index Pattern server started successfully!");
        System.out.println("🌐 API available at: http://localhost:5300");
        System.out.println("📖 Health check: http://localhost:5300/actuator/health");
    }
}