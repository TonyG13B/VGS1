
package com.vgs.kvpoc.embedded.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class CircuitBreakerService {
    
    private enum State { CLOSED, OPEN, HALF_OPEN }
    
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicReference<LocalDateTime> lastFailureTime = new AtomicReference<>();
    
    private final int failureThreshold = 5;
    private final int successThreshold = 3;
    private final Duration timeout = Duration.ofMinutes(1);
    
    public boolean allowRequest() {
        State currentState = state.get();
        
        switch (currentState) {
            case CLOSED:
                return true;
            case OPEN:
                if (shouldAttemptReset()) {
                    state.set(State.HALF_OPEN);
                    successCount.set(0);
                    return true;
                }
                return false;
            case HALF_OPEN:
                return true;
            default:
                return false;
        }
    }
    
    public void recordSuccess() {
        if (state.get() == State.HALF_OPEN) {
            if (successCount.incrementAndGet() >= successThreshold) {
                state.set(State.CLOSED);
                failureCount.set(0);
            }
        } else {
            failureCount.set(0);
        }
    }
    
    public void recordFailure() {
        lastFailureTime.set(LocalDateTime.now());
        
        if (state.get() == State.HALF_OPEN) {
            state.set(State.OPEN);
        } else if (failureCount.incrementAndGet() >= failureThreshold) {
            state.set(State.OPEN);
        }
    }
    
    private boolean shouldAttemptReset() {
        LocalDateTime lastFailure = lastFailureTime.get();
        return lastFailure != null && 
               LocalDateTime.now().isAfter(lastFailure.plus(timeout));
    }
    
    public State getState() {
        return state.get();
    }
}
