package com.vgs.kvpoc.index.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Transaction Validator and Risk Assessment Utility
 * 
 * This utility class provides transaction validation and risk assessment
 * functionality for gaming transactions. It helps ensure transaction integrity
 * and provides risk scoring for regulatory compliance and fraud prevention.
 * 
 * Key features:
 * - Transaction validation (required fields, value ranges, etc.)
 * - Risk assessment scoring based on transaction patterns
 * - Regulatory compliance checks
 * - Fraud detection indicators
 */
public class TransactionValidator {

    // Risk level constants
    public static final String RISK_LOW = "LOW";
    public static final String RISK_MEDIUM = "MEDIUM";
    public static final String RISK_HIGH = "HIGH";
    
    /**
     * Validates a transaction and performs risk assessment
     * 
     * @param transactionId Unique transaction identifier
     * @param roundId Game round identifier
     * @param type Transaction type (BET, WIN, etc.)
     * @param amount Transaction amount
     * @param playerId Player identifier
     * @return Map containing validation results and risk assessment
     */
    public static Map<String, Object> validateAndAssessRisk(
            String transactionId, 
            String roundId, 
            String type, 
            Double amount, 
            String playerId) {
        
        Map<String, Object> result = new HashMap<>();
        boolean isValid = true;
        String riskLevel = RISK_LOW;
        String riskReason = "Standard transaction";
        
        // Basic validation
        if (transactionId == null || transactionId.isEmpty()) {
            isValid = false;
            result.put("error", "Transaction ID is required");
        } else if (roundId == null || roundId.isEmpty()) {
            isValid = false;
            result.put("error", "Round ID is required");
        } else if (type == null || type.isEmpty()) {
            isValid = false;
            result.put("error", "Transaction type is required");
        } else if (amount == null) {
            isValid = false;
            result.put("error", "Amount is required");
        }
        
        // Risk assessment based on transaction type and amount
        if (isValid) {
            // Check for high-value transactions
            if (amount > 1000.0) {
                riskLevel = RISK_HIGH;
                riskReason = "High-value transaction exceeding $1000";
            } else if (amount > 500.0) {
                riskLevel = RISK_MEDIUM;
                riskReason = "Medium-value transaction exceeding $500";
            }
            
            // Additional risk factors based on transaction type
            if ("BET".equals(type) && amount > 200.0) {
                riskLevel = Math.max(riskLevel.equals(RISK_HIGH) ? 3 : 
                                    riskLevel.equals(RISK_MEDIUM) ? 2 : 1, 2) == 3 ? 
                                    RISK_HIGH : RISK_MEDIUM;
                riskReason += ", Large bet amount";
            } else if ("WIN".equals(type) && amount > 300.0) {
                riskLevel = Math.max(riskLevel.equals(RISK_HIGH) ? 3 : 
                                    riskLevel.equals(RISK_MEDIUM) ? 2 : 1, 2) == 3 ? 
                                    RISK_HIGH : RISK_MEDIUM;
                riskReason += ", Large win amount";
            }
            
            // Regulatory compliance check
            if ("BONUS".equals(type) && amount > 100.0) {
                riskLevel = Math.max(riskLevel.equals(RISK_HIGH) ? 3 : 
                                    riskLevel.equals(RISK_MEDIUM) ? 2 : 1, 2) == 3 ? 
                                    RISK_HIGH : RISK_MEDIUM;
                riskReason += ", Large bonus amount requires verification";
            }
        }
        
        // Build result
        result.put("valid", isValid);
        result.put("riskLevel", riskLevel);
        result.put("riskReason", riskReason);
        result.put("transactionId", transactionId);
        result.put("roundId", roundId);
        result.put("timestamp", System.currentTimeMillis());
        
        return result;
    }
    
    /**
     * Performs advanced risk assessment for regulatory compliance
     * 
     * @param transactionId Transaction identifier
     * @param type Transaction type
     * @param amount Transaction amount
     * @param playerId Player identifier
     * @return Map containing detailed risk assessment
     */
    public static Map<String, Object> performAdvancedRiskAssessment(
            String transactionId,
            String type,
            Double amount,
            String playerId) {
        
        Map<String, Object> assessment = new HashMap<>();
        
        // Calculate risk score (0-100)
        int riskScore = 0;
        
        // Base score based on amount
        if (amount <= 10.0) {
            riskScore += 5;
        } else if (amount <= 100.0) {
            riskScore += 10;
        } else if (amount <= 500.0) {
            riskScore += 30;
        } else if (amount <= 1000.0) {
            riskScore += 50;
        } else {
            riskScore += 70;
        }
        
        // Adjust based on transaction type
        if ("BET".equals(type)) {
            riskScore += 10;
        } else if ("WIN".equals(type)) {
            riskScore += 15;
        } else if ("BONUS".equals(type)) {
            riskScore += 20;
        } else if ("REFUND".equals(type)) {
            riskScore += 25;
        }
        
        // Determine risk category
        String riskCategory;
        if (riskScore < 30) {
            riskCategory = RISK_LOW;
        } else if (riskScore < 60) {
            riskCategory = RISK_MEDIUM;
        } else {
            riskCategory = RISK_HIGH;
        }
        
        // Build assessment result
        assessment.put("riskScore", riskScore);
        assessment.put("riskCategory", riskCategory);
        assessment.put("requiresReview", riskScore >= 70);
        assessment.put("requiresApproval", riskScore >= 85);
        assessment.put("transactionId", transactionId);
        assessment.put("playerId", playerId);
        assessment.put("assessmentTimestamp", System.currentTimeMillis());
        
        return assessment;
    }
}