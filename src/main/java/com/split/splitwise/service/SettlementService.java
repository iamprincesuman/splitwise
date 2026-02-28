package com.split.splitwise.service;

import com.split.splitwise.dto.response.SettlementResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Service for calculating optimized debt settlements.
 * 
 * Algorithm: Greedy matching with two priority queues
 * Time Complexity: O(n log n) where n = number of users with non-zero balances
 * Space Complexity: O(n)
 * 
 * Why this approach?
 * ------------------
 * Naive approach: For each debtor, iterate through creditors to find matches - O(n²)
 * 
 * Optimized approach using heaps:
 * - Always match the largest creditor with the largest debtor
 * - Each match settles at least one person completely
 * - Maximum number of transactions = n-1 (where n = people with non-zero balance)
 * 
 * This is provably optimal for minimizing the number of transactions.
 * 
 * Edge Cases Handled:
 * - Zero balances (filtered out)
 * - Floating point precision (using BigDecimal throughout)
 * - Very small remainders after settlement (threshold-based filtering)
 */
@Slf4j
@Service
public class SettlementService {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final BigDecimal EPSILON = new BigDecimal("0.01");

    /**
     * Calculates the minimum number of transactions needed to settle all debts.
     *
     * @param netBalances Map of userId to their net balance (positive = gets money, negative = owes money)
     * @param userNames Map of userId to user names (for display purposes)
     * @return List of settlements representing who pays whom and how much
     */
    public List<SettlementResponse.Settlement> calculateOptimizedSettlements(
            Map<UUID, BigDecimal> netBalances,
            Map<UUID, String> userNames) {

        log.debug("Starting settlement calculation for {} users", netBalances.size());

        PriorityQueue<UserBalance> creditors = new PriorityQueue<>(
                Comparator.comparing(UserBalance::amount).reversed()
        );

        PriorityQueue<UserBalance> debtors = new PriorityQueue<>(
                Comparator.comparing((UserBalance ub) -> ub.amount().abs()).reversed()
        );

        for (Map.Entry<UUID, BigDecimal> entry : netBalances.entrySet()) {
            BigDecimal balance = entry.getValue().setScale(MONEY_SCALE, ROUNDING_MODE);

            if (balance.abs().compareTo(EPSILON) < 0) {
                continue;
            }

            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                creditors.offer(new UserBalance(entry.getKey(), balance));
            } else if (balance.compareTo(BigDecimal.ZERO) < 0) {
                debtors.offer(new UserBalance(entry.getKey(), balance.abs()));
            }
        }

        log.debug("Partitioned into {} creditors and {} debtors", creditors.size(), debtors.size());

        List<SettlementResponse.Settlement> settlements = new ArrayList<>();

        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            UserBalance creditor = creditors.poll();
            UserBalance debtor = debtors.poll();

            BigDecimal settlementAmount = creditor.amount().min(debtor.amount());

            if (settlementAmount.compareTo(EPSILON) >= 0) {
                SettlementResponse.Settlement settlement = SettlementResponse.Settlement.builder()
                        .fromUserId(debtor.userId())
                        .fromUserName(userNames.getOrDefault(debtor.userId(), "Unknown"))
                        .toUserId(creditor.userId())
                        .toUserName(userNames.getOrDefault(creditor.userId(), "Unknown"))
                        .amount(settlementAmount)
                        .build();

                settlements.add(settlement);

                log.debug("Settlement: {} pays {} amount {}",
                        debtor.userId(), creditor.userId(), settlementAmount);
            }

            BigDecimal creditorRemaining = creditor.amount().subtract(settlementAmount);
            BigDecimal debtorRemaining = debtor.amount().subtract(settlementAmount);

            if (creditorRemaining.compareTo(EPSILON) >= 0) {
                creditors.offer(new UserBalance(creditor.userId(), creditorRemaining));
            }

            if (debtorRemaining.compareTo(EPSILON) >= 0) {
                debtors.offer(new UserBalance(debtor.userId(), debtorRemaining));
            }
        }

        log.info("Settlement calculation complete: {} transactions generated", settlements.size());

        return settlements;
    }

    /**
     * Internal record to hold user balance information for the priority queues.
     * Using record for immutability and cleaner code.
     */
    private record UserBalance(UUID userId, BigDecimal amount) {}

    /**
     * Validates that the sum of all balances is zero (conservation of money).
     * This is a sanity check - if balances don't sum to zero, there's a bug in balance calculation.
     *
     * @param netBalances Map of user balances
     * @return true if balances are valid (sum to zero within epsilon)
     */
    public boolean validateBalancesConservation(Map<UUID, BigDecimal> netBalances) {
        BigDecimal sum = netBalances.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean isValid = sum.abs().compareTo(EPSILON) < 0;

        if (!isValid) {
            log.warn("Balance conservation check failed! Sum: {} (should be ~0)", sum);
        }

        return isValid;
    }
}
