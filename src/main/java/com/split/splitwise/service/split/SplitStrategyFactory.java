package com.split.splitwise.service.split;

import com.split.splitwise.entity.SplitType;
import com.split.splitwise.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Factory Pattern: Creates appropriate SplitStrategy based on SplitType.
 * 
 * Why Factory here?
 * - Encapsulates object creation logic
 * - Client (ExpenseService) doesn't need to know concrete strategy classes
 * - Easy to add new strategies without changing client code
 * 
 * Combined with Strategy Pattern, this is a powerful combination:
 * - Factory decides WHICH strategy
 * - Strategy decides HOW to split
 */
@Component
@RequiredArgsConstructor
public class SplitStrategyFactory {

    private final EqualSplitStrategy equalSplitStrategy;
    private final ExactSplitStrategy exactSplitStrategy;

    /**
     * Returns the appropriate strategy for the given split type.
     * 
     * @param splitType The type of split requested
     * @return The corresponding SplitStrategy implementation
     * @throws ValidationException if split type is not supported
     */
    public SplitStrategy getStrategy(SplitType splitType) {
        return switch (splitType) {
            case EQUAL -> equalSplitStrategy;
            case EXACT -> exactSplitStrategy;
        };
    }

    /**
     * Alternative implementation using Map for O(1) lookup.
     * Useful when you have many strategies.
     */
    // private final Map<SplitType, SplitStrategy> strategies;
    // 
    // public SplitStrategy getStrategy(SplitType splitType) {
    //     SplitStrategy strategy = strategies.get(splitType);
    //     if (strategy == null) {
    //         throw new ValidationException("Unsupported split type: " + splitType);
    //     }
    //     return strategy;
    // }
}
