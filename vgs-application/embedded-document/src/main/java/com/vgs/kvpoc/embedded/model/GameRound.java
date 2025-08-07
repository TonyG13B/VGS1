package com.vgs.kvpoc.embedded.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * ====================================================================
 * VGS KV POC - Embedded Document Pattern: GameRound Model
 * ====================================================================
 * 
 * WHAT THIS CLASS REPRESENTS:
 * This is the main "container" for a single game round (like one hand of poker).
 * It stores ALL information about the round and ALL transactions that happen
 * during that round in one single database document.
 * 
 * WHY WE USE THIS PATTERN:
 * - Think of it like a game scorecard that contains all the plays
 * - Everything related to one round is stored together
 * - Very fast to read all round information (only 1 database call)
 * - Ensures all updates to the round happen together (atomic operations)
 * 
 * HOW IT WORKS:
 * 1. When a game round starts, this object is created with basic info
 * 2. As players place bets, win, or lose, transactions are added to the embedded list
 * 3. The entire round (with all transactions) is saved as one document
 * 4. Reading the round gives you complete information instantly
 * 
 * REAL-WORLD EXAMPLE:
 * - Round ID: "poker-round-12345"
 * - Player ID: "player-abc"
 * - Transactions: [bet $10, win $20, bonus $5] all stored inside this object
 * - Balance tracking: $100 -> $90 -> $110 -> $115
 * 
 * BENEFITS FOR GAMING:
 * - Lightning fast round queries (everything in one place)
 * - Guaranteed data consistency (all changes happen together)
 * - Perfect for real-time gaming where every millisecond matters
 * 
 * TECHNICAL DETAILS:
 * - Stored in Couchbase as a single JSON document
 * - Uses Jackson annotations for JSON conversion
 * - Contains embedded EmbeddedTransaction array
 */
public class GameRound {
    
    // BASIC ROUND IDENTIFICATION
    // These fields uniquely identify and describe this game round
    
    @JsonProperty("id")
    private String id;                    // Unique identifier for this round (like "round-12345")
    
    @JsonProperty("roundNumber")
    private Integer roundNumber;          // Sequential number within a game session (1, 2, 3...)
    
    @JsonProperty("agentPlayerId") 
    private String agentPlayerId;         // Player ID in agent system (industry standard naming)
    
    @JsonProperty("vendorId")
    private String vendorId;              // Vendor/operator ID (gaming industry standard)
    
    @JsonProperty("operatorId")  
    private String operatorId;            // Multi-operator environment support
    
    @JsonProperty("vendorGameId")
    private String vendorGameId;          // Vendor-specific game identifier
    
    @JsonProperty("gameCategory")
    private String gameCategory;          // Gaming category for regulatory compliance
    
    // FINANCIAL TRACKING
    // These fields track money movement during the round
    
    @JsonProperty("initialBalance")
    private Double initialBalance;        // Player's balance when round started (like $100.00)
    
    @JsonProperty("currentBalance")
    private Double currentBalance;        // Player's balance right now (updated with each transaction)
    
    // STATUS AND TIMING
    // These fields track the round's lifecycle and important timestamps
    
    @JsonProperty("roundStatus")
    private String roundStatus;           // Current status: ACTIVE, COMPLETED, CANCELLED
    
    @JsonProperty("startTimestamp")
    private Long startTimestamp;          // When this round started (Unix timestamp)
    
    @JsonProperty("vendorBetTime")  
    private Long vendorBetTime;           // Vendor bet time (Unix timestamp)
    
    @JsonProperty("vendorSetTime")
    private Long vendorSetTime;           // Vendor settlement time (Unix timestamp)
    
    @JsonProperty("endTimestamp")
    private Long endTimestamp;            // When this round ended (Unix timestamp, null if active)
    
    @JsonProperty("lastUpdateTimestamp")
    private Long lastUpdateTimestamp;     // Last modification time (Unix timestamp)
    
    // THE HEART OF THE EMBEDDED PATTERN: TRANSACTION STORAGE
    /**
     * EMBEDDED TRANSACTIONS ARRAY - This is the core of the embedded document pattern!
     * 
     * WHAT THIS IS:
     * This list contains ALL transactions that happen during this round.
     * Instead of storing transactions in separate database documents,
     * we embed them directly inside this GameRound document.
     * 
     * WHY THIS IS POWERFUL:
     * - Reading this round gives you ALL transaction history instantly
     * - All round data (basic info + all transactions) comes in one database call
     * - Changes to round and transactions happen atomically (all-or-nothing)
     * 
     * REAL EXAMPLE:
     * If a player bets $10, wins $20, gets a $5 bonus in one round:
     * transactions = [
     *   {type: "BET", amount: 10.00, balanceAfter: 90.00},
     *   {type: "WIN", amount: 20.00, balanceAfter: 110.00},
     *   {type: "BONUS", amount: 5.00, balanceAfter: 115.00}
     * ]
     * All stored together in this single GameRound document!
     */
    @JsonProperty("transactions")
    private List<EmbeddedTransaction> transactions = new ArrayList<>();
    
    // GAMING-SPECIFIC FIELDS FOR COMPLIANCE
    @JsonProperty("effectiveTurnover")
    private String effectiveTurnover;     // Effective turnover for regulatory compliance
    
    @JsonProperty("jackpotAmount")
    private String jackpotAmount;         // Jackpot amount for progressive games
    
    @JsonProperty("gameSessionToken")
    private String gameSessionToken;      // Session management and security token
    
    // ROUND SUMMARY AND STATISTICS
    @JsonProperty("roundSummary")
    private RoundSummary roundSummary = new RoundSummary();  // Computed totals and statistics
    
    // CONSTRUCTORS
    // These functions create new GameRound objects
    
    /**
     * DEFAULT CONSTRUCTOR
     * 
     * Creates a new empty GameRound with basic default values.
     * This is like creating a blank scorecard for a new game round.
     * 
     * WHAT IT SETS UP:
     * - Current timestamp for start and last update times
     * - Sets status to "ACTIVE" (round is ready to begin)
     * - Initializes empty transaction list
     */
    public GameRound() {
        // Record when this round was created (Unix timestamp)
        long currentTime = System.currentTimeMillis();
        this.startTimestamp = currentTime;
        this.vendorBetTime = currentTime;
        this.lastUpdateTimestamp = currentTime;
        // New rounds start as active
        this.roundStatus = "ACTIVE";
        // Initialize gaming compliance fields
        this.effectiveTurnover = "0.00";
        this.jackpotAmount = "0.00";
        this.gameSessionToken = "SESSION_" + System.currentTimeMillis();
    }
    
    /**
     * FULL CONSTRUCTOR
     * 
     * Creates a new GameRound with specific details provided.
     * This is like filling out a scorecard with player info before starting.
     * 
     * @param id Unique identifier for this round
     * @param roundNumber Sequential number (1st round, 2nd round, etc.)
     * @param playerId Who is playing this round
     * @param agentId Gaming operator/agent identifier
     * @param initialBalance How much money the player starts with
     */
    public GameRound(String id, Integer roundNumber, String agentPlayerId, String vendorId, Double initialBalance) {
        // First call the default constructor to set up basic fields
        this();
        // Then set the specific values provided
        this.id = id;
        this.roundNumber = roundNumber;
        this.agentPlayerId = agentPlayerId;
        this.vendorId = vendorId;
        this.operatorId = vendorId; // Default same as vendor
        this.initialBalance = initialBalance;
        this.currentBalance = initialBalance;  // Start with same balance as initial
        this.vendorGameId = "503";             // Default game ID
        this.gameCategory = "7";               // Default gaming category
    }
    
    // Business Methods for Embedded Pattern
    
    /**
     * Add transaction to embedded array - demonstrates atomic updates
     */
    public void addTransaction(EmbeddedTransaction transaction) {
        transaction.setSequenceNumber(this.transactions.size() + 1);
        transaction.setCreateTime(System.currentTimeMillis());
        this.transactions.add(transaction);
        
        // Update round balance based on transaction
        updateBalanceFromTransaction(transaction);
        updateRoundSummary();
        updateEffectiveTurnover(transaction);
        this.lastUpdateTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Update balance based on transaction type
     */
    private void updateBalanceFromTransaction(EmbeddedTransaction transaction) {
        switch (transaction.getType()) {
            case "BET":
                this.currentBalance -= transaction.getAmount();
                break;
            case "WIN":
                this.currentBalance += transaction.getAmount();
                break;
            case "REFUND":
                this.currentBalance += transaction.getAmount();
                break;
        }
    }
    
    /**
     * Update round summary with aggregated data
     */
    private void updateRoundSummary() {
        this.roundSummary.setTotalTransactions(this.transactions.size());
        this.roundSummary.setTotalBets(this.transactions.stream()
            .filter(t -> "BET".equals(t.getType()))
            .mapToDouble(EmbeddedTransaction::getAmount)
            .sum());
        this.roundSummary.setTotalWins(this.transactions.stream()
            .filter(t -> "WIN".equals(t.getType()))
            .mapToDouble(EmbeddedTransaction::getAmount)
            .sum());
        this.roundSummary.setNetAmount(this.roundSummary.getTotalWins() - this.roundSummary.getTotalBets());
    }
    
    /**
     * Update effective turnover for compliance
     */
    private void updateEffectiveTurnover(EmbeddedTransaction transaction) {
        if ("BET".equals(transaction.getType())) {
            double currentTurnover = Double.parseDouble(this.effectiveTurnover);
            currentTurnover += transaction.getBetAmount();
            this.effectiveTurnover = String.format("%.2f", currentTurnover);
        }
    }
    
    /**
     * Complete the round - final state
     */
    public void completeRound() {
        this.roundStatus = "COMPLETED";
        this.endTimestamp = System.currentTimeMillis();
        this.vendorSetTime = System.currentTimeMillis();
        this.lastUpdateTimestamp = System.currentTimeMillis();
        updateRoundSummary();
    }
    
    /**
     * Get transaction by ID from embedded array
     */
    public EmbeddedTransaction getTransactionById(String transactionId) {
        return this.transactions.stream()
            .filter(t -> transactionId.equals(t.getId()))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get transactions by type from embedded array
     */
    public List<EmbeddedTransaction> getTransactionsByType(String type) {
        return this.transactions.stream()
            .filter(t -> type.equals(t.getType()))
            .toList();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public Integer getRoundNumber() { return roundNumber; }
    public void setRoundNumber(Integer roundNumber) { this.roundNumber = roundNumber; }
    
    public String getAgentPlayerId() { return agentPlayerId; }
    public void setAgentPlayerId(String agentPlayerId) { this.agentPlayerId = agentPlayerId; }
    
    public String getVendorId() { return vendorId; }
    public void setVendorId(String vendorId) { this.vendorId = vendorId; }
    
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
    
    public String getVendorGameId() { return vendorGameId; }
    public void setVendorGameId(String vendorGameId) { this.vendorGameId = vendorGameId; }
    
    public String getGameCategory() { return gameCategory; }
    public void setGameCategory(String gameCategory) { this.gameCategory = gameCategory; }
    
    public Double getInitialBalance() { return initialBalance; }
    public void setInitialBalance(Double initialBalance) { this.initialBalance = initialBalance; }
    
    public Double getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(Double currentBalance) { this.currentBalance = currentBalance; }
    
    public String getRoundStatus() { return roundStatus; }
    public void setRoundStatus(String roundStatus) { this.roundStatus = roundStatus; }
    
    public Long getStartTimestamp() { return startTimestamp; }
    public void setStartTimestamp(Long startTimestamp) { this.startTimestamp = startTimestamp; }
    
    public Long getVendorBetTime() { return vendorBetTime; }
    public void setVendorBetTime(Long vendorBetTime) { this.vendorBetTime = vendorBetTime; }
    
    public Long getVendorSetTime() { return vendorSetTime; }
    public void setVendorSetTime(Long vendorSetTime) { this.vendorSetTime = vendorSetTime; }
    
    public Long getEndTimestamp() { return endTimestamp; }
    public void setEndTimestamp(Long endTimestamp) { this.endTimestamp = endTimestamp; }
    
    public Long getLastUpdateTimestamp() { return lastUpdateTimestamp; }
    public void setLastUpdateTimestamp(Long lastUpdateTimestamp) { this.lastUpdateTimestamp = lastUpdateTimestamp; }
    
    public String getEffectiveTurnover() { return effectiveTurnover; }
    public void setEffectiveTurnover(String effectiveTurnover) { this.effectiveTurnover = effectiveTurnover; }
    
    public String getJackpotAmount() { return jackpotAmount; }
    public void setJackpotAmount(String jackpotAmount) { this.jackpotAmount = jackpotAmount; }
    
    public String getGameSessionToken() { return gameSessionToken; }
    public void setGameSessionToken(String gameSessionToken) { this.gameSessionToken = gameSessionToken; }
    
    public List<EmbeddedTransaction> getTransactions() { return transactions; }
    public void setTransactions(List<EmbeddedTransaction> transactions) { this.transactions = transactions; }
    
    public RoundSummary getRoundSummary() { return roundSummary; }
    public void setRoundSummary(RoundSummary roundSummary) { this.roundSummary = roundSummary; }
    
    /**
     * Embedded Round Summary for aggregated data
     */
    public static class RoundSummary {
        @JsonProperty("totalTransactions")
        private Integer totalTransactions = 0;
        
        @JsonProperty("totalBets")
        private Double totalBets = 0.0;
        
        @JsonProperty("totalWins")
        private Double totalWins = 0.0;
        
        @JsonProperty("netAmount")
        private Double netAmount = 0.0;
        
        // Getters and Setters
        public Integer getTotalTransactions() { return totalTransactions; }
        public void setTotalTransactions(Integer totalTransactions) { this.totalTransactions = totalTransactions; }
        
        public Double getTotalBets() { return totalBets; }
        public void setTotalBets(Double totalBets) { this.totalBets = totalBets; }
        
        public Double getTotalWins() { return totalWins; }
        public void setTotalWins(Double totalWins) { this.totalWins = totalWins; }
        
        public Double getNetAmount() { return netAmount; }
        public void setNetAmount(Double netAmount) { this.netAmount = netAmount; }
    }
}