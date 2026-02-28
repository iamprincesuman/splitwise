package com.split.splitwise.service.split;

import com.split.splitwise.dto.request.CreateExpenseRequest;
import com.split.splitwise.entity.Expense;
import com.split.splitwise.entity.ExpenseSplit;

import java.util.List;
import java.util.UUID;

/**
 * Strategy Pattern: Defines a family of algorithms for splitting expenses.
 * 
 * Why Strategy Pattern here?
 * - Open/Closed Principle: Add new split types (PERCENTAGE, SHARES) without modifying existing code
 * - Single Responsibility: Each strategy handles one split algorithm
 * - Testability: Each strategy can be unit tested in isolation
 * 
 * Alternative (what we're replacing):
 * - Switch statement in service - violates OCP, grows with each new type
 */
public interface SplitStrategy {

    /**
     * Calculate expense splits based on the strategy implementation.
     *
     * @param expense The expense being split
     * @param groupId The group ID for member lookup
     * @param request The original request (may contain split details for EXACT)
     * @return List of expense splits
     */
    List<ExpenseSplit> calculateSplits(Expense expense, UUID groupId, CreateExpenseRequest request);
}
