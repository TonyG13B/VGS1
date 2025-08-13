
#!/bin/bash

echo "VGS Performance Validation..."
echo "Timestamp: $(date)"

# Performance thresholds
MAX_AVG_RESPONSE_TIME=200  # milliseconds
MIN_REQUESTS_PER_SEC=50
MAX_ERROR_RATE=5  # percentage

# Function to validate performance metrics
validate_performance() {
    local csv_file=$1
    local test_name=$2
    
    echo "Validating performance for $test_name..."
    
    # Extract metrics from CSV
    local avg_response=$(awk -F',' 'NR==2 {print $8}' "$csv_file")
    local requests_per_sec=$(awk -F',' 'NR==2 {print $11}' "$csv_file")
    local failure_count=$(awk -F',' 'NR==2 {print $5}')
    local request_count=$(awk -F',' 'NR==2 {print $4}')
    
    # Calculate error rate
    local error_rate=$(echo "scale=2; $failure_count * 100 / $request_count" | bc)
    
    echo "  Average Response Time: ${avg_response}ms"
    echo "  Requests per Second: $requests_per_sec"
    echo "  Error Rate: ${error_rate}%"
    
    # Validate thresholds
    local passed=true
    
    if (( $(echo "$avg_response > $MAX_AVG_RESPONSE_TIME" | bc -l) )); then
        echo "  ❌ FAIL: Average response time exceeds threshold (${avg_response}ms > ${MAX_AVG_RESPONSE_TIME}ms)"
        passed=false
    else
        echo "  ✅ PASS: Average response time within threshold"
    fi
    
    if (( $(echo "$requests_per_sec < $MIN_REQUESTS_PER_SEC" | bc -l) )); then
        echo "  ❌ FAIL: Requests per second below threshold ($requests_per_sec < $MIN_REQUESTS_PER_SEC)"
        passed=false
    else
        echo "  ✅ PASS: Requests per second meets threshold"
    fi
    
    if (( $(echo "$error_rate > $MAX_ERROR_RATE" | bc -l) )); then
        echo "  ❌ FAIL: Error rate exceeds threshold (${error_rate}% > ${MAX_ERROR_RATE}%)"
        passed=false
    else
        echo "  ✅ PASS: Error rate within threshold"
    fi
    
    return $passed
}

# Find latest test results
LATEST_RESULTS=$(ls -td results/*/ | head -1)

if [ -z "$LATEST_RESULTS" ]; then
    echo "No test results found. Please run comprehensive tests first."
    exit 1
fi

echo "Validating results from: $LATEST_RESULTS"

# Validate each test
overall_passed=true

for csv_file in "$LATEST_RESULTS"*_stats.csv; do
    if [ -f "$csv_file" ]; then
        test_name=$(basename "$csv_file" _stats.csv)
        if ! validate_performance "$csv_file" "$test_name"; then
            overall_passed=false
        fi
        echo ""
    fi
done

# Final result
if $overall_passed; then
    echo "🎉 ALL PERFORMANCE VALIDATIONS PASSED!"
    exit 0
else
    echo "❌ SOME PERFORMANCE VALIDATIONS FAILED!"
    exit 1
fi
