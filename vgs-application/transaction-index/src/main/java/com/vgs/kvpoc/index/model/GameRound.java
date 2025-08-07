package com.vgs.kvpoc.index.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * ====================================================================
 * VGS KV POC - Transaction Index Pattern: GameRound Model
 * ====================================================================
 * 
 * WHAT THIS CLASS REPRESENTS:
 * This is a GameRound for the "Transaction Index" pattern. Unlike the embedded
 * pattern, this GameRound does NOT store full transaction details inside it.
 * Instead, it keeps lightweight "references" or "pointers" to transactions
 * that are stored separately in TransactionIndex documents.
 * 
 * WHY WE USE THIS PATTERN:
 * - Think of it like a library catalog card system
 * - The GameRound is like a catalog card that lists what books (transactions) exist
 * - The actual books (full transaction details) are stored on separate shelves
 * - This makes it very fast to search across ALL transactions from ALL rounds
 * 
 * HOW IT WORKS:
 * 1. GameRound stores basic round info + lightweight transaction references
 * 2. Each transaction reference points to a separate TransactionIndex document
 * 3. To get full transaction details, you look up the TransactionIndex by reference
 * 4. This creates fast lookups and supports complex queries across all data
 * 
 * REAL-WORLD EXAMPLE:
 * - Round ID: "poker-round-12345"
 * - Transaction References: ["txn-idx-001", "txn-idx-002", "txn-idx-003"]
 * - Full transaction details stored separately in TransactionIndex documents
 * - Can quickly find all transactions for a player across ALL rounds
 * 
 * BENEFITS FOR GAMING:
 * - Lightning-fast searches across all transactions (even from different rounds)
 * - Perfect for regulatory compliance and audit requirements
 * - Can handle millions of transactions without slowing down
 * - Easy to generate reports across all player activity
 * 
 * TRADE-OFFS:
 * - Requires more database calls to get complete round information
 * - Slightly more complex data relationships to manage
 * 
 * TECHNICAL DETAILS:
 * - Stored in Couchbase as lightweight JSON documents
 * - Contains references to separate TransactionIndex documents
 * - Includes enhanced compliance and risk assessment features
 */
public class GameRound {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("roundNumber")
    private Integer roundNumber;
    
    @JsonProperty("agentPlayerId")
    private String agentPlayerId;
    
    @JsonProperty("vendorId")
    private String vendorId;
    
    @JsonProperty("operatorId")
    private String operatorId;
    
    @JsonProperty("gameType")
    private String gameType;
    
    @JsonProperty("initialBalance")
    private Double initialBalance;
    
    @JsonProperty("currentBalance")
    private Double currentBalance;
    
    @JsonProperty("roundStatus")
    private String roundStatus; // ACTIVE, COMPLETED, CANCELLED, UNDER_REVIEW
    
    @JsonProperty("startTime")
    private String startTime;
    
    @JsonProperty("endTime")
    private String endTime;
    
    @JsonProperty("lastUpdateTime")
    private String lastUpdateTime;
    
    // THE HEART OF THE TRANSACTION INDEX PATTERN: LIGHTWEIGHT REFERENCES
    /**
     * TRANSACTION REFERENCES ARRAY - This is the core of the transaction index pattern!
     * 
     * WHAT THIS IS:
     * Instead of storing full transaction objects (like the embedded pattern),
     * this list contains lightweight "references" or "pointers" to transactions
     * that are stored separately in TransactionIndex documents.
     * 
     * WHY THIS IS POWERFUL:
     * - GameRound stays small and fast to read/update
     * - All transaction details are stored in separate, searchable TransactionIndex docs
     * - Can quickly search ALL transactions across ALL rounds
     * - Perfect for compliance and audit requirements
     * 
     * REAL EXAMPLE:
     * If a player bets $10, wins $20, gets a $5 bonus in one round:
     * transactionRefs = [
     *   {refId: "txn-idx-001", type: "BET", amount: 10.00},
     *   {refId: "txn-idx-002", type: "WIN", amount: 20.00},
     *   {refId: "txn-idx-003", type: "BONUS", amount: 5.00}
     * ]
     * Full details stored separately in TransactionIndex documents!
     */
    @JsonProperty("transactionRefs")
    private List<TransactionRef> transactionRefs = new ArrayList<>();
    
    // ADVANCED FEATURES FOR COMPLIANCE AND RISK MANAGEMENT
    @JsonProperty("roundMetrics")
    private RoundMetrics roundMetrics = new RoundMetrics();        // Performance and volume metrics
    
    @JsonProperty("complianceInfo")
    private ComplianceInfo complianceInfo = new ComplianceInfo();  // Regulatory compliance tracking
    
    @JsonProperty("riskAssessment")
    private RiskAssessment riskAssessment = new RiskAssessment();  // Fraud and risk analysis
    
    // Constructors
    public GameRound() {
        this.startTime = java.time.LocalDateTime.now().toString().toString();
        this.lastUpdateTime = java.time.LocalDateTime.now().toString().toString();
        this.roundStatus = "ACTIVE";
    }
    
    public GameRound(String id, Integer roundNumber, String agentPlayerId, String vendorId, Double initialBalance) {
        this();
        this.id = id;
        this.roundNumber = roundNumber;
        this.agentPlayerId = agentPlayerId;
        this.vendorId = vendorId;
        this.operatorId = vendorId;
        this.initialBalance = initialBalance;
        this.currentBalance = initialBalance;
        this.gameType = "STANDARD";
    }
    
    // Business Methods for Transaction Index Pattern
    
    /**
     * Add transaction reference - lightweight approach
     */
    public void addTransactionRef(String transactionId, String type, Double amount, String timestamp) {
        TransactionRef ref = new TransactionRef(transactionId, type, amount, timestamp);
        ref.setSequenceNumber(this.transactionRefs.size() + 1);
        this.transactionRefs.add(ref);
        
        // Update balance and metrics
        updateBalanceFromTransactionRef(ref);
        updateRoundMetrics();
        this.lastUpdateTime = java.time.LocalDateTime.now().toString().toString();
    }
    
    /**
     * Update balance based on transaction reference
     */
    private void updateBalanceFromTransactionRef(TransactionRef ref) {
        switch (ref.getType()) {
            case "BET":
                this.currentBalance -= ref.getAmount();
                break;
            case "WIN":
                this.currentBalance += ref.getAmount();
                break;
            case "REFUND":
                this.currentBalance += ref.getAmount();
                break;
            case "BONUS":
                this.currentBalance += ref.getAmount();
                break;
        }
    }
    
    /**
     * Update round metrics from transaction references
     */
    private void updateRoundMetrics() {
        this.roundMetrics.setTotalTransactions(this.transactionRefs.size());
        this.roundMetrics.setTotalBets(this.transactionRefs.stream()
            .filter(ref -> "BET".equals(ref.getType()))
            .mapToDouble(TransactionRef::getAmount)
            .sum());
        this.roundMetrics.setTotalWins(this.transactionRefs.stream()
            .filter(ref -> "WIN".equals(ref.getType()))
            .mapToDouble(TransactionRef::getAmount)
            .sum());
        this.roundMetrics.setNetAmount(this.roundMetrics.getTotalWins() - this.roundMetrics.getTotalBets());
        
        // Update risk assessment
        updateRiskAssessment();
    }
    
    /**
     * Update risk assessment based on transaction patterns
     */
    private void updateRiskAssessment() {
        // Simple risk scoring based on transaction patterns
        double totalAmount = this.transactionRefs.stream()
            .mapToDouble(TransactionRef::getAmount)
            .sum();
        
        if (totalAmount > 10000) {
            this.riskAssessment.setRiskLevel("HIGH");
            this.riskAssessment.setRiskScore(85);
        } else if (totalAmount > 5000) {
            this.riskAssessment.setRiskLevel("MEDIUM");
            this.riskAssessment.setRiskScore(65);
        } else {
            this.riskAssessment.setRiskLevel("LOW");
            this.riskAssessment.setRiskScore(25);
        }
        
        this.riskAssessment.setLastAssessmentTime(java.time.LocalDateTime.now().toString());
    }
    
    /**
     * Complete round with compliance checks
     */
    public void completeRound() {
        this.roundStatus = "COMPLETED";
        this.endTime = java.time.LocalDateTime.now().toString().toString();
        this.lastUpdateTime = java.time.LocalDateTime.now().toString().toString();
        
        // Update compliance info
        this.complianceInfo.setComplianceStatus("VERIFIED");
        this.complianceInfo.setVerificationTime(java.time.LocalDateTime.now().toString());
        
        updateRoundMetrics();
    }
    
    /**
     * Flag round for review (compliance/fraud detection)
     */
    public void flagForReview(String reason) {
        this.roundStatus = "UNDER_REVIEW";
        this.complianceInfo.setComplianceStatus("UNDER_REVIEW");
        this.complianceInfo.setReviewReason(reason);
        this.complianceInfo.setReviewStartTime(java.time.LocalDateTime.now().toString());
        this.lastUpdateTime = java.time.LocalDateTime.now().toString().toString();
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
    
    public String getGameType() { return gameType; }
    public void setGameType(String gameType) { this.gameType = gameType; }
    
    public Double getInitialBalance() { return initialBalance; }
    public void setInitialBalance(Double initialBalance) { this.initialBalance = initialBalance; }
    
    public Double getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(Double currentBalance) { this.currentBalance = currentBalance; }
    
    public String getRoundStatus() { return roundStatus; }
    public void setRoundStatus(String roundStatus) { this.roundStatus = roundStatus; }
    
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    
    public String getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(String lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
    
    public void setLastUpdateTimestamp(Long timestamp) { 
        this.lastUpdateTime = String.valueOf(timestamp); 
    }
    
    public List<TransactionRef> getTransactionRefs() { return transactionRefs; }
    public void setTransactionRefs(List<TransactionRef> transactionRefs) { this.transactionRefs = transactionRefs; }
    
    public RoundMetrics getRoundMetrics() { return roundMetrics; }
    public void setRoundMetrics(RoundMetrics roundMetrics) { this.roundMetrics = roundMetrics; }
    
    public ComplianceInfo getComplianceInfo() { return complianceInfo; }
    public void setComplianceInfo(ComplianceInfo complianceInfo) { this.complianceInfo = complianceInfo; }
    
    public RiskAssessment getRiskAssessment() { return riskAssessment; }
    public void setRiskAssessment(RiskAssessment riskAssessment) { this.riskAssessment = riskAssessment; }
    
    /**
     * Lightweight transaction reference for index pattern
     */
    public static class TransactionRef {
        @JsonProperty("transactionId")
        private String transactionId;
        
        @JsonProperty("sequenceNumber")
        private Integer sequenceNumber;
        
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("amount")
        private Double amount;
        
        @JsonProperty("timestamp")
        private String timestamp;
        
        @JsonProperty("status")
        private String status;
        
        public TransactionRef() {}
        
        public TransactionRef(String transactionId, String type, Double amount, String timestamp) {
            this.transactionId = transactionId;
            this.type = type;
            this.amount = amount;
            this.timestamp = timestamp;
            this.status = "INDEXED";
        }
        
        // Getters and Setters
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        
        public Integer getSequenceNumber() { return sequenceNumber; }
        public void setSequenceNumber(Integer sequenceNumber) { this.sequenceNumber = sequenceNumber; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    
    /**
     * Round metrics for performance tracking
     */
    public static class RoundMetrics {
        @JsonProperty("totalTransactions")
        private Integer totalTransactions = 0;
        
        @JsonProperty("totalBets")
        private Double totalBets = 0.0;
        
        @JsonProperty("totalWins")
        private Double totalWins = 0.0;
        
        @JsonProperty("netAmount")
        private Double netAmount = 0.0;
        
        @JsonProperty("avgTransactionTime")
        private Long avgTransactionTime = 0L;
        
        // Getters and Setters
        public Integer getTotalTransactions() { return totalTransactions; }
        public void setTotalTransactions(Integer totalTransactions) { this.totalTransactions = totalTransactions; }
        
        public Double getTotalBets() { return totalBets; }
        public void setTotalBets(Double totalBets) { this.totalBets = totalBets; }
        
        public Double getTotalWins() { return totalWins; }
        public void setTotalWins(Double totalWins) { this.totalWins = totalWins; }
        
        public Double getNetAmount() { return netAmount; }
        public void setNetAmount(Double netAmount) { this.netAmount = netAmount; }
        
        public Long getAvgTransactionTime() { return avgTransactionTime; }
        public void setAvgTransactionTime(Long avgTransactionTime) { this.avgTransactionTime = avgTransactionTime; }
    }
    
    /**
     * Compliance information for regulatory requirements
     */
    public static class ComplianceInfo {
        @JsonProperty("complianceStatus")
        private String complianceStatus; // VERIFIED, UNDER_REVIEW, FLAGGED
        
        @JsonProperty("verificationTime")
        private String verificationTime;
        
        @JsonProperty("reviewReason")
        private String reviewReason;
        
        @JsonProperty("reviewStartTime")
        private String reviewStartTime;
        
        @JsonProperty("complianceNotes")
        private String complianceNotes;
        
        @JsonProperty("regulatoryFlags")
        private List<String> regulatoryFlags = new ArrayList<>();
        
        public ComplianceInfo() {
            this.complianceStatus = "PENDING";
        }
        
        // Getters and Setters
        public String getComplianceStatus() { return complianceStatus; }
        public void setComplianceStatus(String complianceStatus) { this.complianceStatus = complianceStatus; }
        
        public String getVerificationTime() { return verificationTime; }
        public void setVerificationTime(String verificationTime) { this.verificationTime = verificationTime; }
        
        public String getReviewReason() { return reviewReason; }
        public void setReviewReason(String reviewReason) { this.reviewReason = reviewReason; }
        
        public String getReviewStartTime() { return reviewStartTime; }
        public void setReviewStartTime(String reviewStartTime) { this.reviewStartTime = reviewStartTime; }
        
        public String getComplianceNotes() { return complianceNotes; }
        public void setComplianceNotes(String complianceNotes) { this.complianceNotes = complianceNotes; }
        
        public List<String> getRegulatoryFlags() { return regulatoryFlags; }
        public void setRegulatoryFlags(List<String> regulatoryFlags) { this.regulatoryFlags = regulatoryFlags; }
    }
    
    /**
     * Risk assessment for fraud detection
     */
    public static class RiskAssessment {
        @JsonProperty("riskLevel")
        private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
        
        @JsonProperty("riskScore")
        private Integer riskScore; // 0-100
        
        @JsonProperty("riskFactors")
        private List<String> riskFactors = new ArrayList<>();
        
        @JsonProperty("lastAssessmentTime")
        private String lastAssessmentTime;
        
        @JsonProperty("fraudIndicators")
        private List<String> fraudIndicators = new ArrayList<>();
        
        public RiskAssessment() {
            this.riskLevel = "LOW";
            this.riskScore = 10;
        }
        
        // Getters and Setters
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        
        public Integer getRiskScore() { return riskScore; }
        public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
        
        public List<String> getRiskFactors() { return riskFactors; }
        public void setRiskFactors(List<String> riskFactors) { this.riskFactors = riskFactors; }
        
        public String getLastAssessmentTime() { return lastAssessmentTime; }
        public void setLastAssessmentTime(String lastAssessmentTime) { this.lastAssessmentTime = lastAssessmentTime; }
        
        public List<String> getFraudIndicators() { return fraudIndicators; }
        public void setFraudIndicators(List<String> fraudIndicators) { this.fraudIndicators = fraudIndicators; }
    }
}