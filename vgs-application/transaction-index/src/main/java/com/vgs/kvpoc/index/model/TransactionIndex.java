package com.vgs.kvpoc.index.model;

import com.fasterxml.jackson.annotation.JsonProperty;


import java.util.ArrayList;
import java.util.List;

/**
 * ====================================================================
 * VGS KV POC - Transaction Index Pattern: TransactionIndex Model
 * ====================================================================
 * 
 * WHAT THIS CLASS REPRESENTS:
 * This is a separate document that stores complete details about a single transaction.
 * It's the "detailed filing cabinet" that works with the GameRound "catalog card system".
 * Each TransactionIndex document contains everything you'd want to know about one transaction.
 * 
 * WHY WE NEED SEPARATE TRANSACTION DOCUMENTS:
 * - Enables lightning-fast searches across ALL transactions (even from different rounds)
 * - Perfect for audit trails and compliance reporting
 * - Supports complex queries like "find all transactions over $100 for player X"
 * - Each transaction gets its own document ID for direct access
 * 
 * HOW IT WORKS WITH GAMEROUNDS:
 * 1. GameRound stores lightweight references to these TransactionIndex documents
 * 2. When you need full transaction details, you look up this document by ID
 * 3. This creates a "many-to-one" relationship: many transactions belong to one round
 * 4. But each transaction is independently searchable and auditable
 * 
 * REAL-WORLD EXAMPLE:
 * - Transaction ID: "txn-idx-12345"
 * - Round ID: "poker-round-67890" (which round this transaction belongs to)
 * - Player ID: "player-abc123" (who made this transaction)
 * - Full details: amount, type, timestamp, compliance checks, fraud detection
 * 
 * BENEFITS FOR GAMING:
 * - Can search "all BET transactions for player X in the last month"
 * - Perfect for regulatory reporting and compliance audits
 * - Fraud detection across all player activity
 * - Fast lookups by transaction ID, player ID, or amount ranges
 * 
 * COMPLIANCE FEATURES:
 * - Complete audit trails for each transaction
 * - Automated compliance checking and flagging
 * - Fraud detection and risk assessment
 * - Performance metrics for system optimization
 * 
 * TECHNICAL DETAILS:
 * - Each transaction gets its own Couchbase document
 * - Designed for fast key-value lookups and range queries
 * - Contains rich metadata for compliance and business intelligence
 */
public class TransactionIndex {
    
    // CORE TRANSACTION IDENTIFICATION
    // These fields uniquely identify this transaction and link it to other entities
    
    @JsonProperty("id")
    private String id;                        // Unique document ID in Couchbase (like "txn-idx-12345")
    
    @JsonProperty("transactionId")
    private String transactionId;             // Business transaction ID (may be same as id)
    
    @JsonProperty("roundId")
    private String roundId;                   // Which GameRound this transaction belongs to
    
    @JsonProperty("agentPlayerId")
    private String agentPlayerId;             // Player ID in agent system (industry standard)
    
    @JsonProperty("vendorId")
    private String vendorId;                  // Vendor/operator ID (gaming industry standard)
    
    @JsonProperty("operatorId")
    private String operatorId;                // Multi-operator environment support
    
    // FINANCIAL TRANSACTION DETAILS
    // These fields contain the core financial information
    
    @JsonProperty("type")
    private String type;                      // Transaction type (BET, WIN, BONUS, REFUND, etc.)
    
    @JsonProperty("betAmount")
    private String betAmount;                 // Bet amount (industry standard string format)
    
    @JsonProperty("amount")
    private Double amount;                    // How much money was involved (legacy support)
    
    @JsonProperty("currency")
    private String currency;                  // Currency code (USD, EUR, GBP, etc.)
    
    // TIMING AND STATUS INFORMATION
    // These fields track the transaction lifecycle and performance
    
    @JsonProperty("createTime")
    private Long createTime;                  // Creation time (Unix timestamp - industry standard)
    
    @JsonProperty("timestamp")
    private String timestamp;                 // When the transaction occurred (legacy support)
    
    @JsonProperty("status")
    private String status;                    // Current status (PENDING, COMPLETED, FAILED)
    
    @JsonProperty("processingTime")
    private Long processingTime;              // How long it took to process (milliseconds)
    
    // BALANCE TRACKING
    // These fields track the player's account balance changes
    
    @JsonProperty("balanceBefore")
    private Double balanceBefore;             // Player's balance before this transaction
    
    @JsonProperty("balanceAfter")
    private Double balanceAfter;              // Player's balance after this transaction
    
    // GAMING-SPECIFIC COMPLIANCE FIELDS
    @JsonProperty("effectiveTurnover")
    private String effectiveTurnover;         // Effective turnover for regulatory compliance
    
    @JsonProperty("gameCategory")
    private String gameCategory;              // Gaming category for regulatory requirements
    
    @JsonProperty("jackpotAmount")
    private String jackpotAmount;             // Jackpot amount for progressive games
    
    @JsonProperty("gameSessionToken")
    private String gameSessionToken;          // Session management and security token
    
    @JsonProperty("internalTransactionId")
    private String internalTransactionId;     // Internal transaction tracking
    
    @JsonProperty("settlementTime")
    private Long settlementTime;              // Settlement timing for compliance
    
    @JsonProperty("winAmount")
    private String winAmount;                 // Separate win amount tracking
    
    @JsonProperty("winLoss")
    private String winLoss;                   // Net position calculation
    
    @JsonProperty("resultType")
    private String resultType;                // Result type (numeric - industry standard)
    
    // ADVANCED INDEX FEATURES FOR ENTERPRISE GAMING
    /**
     * INDEX-SPECIFIC FIELDS FOR FAST LOOKUPS AND COMPLIANCE
     * 
     * These fields are what make the Transaction Index pattern powerful for enterprise gaming.
     * They provide fast search capabilities, audit trails, and compliance features
     * that are essential for regulated gaming environments.
     */
    
    @JsonProperty("indexMetadata")
    private IndexMetadata indexMetadata;         // Search optimization and indexing hints
    
    @JsonProperty("auditTrail")
    private AuditTrail auditTrail;               // Complete audit trail for compliance
    
    @JsonProperty("complianceChecks")
    private ComplianceChecks complianceChecks;   // Automated regulatory compliance checks
    
    @JsonProperty("fraudDetection")
    private FraudDetection fraudDetection;       // Real-time fraud detection and risk scoring
    
    @JsonProperty("performanceMetrics")
    private PerformanceMetrics performanceMetrics; // System performance and optimization data
    
    // Constructors
    public TransactionIndex() {
        this.createTime = System.currentTimeMillis();
        this.timestamp = java.time.LocalDateTime.now().toString();
        this.currency = "USD";
        this.status = "INDEXED";
        this.effectiveTurnover = "0.00";
        this.jackpotAmount = "0.00";
        this.winAmount = "0.00";
        this.winLoss = "0.00";
        this.resultType = "1";
        this.indexMetadata = new IndexMetadata();
        this.auditTrail = new AuditTrail();
        this.complianceChecks = new ComplianceChecks();
        this.fraudDetection = new FraudDetection();
        this.performanceMetrics = new PerformanceMetrics();
    }
    
    public TransactionIndex(String transactionId, String roundId, String agentPlayerId, String type, Double amount) {
        this();
        this.transactionId = transactionId;
        this.roundId = roundId;
        this.agentPlayerId = agentPlayerId;
        this.type = type;
        this.amount = amount;
        this.betAmount = String.format("%.2f", amount);
        this.effectiveTurnover = String.format("%.2f", amount);
        this.id = generateIndexId();
    }
    
    // Business Methods for Transaction Index Pattern
    
    /**
     * Generate unique index ID
     */
    private String generateIndexId() {
        return "TXN_IDX_" + System.currentTimeMillis() + "_" + this.transactionId;
    }
    
    /**
     * Add audit trail entry
     */
    public void addAuditEntry(String event, String details) {
        AuditTrail.AuditEntry entry = new AuditTrail.AuditEntry();
        entry.setEvent(event);
        entry.setDetails(details);
        entry.setTimestamp(java.time.LocalDateTime.now().toString());
        this.auditTrail.getEntries().add(entry);
    }
    
    /**
     * Perform compliance checks
     */
    public void performComplianceChecks() {
        this.complianceChecks.setLastCheckTime(java.time.LocalDateTime.now().toString());
        
        // AML (Anti-Money Laundering) check
        if (this.amount > 10000) {
            this.complianceChecks.getFlags().add("HIGH_VALUE_TRANSACTION");
        }
        
        // Velocity check
        if (this.performanceMetrics.getProcessingTime() < 100) {
            this.complianceChecks.getFlags().add("FAST_PROCESSING");
        }
        
        // Set compliance status
        this.complianceChecks.setComplianceStatus(
            this.complianceChecks.getFlags().isEmpty() ? "PASSED" : "FLAGGED"
        );
    }
    
    /**
     * Perform fraud detection analysis
     */
    public void performFraudDetection() {
        this.fraudDetection.setLastAnalysisTime(java.time.LocalDateTime.now().toString());
        
        // Pattern analysis
        if (this.amount > 5000 && this.processingTime < 500) {
            this.fraudDetection.getRiskFactors().add("HIGH_AMOUNT_FAST_PROCESSING");
            this.fraudDetection.setRiskScore(75);
        }
        
        // Velocity analysis
        if (this.performanceMetrics.getHourlyTransactionCount() > 100) {
            this.fraudDetection.getRiskFactors().add("HIGH_VELOCITY");
            this.fraudDetection.setRiskScore(85);
        }
        
        // Set fraud status
        if (this.fraudDetection.getRiskScore() > 70) {
            this.fraudDetection.setFraudStatus("HIGH_RISK");
        } else if (this.fraudDetection.getRiskScore() > 40) {
            this.fraudDetection.setFraudStatus("MEDIUM_RISK");
        } else {
            this.fraudDetection.setFraudStatus("LOW_RISK");
        }
    }
    
    /**
     * Update performance metrics
     */
    public void updatePerformanceMetrics(Long processingTime, Integer hourlyCount, Integer dailyCount) {
        this.processingTime = processingTime;
        this.performanceMetrics.setProcessingTime(processingTime);
        this.performanceMetrics.setHourlyTransactionCount(hourlyCount);
        this.performanceMetrics.setDailyTransactionCount(dailyCount);
        this.performanceMetrics.setLastUpdateTime(java.time.LocalDateTime.now().toString());
    }
    
    /**
     * Complete index entry
     */
    public void completeIndexing() {
        this.status = "COMPLETED";
        this.indexMetadata.setIndexingCompleteTime(java.time.LocalDateTime.now().toString());
        addAuditEntry("INDEXING_COMPLETED", "Transaction successfully indexed");
        performComplianceChecks();
        performFraudDetection();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    
    public String getRoundId() { return roundId; }
    public void setRoundId(String roundId) { this.roundId = roundId; }
    
    public String getAgentPlayerId() { return agentPlayerId; }
    public void setAgentPlayerId(String agentPlayerId) { this.agentPlayerId = agentPlayerId; }
    
    public String getVendorId() { return vendorId; }
    public void setVendorId(String vendorId) { this.vendorId = vendorId; }
    
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
    
    public String getBetAmount() { return betAmount; }
    public void setBetAmount(String betAmount) { this.betAmount = betAmount; }
    
    public Long getCreateTime() { return createTime; }
    public void setCreateTime(Long createTime) { this.createTime = createTime; }
    
    public String getEffectiveTurnover() { return effectiveTurnover; }
    public void setEffectiveTurnover(String effectiveTurnover) { this.effectiveTurnover = effectiveTurnover; }
    
    public String getGameCategory() { return gameCategory; }
    public void setGameCategory(String gameCategory) { this.gameCategory = gameCategory; }
    
    public String getJackpotAmount() { return jackpotAmount; }
    public void setJackpotAmount(String jackpotAmount) { this.jackpotAmount = jackpotAmount; }
    
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
    
    public String getResultType() { return resultType; }
    public void setResultType(String resultType) { this.resultType = resultType; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Long getProcessingTime() { return processingTime; }
    public void setProcessingTime(Long processingTime) { this.processingTime = processingTime; }
    
    public Double getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(Double balanceBefore) { this.balanceBefore = balanceBefore; }
    
    public Double getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Double balanceAfter) { this.balanceAfter = balanceAfter; }
    
    public IndexMetadata getIndexMetadata() { return indexMetadata; }
    public void setIndexMetadata(IndexMetadata indexMetadata) { this.indexMetadata = indexMetadata; }
    
    public AuditTrail getAuditTrail() { return auditTrail; }
    public void setAuditTrail(AuditTrail auditTrail) { this.auditTrail = auditTrail; }
    
    public ComplianceChecks getComplianceChecks() { return complianceChecks; }
    public void setComplianceChecks(ComplianceChecks complianceChecks) { this.complianceChecks = complianceChecks; }
    
    public FraudDetection getFraudDetection() { return fraudDetection; }
    public void setFraudDetection(FraudDetection fraudDetection) { this.fraudDetection = fraudDetection; }
    
    public PerformanceMetrics getPerformanceMetrics() { return performanceMetrics; }
    public void setPerformanceMetrics(PerformanceMetrics performanceMetrics) { this.performanceMetrics = performanceMetrics; }
    
    /**
     * Index metadata for indexing operations
     */
    public static class IndexMetadata {
        @JsonProperty("indexingStartTime")
        private String indexingStartTime;
        
        @JsonProperty("indexingCompleteTime")
        private String indexingCompleteTime;
        
        @JsonProperty("indexVersion")
        private String indexVersion;
        
        @JsonProperty("indexingSource")
        private String indexingSource;
        
        @JsonProperty("indexingMethod")
        private String indexingMethod;
        
        public IndexMetadata() {
            this.indexingStartTime = java.time.LocalDateTime.now().toString().toString();
            this.indexVersion = "1.0";
            this.indexingSource = "TRANSACTION_PROCESSOR";
            this.indexingMethod = "REAL_TIME";
        }
        
        // Getters and Setters
        public String getIndexingStartTime() { return indexingStartTime; }
        public void setIndexingStartTime(String indexingStartTime) { this.indexingStartTime = indexingStartTime; }
        
        public String getIndexingCompleteTime() { return indexingCompleteTime; }
        public void setIndexingCompleteTime(String indexingCompleteTime) { this.indexingCompleteTime = indexingCompleteTime; }
        
        public String getIndexVersion() { return indexVersion; }
        public void setIndexVersion(String indexVersion) { this.indexVersion = indexVersion; }
        
        public String getIndexingSource() { return indexingSource; }
        public void setIndexingSource(String indexingSource) { this.indexingSource = indexingSource; }
        
        public String getIndexingMethod() { return indexingMethod; }
        public void setIndexingMethod(String indexingMethod) { this.indexingMethod = indexingMethod; }
    }
    
    /**
     * Audit trail for compliance and tracking
     */
    public static class AuditTrail {
        @JsonProperty("entries")
        private List<AuditEntry> entries = new ArrayList<>();
        
        public List<AuditEntry> getEntries() { return entries; }
        public void setEntries(List<AuditEntry> entries) { this.entries = entries; }
        
        public static class AuditEntry {
            @JsonProperty("event")
            private String event;
            
            @JsonProperty("details")
            private String details;
            
            @JsonProperty("timestamp")
            private String timestamp;
            
            @JsonProperty("userId")
            private String userId;
            
            @JsonProperty("systemId")
            private String systemId;
            
            public AuditEntry() {
                this.systemId = "TRANSACTION_INDEX_SYSTEM";
            }
            
            // Getters and Setters
            public String getEvent() { return event; }
            public void setEvent(String event) { this.event = event; }
            
            public String getDetails() { return details; }
            public void setDetails(String details) { this.details = details; }
            
            public String getTimestamp() { return timestamp; }
            public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
            
            public String getUserId() { return userId; }
            public void setUserId(String userId) { this.userId = userId; }
            
            public String getSystemId() { return systemId; }
            public void setSystemId(String systemId) { this.systemId = systemId; }
        }
    }
    
    /**
     * Compliance checks for regulatory requirements
     */
    public static class ComplianceChecks {
        @JsonProperty("complianceStatus")
        private String complianceStatus; // PASSED, FLAGGED, FAILED
        
        @JsonProperty("lastCheckTime")
        private String lastCheckTime;
        
        @JsonProperty("flags")
        private List<String> flags = new ArrayList<>();
        
        @JsonProperty("regulatoryRequirements")
        private List<String> regulatoryRequirements = new ArrayList<>();
        
        public ComplianceChecks() {
            this.complianceStatus = "PENDING";
        }
        
        // Getters and Setters
        public String getComplianceStatus() { return complianceStatus; }
        public void setComplianceStatus(String complianceStatus) { this.complianceStatus = complianceStatus; }
        
        public String getLastCheckTime() { return lastCheckTime; }
        public void setLastCheckTime(String lastCheckTime) { this.lastCheckTime = lastCheckTime; }
        
        public List<String> getFlags() { return flags; }
        public void setFlags(List<String> flags) { this.flags = flags; }
        
        public List<String> getRegulatoryRequirements() { return regulatoryRequirements; }
        public void setRegulatoryRequirements(List<String> regulatoryRequirements) { this.regulatoryRequirements = regulatoryRequirements; }
    }
    
    /**
     * Fraud detection analysis
     */
    public static class FraudDetection {
        @JsonProperty("fraudStatus")
        private String fraudStatus; // LOW_RISK, MEDIUM_RISK, HIGH_RISK, BLOCKED
        
        @JsonProperty("riskScore")
        private Integer riskScore;
        
        @JsonProperty("riskFactors")
        private List<String> riskFactors = new ArrayList<>();
        
        @JsonProperty("lastAnalysisTime")
        private String lastAnalysisTime;
        
        @JsonProperty("fraudIndicators")
        private List<String> fraudIndicators = new ArrayList<>();
        
        public FraudDetection() {
            this.fraudStatus = "LOW_RISK";
            this.riskScore = 10;
        }
        
        // Getters and Setters
        public String getFraudStatus() { return fraudStatus; }
        public void setFraudStatus(String fraudStatus) { this.fraudStatus = fraudStatus; }
        
        public Integer getRiskScore() { return riskScore; }
        public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
        
        public List<String> getRiskFactors() { return riskFactors; }
        public void setRiskFactors(List<String> riskFactors) { this.riskFactors = riskFactors; }
        
        public String getLastAnalysisTime() { return lastAnalysisTime; }
        public void setLastAnalysisTime(String lastAnalysisTime) { this.lastAnalysisTime = lastAnalysisTime; }
        
        public List<String> getFraudIndicators() { return fraudIndicators; }
        public void setFraudIndicators(List<String> fraudIndicators) { this.fraudIndicators = fraudIndicators; }
    }
    
    /**
     * Performance metrics for optimization
     */
    public static class PerformanceMetrics {
        @JsonProperty("processingTime")
        private Long processingTime;
        
        @JsonProperty("hourlyTransactionCount")
        private Integer hourlyTransactionCount;
        
        @JsonProperty("dailyTransactionCount")
        private Integer dailyTransactionCount;
        
        @JsonProperty("lastUpdateTime")
        private String lastUpdateTime;
        
        @JsonProperty("systemLoad")
        private Double systemLoad;
        
        public PerformanceMetrics() {
            this.processingTime = 0L;
            this.hourlyTransactionCount = 0;
            this.dailyTransactionCount = 0;
            this.systemLoad = 0.0;
        }
        
        // Getters and Setters
        public Long getProcessingTime() { return processingTime; }
        public void setProcessingTime(Long processingTime) { this.processingTime = processingTime; }
        
        public Integer getHourlyTransactionCount() { return hourlyTransactionCount; }
        public void setHourlyTransactionCount(Integer hourlyTransactionCount) { this.hourlyTransactionCount = hourlyTransactionCount; }
        
        public Integer getDailyTransactionCount() { return dailyTransactionCount; }
        public void setDailyTransactionCount(Integer dailyTransactionCount) { this.dailyTransactionCount = dailyTransactionCount; }
        
        public String getLastUpdateTime() { return lastUpdateTime; }
        public void setLastUpdateTime(String lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
        
        public Double getSystemLoad() { return systemLoad; }
        public void setSystemLoad(Double systemLoad) { this.systemLoad = systemLoad; }
    }
}