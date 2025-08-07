package com.vgs.kvpoc.embedded.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ====================================================================
 * VGS KV POC - Embedded Document Pattern: EmbeddedTransaction Model
 * ====================================================================
 * 
 * WHAT THIS CLASS REPRESENTS:
 * This represents a single financial transaction within a game round (like one bet, win, or bonus).
 * Unlike other systems that store transactions separately, this transaction is "embedded"
 * (stored inside) the GameRound document.
 * 
 * WHY WE EMBED TRANSACTIONS:
 * - Faster reads: All round transactions come together in one database call
 * - Atomic updates: Round and all its transactions update together (all-or-nothing)
 * - Better performance: No need to join data from multiple documents
 * 
 * REAL-WORLD EXAMPLES:
 * - BET transaction: Player bets $10, balance goes from $100 to $90
 * - WIN transaction: Player wins $25, balance goes from $90 to $115
 * - BONUS transaction: House gives $5 bonus, balance goes from $115 to $120
 * - REFUND transaction: Bet refunded due to system error
 * 
 * HOW IT WORKS:
 * 1. When a player makes a move, a new EmbeddedTransaction is created
 * 2. It gets added to the transactions array inside the GameRound
 * 3. The entire GameRound (with the new transaction) is saved to database
 * 4. Reading the round gives you all transactions instantly
 * 
 * BENEFITS FOR GAMING:
 * - Complete transaction history available instantly
 * - Guarantees consistency between round status and transaction history
 * - Perfect for audit trails and compliance reporting
 * 
 * TECHNICAL DETAILS:
 * - This is a Java POJO (Plain Old Java Object) with JSON annotations
 * - Gets converted to JSON and stored inside GameRound documents
 * - Contains all necessary fields for financial and compliance tracking
 */
public class EmbeddedTransaction {
    
    // BASIC TRANSACTION IDENTIFICATION
    // These fields uniquely identify this specific transaction
    
    @JsonProperty("betId")
    private String betId;                 // Industry standard bet ID (like "BET20250701001")
    
    @JsonProperty("id")
    private String id;                    // Unique ID for this transaction (like "txn-abc123")
    
    @JsonProperty("sequenceNumber")
    private Integer sequenceNumber;       // Order within the round (1st transaction, 2nd, etc.)
    
    // TRANSACTION TYPE AND FINANCIAL DETAILS
    // These fields define what kind of money movement this represents
    
    @JsonProperty("type")
    private String type;                  // Type: BET, WIN, REFUND, BONUS, etc.
    
    @JsonProperty("betAmount")
    private Double betAmount;             // Bet amount in string format (industry standard)
    
    @JsonProperty("amount")
    private Double amount;                // How much money (like 10.50 for $10.50) - legacy support
    
    @JsonProperty("currency")
    private String currency;              // Currency code (USD, EUR, GBP, etc.)
    
    @JsonProperty("description")
    private String description;           // Human-readable description ("Blackjack bet")
    
    // TIMING AND STATUS TRACKING
    // These fields track when the transaction happened and its current state
    
    @JsonProperty("createTime")
    private Long createTime;              // Creation time (Unix timestamp - industry standard)
    
    @JsonProperty("timestamp")
    private String timestamp;             // Exact time transaction occurred (ISO format) - legacy
    
    @JsonProperty("status")
    private String status;                // Current status: PENDING, COMPLETED, FAILED
    
    // GAMING AND BUSINESS CONTEXT
    // These fields provide additional context and business information
    
    @JsonProperty("agentPlayerId")
    private String agentPlayerId;         // Player ID in agent system (industry standard)
    
    @JsonProperty("resultType")
    private String resultType;            // Result type (numeric - industry standard)
    
    @JsonProperty("effectiveTurnover")
    private String effectiveTurnover;     // Effective turnover for regulatory compliance
    
    @JsonProperty("gameCategory")
    private String gameCategory;          // Gaming category for regulatory requirements
    
    @JsonProperty("jackpotAmount")
    private String jackpotAmount;         // Jackpot amount for progressive games
    
    @JsonProperty("operatorId")
    private String operatorId;            // Multi-operator environment support
    
    @JsonProperty("gameSessionToken")
    private String gameSessionToken;      // Session management and security token
    
    @JsonProperty("internalTransactionId")
    private String internalTransactionId; // Internal transaction tracking
    
    @JsonProperty("settlementTime")
    private Long settlementTime;          // Settlement timing for compliance
    
    @JsonProperty("winAmount")
    private String winAmount;             // Separate win amount tracking
    
    @JsonProperty("winLoss")
    private String winLoss;               // Net position calculation
    
    @JsonProperty("gameDetails")
    private GameDetails gameDetails;      // Game-specific info (hand cards, spin results, etc.)
    
    @JsonProperty("balanceAfter")
    private Double balanceAfter;          // Player's balance AFTER this transaction
    
    @JsonProperty("metadata")
    private TransactionMetadata metadata; // Additional tracking info (IP, device, etc.)
    
    // ACCOUNTING FLAGS
    // These boolean flags help with financial reporting and compliance
    
    @JsonProperty("debit")
    private Boolean debit;                // True if this takes money from player (like BET)
    
    @JsonProperty("credit")
    private Boolean credit;               // True if this gives money to player (like WIN)
    
    // CONSTRUCTORS
    // These functions create new EmbeddedTransaction objects
    
    /**
     * DEFAULT CONSTRUCTOR
     * 
     * Creates a new empty transaction with basic default values.
     * This is like creating a blank transaction slip before filling it out.
     * 
     * WHAT IT SETS UP:
     * - Current timestamp (when the transaction is being created)
     * - Status as "PENDING" (not yet completed)
     * - Currency as "USD" (default to US Dollars)
     */
    public EmbeddedTransaction() {
        // Record exactly when this transaction was created
        this.createTime = System.currentTimeMillis();
        this.timestamp = java.time.LocalDateTime.now().toString();
        // All new transactions start as pending until processed
        this.status = "PENDING";
        // Default to USD unless specified otherwise
        this.currency = "USD";
        // Initialize gaming compliance fields
        this.effectiveTurnover = "0.00";
        this.jackpotAmount = "0.00";
        this.winAmount = "0.00";
        this.winLoss = "0.00";
        this.resultType = "1";
    }
    
    /**
     * FULL CONSTRUCTOR
     * 
     * Creates a new transaction with the essential information provided.
     * This is like filling out a transaction slip with the key details.
     * 
     * @param id Unique identifier for this transaction
     * @param type What kind of transaction (BET, WIN, BONUS, etc.)
     * @param amount How much money is involved
     * @param description Human-readable description of what happened
     */
    public EmbeddedTransaction(String id, String type, Double amount, String description) {
        // First call the default constructor to set up basic fields
        this();
        // Then set the specific values provided
        this.id = id;
        this.betId = "BET" + System.currentTimeMillis() + "_" + id;
        this.type = type;
        this.amount = amount;
        this.betAmount = amount;
        this.description = description;
        this.effectiveTurnover = String.format("%.2f", amount);
        // Initialize nested objects with default values
        this.gameDetails = new GameDetails();
        this.metadata = new TransactionMetadata();
        // Figure out if this is a debit or credit based on transaction type
        updateDebitCreditFlags();
    }
    
    // BUSINESS METHODS
    // These functions perform important business operations on transactions
    
    /**
     * COMPLETE THE TRANSACTION
     * 
     * This function marks a transaction as successfully completed and updates
     * the player's balance. It's like stamping "APPROVED" on a bank transaction.
     * 
     * WHAT IT DOES:
     * 1. Changes status from "PENDING" to "COMPLETED"
     * 2. Records the player's new balance after this transaction
     * 3. Updates the last modified timestamp
     * 
     * WHY THIS IS IMPORTANT:
     * - Provides a clear audit trail of when transactions were finalized
     * - Tracks balance changes for financial reporting
     * - Helps with reconciliation and compliance
     * 
     * @param newBalance The player's balance after this transaction is applied
     */
    public void complete(Double newBalance) {
        this.status = "COMPLETED";
        this.balanceAfter = newBalance;
        this.settlementTime = System.currentTimeMillis();
        this.timestamp = java.time.LocalDateTime.now().toString();
    }
    
    /**
     * Fail the transaction with reason
     */
    public void fail(String reason) {
        this.status = "FAILED";
        if (this.metadata != null) {
            this.metadata.setFailureReason(reason);
        }
    }
    
    /**
     * Check if transaction is a debit (reduces balance)
     */
    public boolean isDebit() {
        return "BET".equals(this.type) || "FEE".equals(this.type);
    }
    
    /**
     * Check if transaction is a credit (increases balance)
     */
    public boolean isCredit() {
        return "WIN".equals(this.type) || "REFUND".equals(this.type) || "BONUS".equals(this.type);
    }
    
    /**
     * Update debit/credit flags based on transaction type
     */
    private void updateDebitCreditFlags() {
        this.debit = isDebit();
        this.credit = isCredit();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public Integer getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(Integer sequenceNumber) { this.sequenceNumber = sequenceNumber; }
    
    public String getType() { return type; }
    public void setType(String type) { 
        this.type = type; 
        updateDebitCreditFlags();
    }
    
    public String getBetId() { return betId; }
    public void setBetId(String betId) { this.betId = betId; }
    
    public Double getBetAmount() { return betAmount; }
    public void setBetAmount(Double betAmount) { this.betAmount = betAmount; this.amount = betAmount; }
    
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; this.betAmount = amount; }
    
    public Long getCreateTime() { return createTime; }
    public void setCreateTime(Long createTime) { this.createTime = createTime; }
    
    public String getAgentPlayerId() { return agentPlayerId; }
    public void setAgentPlayerId(String agentPlayerId) { this.agentPlayerId = agentPlayerId; }
    
    public String getResultType() { return resultType; }
    public void setResultType(String resultType) { this.resultType = resultType; }
    
    public String getEffectiveTurnover() { return effectiveTurnover; }
    public void setEffectiveTurnover(String effectiveTurnover) { this.effectiveTurnover = effectiveTurnover; }
    
    public String getGameCategory() { return gameCategory; }
    public void setGameCategory(String gameCategory) { this.gameCategory = gameCategory; }
    
    public String getJackpotAmount() { return jackpotAmount; }
    public void setJackpotAmount(String jackpotAmount) { this.jackpotAmount = jackpotAmount; }
    
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
    
    public String getGameSessionToken() { return gameSessionToken; }
    public void setGameSessionToken(String gameSessionToken) { this.gameSessionToken = gameSessionToken; }
    
    public String getInternalTransactionId() { return internalTransactionId; }
    public void setInternalTransactionId(String internalTransactionId) { this.internalTransactionId = internalTransactionId; }
    
    public Long getSettlementTime() { return settlementTime; }
    public void setSettlementTime(Long settlementTime) { this.settlementTime = settlementTime; }
    
    public String getWinAmount() { return winAmount; }
    public void setWinAmount(String winAmount) { this.winAmount = winAmount; }
    
    public String getWinLoss() { return winLoss; }
    public void setWinLoss(String winLoss) { this.winLoss = winLoss; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public GameDetails getGameDetails() { return gameDetails; }
    public void setGameDetails(GameDetails gameDetails) { this.gameDetails = gameDetails; }
    
    public Double getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Double balanceAfter) { this.balanceAfter = balanceAfter; }
    
    public TransactionMetadata getMetadata() { return metadata; }
    public void setMetadata(TransactionMetadata metadata) { this.metadata = metadata; }
    
    public Boolean getDebit() { return debit; }
    public void setDebit(Boolean debit) { this.debit = debit; }
    
    public Boolean getCredit() { return credit; }
    public void setCredit(Boolean credit) { this.credit = credit; }
    
    /**
     * Embedded game details specific to the transaction
     */
    public static class GameDetails {
        @JsonProperty("gameId")
        private String gameId;
        
        @JsonProperty("gameType")
        private String gameType;
        
        @JsonProperty("betType")
        private String betType;
        
        @JsonProperty("odds")
        private Double odds;
        
        @JsonProperty("multiplier")
        private Double multiplier;
        
        public GameDetails() {
            this.gameType = "STANDARD";
            this.odds = 1.0;
            this.multiplier = 1.0;
        }
        
        // Getters and Setters
        public String getGameId() { return gameId; }
        public void setGameId(String gameId) { this.gameId = gameId; }
        
        public String getGameType() { return gameType; }
        public void setGameType(String gameType) { this.gameType = gameType; }
        
        public String getBetType() { return betType; }
        public void setBetType(String betType) { this.betType = betType; }
        
        public Double getOdds() { return odds; }
        public void setOdds(Double odds) { this.odds = odds; }
        
        public Double getMultiplier() { return multiplier; }
        public void setMultiplier(Double multiplier) { this.multiplier = multiplier; }
    }
    
    /**
     * Embedded transaction metadata
     */
    public static class TransactionMetadata {
        @JsonProperty("sourceSystem")
        private String sourceSystem;
        
        @JsonProperty("sessionId")
        private String sessionId;
        
        @JsonProperty("deviceId")
        private String deviceId;
        
        @JsonProperty("ipAddress")
        private String ipAddress;
        
        @JsonProperty("userAgent")
        private String userAgent;
        
        @JsonProperty("failureReason")
        private String failureReason;
        
        @JsonProperty("processingTime")
        private Long processingTime;
        
        public TransactionMetadata() {
            this.sourceSystem = "GAMING_PLATFORM";
        }
        
        // Getters and Setters
        public String getSourceSystem() { return sourceSystem; }
        public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        
        public String getFailureReason() { return failureReason; }
        public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
        
        public Long getProcessingTime() { return processingTime; }
        public void setProcessingTime(Long processingTime) { this.processingTime = processingTime; }
    }
}