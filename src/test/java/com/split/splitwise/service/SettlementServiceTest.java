package com.split.splitwise.service;

import com.split.splitwise.dto.response.SettlementResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the debt simplification algorithm.
 * 
 * These tests verify:
 * - Correct settlement amounts
 * - Minimum number of transactions
 * - Proper handling of edge cases
 * - BigDecimal precision handling
 */
class SettlementServiceTest {

    private SettlementService settlementService;
    private Map<UUID, String> userNames;

    @BeforeEach
    void setUp() {
        settlementService = new SettlementService();
        userNames = new HashMap<>();
    }

    @Nested
    @DisplayName("Basic Settlement Scenarios")
    class BasicSettlementScenarios {

        @Test
        @DisplayName("Should settle simple two-person debt")
        void shouldSettleSimpleTwoPersonDebt() {
            UUID alice = UUID.randomUUID();
            UUID bob = UUID.randomUUID();
            userNames.put(alice, "Alice");
            userNames.put(bob, "Bob");

            Map<UUID, BigDecimal> balances = Map.of(
                    alice, new BigDecimal("50.00"),
                    bob, new BigDecimal("-50.00")
            );

            List<SettlementResponse.Settlement> settlements = 
                    settlementService.calculateOptimizedSettlements(balances, userNames);

            assertThat(settlements).hasSize(1);
            assertThat(settlements.get(0).getFromUserId()).isEqualTo(bob);
            assertThat(settlements.get(0).getToUserId()).isEqualTo(alice);
            assertThat(settlements.get(0).getAmount()).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("Should handle three-way settlement")
        void shouldHandleThreeWaySettlement() {
            UUID alice = UUID.randomUUID();
            UUID bob = UUID.randomUUID();
            UUID charlie = UUID.randomUUID();
            userNames.put(alice, "Alice");
            userNames.put(bob, "Bob");
            userNames.put(charlie, "Charlie");

            Map<UUID, BigDecimal> balances = Map.of(
                    alice, new BigDecimal("100.00"),
                    bob, new BigDecimal("-60.00"),
                    charlie, new BigDecimal("-40.00")
            );

            List<SettlementResponse.Settlement> settlements = 
                    settlementService.calculateOptimizedSettlements(balances, userNames);

            assertThat(settlements).hasSize(2);
            
            BigDecimal totalSettled = settlements.stream()
                    .map(SettlementResponse.Settlement::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(totalSettled).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("Should minimize transactions for complex scenario")
        void shouldMinimizeTransactionsForComplexScenario() {
            UUID user1 = UUID.randomUUID();
            UUID user2 = UUID.randomUUID();
            UUID user3 = UUID.randomUUID();
            UUID user4 = UUID.randomUUID();
            userNames.put(user1, "User1");
            userNames.put(user2, "User2");
            userNames.put(user3, "User3");
            userNames.put(user4, "User4");

            Map<UUID, BigDecimal> balances = Map.of(
                    user1, new BigDecimal("70.00"),
                    user2, new BigDecimal("30.00"),
                    user3, new BigDecimal("-50.00"),
                    user4, new BigDecimal("-50.00")
            );

            List<SettlementResponse.Settlement> settlements = 
                    settlementService.calculateOptimizedSettlements(balances, userNames);

            assertThat(settlements.size()).isLessThanOrEqualTo(3);
            
            verifySettlementsBalance(balances, settlements);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should return empty list when all balances are zero")
        void shouldReturnEmptyListWhenAllBalancesZero() {
            UUID alice = UUID.randomUUID();
            UUID bob = UUID.randomUUID();

            Map<UUID, BigDecimal> balances = Map.of(
                    alice, BigDecimal.ZERO,
                    bob, BigDecimal.ZERO
            );

            List<SettlementResponse.Settlement> settlements = 
                    settlementService.calculateOptimizedSettlements(balances, userNames);

            assertThat(settlements).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list for empty input")
        void shouldReturnEmptyListForEmptyInput() {
            List<SettlementResponse.Settlement> settlements = 
                    settlementService.calculateOptimizedSettlements(Collections.emptyMap(), userNames);

            assertThat(settlements).isEmpty();
        }

        @Test
        @DisplayName("Should handle very small amounts within epsilon")
        void shouldHandleVerySmallAmountsWithinEpsilon() {
            UUID alice = UUID.randomUUID();
            UUID bob = UUID.randomUUID();
            userNames.put(alice, "Alice");
            userNames.put(bob, "Bob");

            Map<UUID, BigDecimal> balances = Map.of(
                    alice, new BigDecimal("0.005"),
                    bob, new BigDecimal("-0.005")
            );

            List<SettlementResponse.Settlement> settlements = 
                    settlementService.calculateOptimizedSettlements(balances, userNames);

            assertThat(settlements).isEmpty();
        }

        @Test
        @DisplayName("Should handle single creditor with multiple debtors")
        void shouldHandleSingleCreditorMultipleDebtors() {
            UUID creditor = UUID.randomUUID();
            UUID debtor1 = UUID.randomUUID();
            UUID debtor2 = UUID.randomUUID();
            UUID debtor3 = UUID.randomUUID();
            userNames.put(creditor, "Creditor");
            userNames.put(debtor1, "Debtor1");
            userNames.put(debtor2, "Debtor2");
            userNames.put(debtor3, "Debtor3");

            Map<UUID, BigDecimal> balances = Map.of(
                    creditor, new BigDecimal("90.00"),
                    debtor1, new BigDecimal("-30.00"),
                    debtor2, new BigDecimal("-30.00"),
                    debtor3, new BigDecimal("-30.00")
            );

            List<SettlementResponse.Settlement> settlements = 
                    settlementService.calculateOptimizedSettlements(balances, userNames);

            assertThat(settlements).hasSize(3);
            
            settlements.forEach(s -> 
                    assertThat(s.getToUserId()).isEqualTo(creditor));
            
            BigDecimal total = settlements.stream()
                    .map(SettlementResponse.Settlement::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(total).isEqualByComparingTo("90.00");
        }
    }

    @Nested
    @DisplayName("Rounding and Precision")
    class RoundingAndPrecision {

        @Test
        @DisplayName("Should handle amounts with many decimal places")
        void shouldHandleAmountsWithManyDecimalPlaces() {
            UUID alice = UUID.randomUUID();
            UUID bob = UUID.randomUUID();
            userNames.put(alice, "Alice");
            userNames.put(bob, "Bob");

            Map<UUID, BigDecimal> balances = Map.of(
                    alice, new BigDecimal("33.333333333"),
                    bob, new BigDecimal("-33.333333333")
            );

            List<SettlementResponse.Settlement> settlements = 
                    settlementService.calculateOptimizedSettlements(balances, userNames);

            assertThat(settlements).hasSize(1);
            assertThat(settlements.get(0).getAmount().scale()).isLessThanOrEqualTo(2);
        }

        @Test
        @DisplayName("Should handle uneven three-way split remainder")
        void shouldHandleUnevenThreeWaySplitRemainder() {
            UUID alice = UUID.randomUUID();
            UUID bob = UUID.randomUUID();
            UUID charlie = UUID.randomUUID();
            userNames.put(alice, "Alice");
            userNames.put(bob, "Bob");
            userNames.put(charlie, "Charlie");

            Map<UUID, BigDecimal> balances = Map.of(
                    alice, new BigDecimal("66.67"),
                    bob, new BigDecimal("-33.33"),
                    charlie, new BigDecimal("-33.34")
            );

            List<SettlementResponse.Settlement> settlements = 
                    settlementService.calculateOptimizedSettlements(balances, userNames);

            verifySettlementsBalance(balances, settlements);
        }
    }

    @Nested
    @DisplayName("Large Group Simulation")
    class LargeGroupSimulation {

        @Test
        @DisplayName("Should handle 20 users efficiently")
        void shouldHandle20UsersEfficiently() {
            Map<UUID, BigDecimal> balances = new HashMap<>();
            Random random = new Random(42);
            BigDecimal totalPositive = BigDecimal.ZERO;
            BigDecimal totalNegative = BigDecimal.ZERO;

            for (int i = 0; i < 10; i++) {
                UUID userId = UUID.randomUUID();
                BigDecimal amount = BigDecimal.valueOf(random.nextInt(1000) + 100)
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                balances.put(userId, amount);
                userNames.put(userId, "Creditor" + i);
                totalPositive = totalPositive.add(amount);
            }

            for (int i = 0; i < 9; i++) {
                UUID userId = UUID.randomUUID();
                BigDecimal amount = BigDecimal.valueOf(random.nextInt(1000) + 100)
                        .setScale(2, java.math.RoundingMode.HALF_UP)
                        .negate();
                balances.put(userId, amount);
                userNames.put(userId, "Debtor" + i);
                totalNegative = totalNegative.add(amount.abs());
            }

            UUID lastDebtor = UUID.randomUUID();
            BigDecimal lastAmount = totalPositive.subtract(totalNegative).negate();
            balances.put(lastDebtor, lastAmount);
            userNames.put(lastDebtor, "LastDebtor");

            long startTime = System.currentTimeMillis();
            List<SettlementResponse.Settlement> settlements = 
                    settlementService.calculateOptimizedSettlements(balances, userNames);
            long endTime = System.currentTimeMillis();

            assertThat(endTime - startTime).isLessThan(100);
            assertThat(settlements.size()).isLessThanOrEqualTo(19);
            
            verifySettlementsBalance(balances, settlements);
        }

        @Test
        @DisplayName("Should handle 100 users")
        void shouldHandle100Users() {
            Map<UUID, BigDecimal> balances = new HashMap<>();
            BigDecimal runningBalance = BigDecimal.ZERO;

            for (int i = 0; i < 99; i++) {
                UUID userId = UUID.randomUUID();
                BigDecimal amount = BigDecimal.valueOf((i % 2 == 0 ? 1 : -1) * (i + 1))
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                balances.put(userId, amount);
                userNames.put(userId, "User" + i);
                runningBalance = runningBalance.add(amount);
            }

            UUID lastUser = UUID.randomUUID();
            balances.put(lastUser, runningBalance.negate());
            userNames.put(lastUser, "LastUser");

            long startTime = System.currentTimeMillis();
            List<SettlementResponse.Settlement> settlements = 
                    settlementService.calculateOptimizedSettlements(balances, userNames);
            long endTime = System.currentTimeMillis();

            assertThat(endTime - startTime).isLessThan(500);
            System.out.println("100 users: " + settlements.size() + 
                    " settlements in " + (endTime - startTime) + "ms");
        }
    }

    @Nested
    @DisplayName("Balance Conservation Validation")
    class BalanceConservationValidation {

        @Test
        @DisplayName("Should validate correct balance conservation")
        void shouldValidateCorrectBalanceConservation() {
            Map<UUID, BigDecimal> balances = Map.of(
                    UUID.randomUUID(), new BigDecimal("50.00"),
                    UUID.randomUUID(), new BigDecimal("-50.00")
            );

            assertThat(settlementService.validateBalancesConservation(balances)).isTrue();
        }

        @Test
        @DisplayName("Should detect incorrect balance conservation")
        void shouldDetectIncorrectBalanceConservation() {
            Map<UUID, BigDecimal> balances = Map.of(
                    UUID.randomUUID(), new BigDecimal("50.00"),
                    UUID.randomUUID(), new BigDecimal("-40.00")
            );

            assertThat(settlementService.validateBalancesConservation(balances)).isFalse();
        }
    }

    private void verifySettlementsBalance(
            Map<UUID, BigDecimal> originalBalances,
            List<SettlementResponse.Settlement> settlements) {

        Map<UUID, BigDecimal> remainingBalances = new HashMap<>(originalBalances);

        for (SettlementResponse.Settlement settlement : settlements) {
            remainingBalances.merge(
                    settlement.getFromUserId(),
                    settlement.getAmount(),
                    BigDecimal::add);
            remainingBalances.merge(
                    settlement.getToUserId(),
                    settlement.getAmount().negate(),
                    BigDecimal::add);
        }

        for (BigDecimal balance : remainingBalances.values()) {
            assertThat(balance.abs()).isLessThan(new BigDecimal("0.02"));
        }
    }
}
