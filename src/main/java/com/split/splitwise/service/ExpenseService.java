package com.split.splitwise.service;

import com.split.splitwise.dto.request.CreateExpenseRequest;
import com.split.splitwise.dto.response.BalanceResponse;
import com.split.splitwise.dto.response.ExpenseResponse;
import com.split.splitwise.dto.response.SettlementResponse;
import com.split.splitwise.entity.*;
import com.split.splitwise.mapper.ExpenseMapper;
import com.split.splitwise.repository.ExpenseRepository;
import com.split.splitwise.service.split.SplitStrategy;
import com.split.splitwise.service.split.SplitStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing expenses.
 * 
 * Design Patterns Used:
 * - Strategy Pattern: Split calculation delegated to SplitStrategy implementations
 * - Factory Pattern: SplitStrategyFactory creates appropriate strategy
 * 
 * This follows Open/Closed Principle - add new split types without modifying this class.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseService {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final ExpenseRepository expenseRepository;
    private final GroupService groupService;
    private final UserService userService;
    private final ExpenseMapper expenseMapper;
    private final SettlementService settlementService;
    private final SplitStrategyFactory splitStrategyFactory;

    @Transactional
    public ExpenseResponse createExpense(UUID groupId, CreateExpenseRequest request) {
        log.info("Creating expense in group {}: {} - {}", 
                groupId, request.getDescription(), request.getTotalAmount());

        Group group = groupService.findGroupByIdOrThrow(groupId);
        User payer = userService.findUserByIdOrThrow(request.getPaidBy());

        groupService.validateUserIsMember(groupId, request.getPaidBy());

        Expense expense = Expense.builder()
                .description(request.getDescription())
                .totalAmount(request.getTotalAmount().setScale(MONEY_SCALE, ROUNDING_MODE))
                .paidBy(payer)
                .group(group)
                .splitType(request.getSplitType())
                .build();

        List<ExpenseSplit> splits = createSplits(expense, groupId, request);
        splits.forEach(expense::addSplit);

        Expense savedExpense = expenseRepository.save(expense);

        log.info("Expense created successfully with ID: {}", savedExpense.getId());
        return expenseMapper.toResponse(savedExpense);
    }

    /**
     * Creates splits using the Strategy Pattern.
     * 
     * The factory determines which strategy to use based on split type,
     * then the strategy handles the actual split calculation.
     * 
     * Benefits:
     * - Adding PERCENTAGE split? Just create PercentageSplitStrategy, register in factory
     * - No changes needed in ExpenseService (Open/Closed Principle)
     */
    private List<ExpenseSplit> createSplits(Expense expense, UUID groupId, CreateExpenseRequest request) {
        SplitStrategy strategy = splitStrategyFactory.getStrategy(request.getSplitType());
        return strategy.calculateSplits(expense, groupId, request);
    }

    /**
     * Calculates net balance for each user in a group.
     * 
     * Logic:
     * - Positive balance = user should receive money
     * - Negative balance = user owes money
     * 
     * For each expense:
     * - Payer gets +totalAmount (they paid for everyone)
     * - Each participant gets -amountOwed (what they owe)
     */
    public BalanceResponse calculateBalances(UUID groupId) {
        log.info("Calculating balances for group: {}", groupId);

        groupService.findGroupByIdOrThrow(groupId);

        List<Expense> expenses = expenseRepository.findByGroupIdWithSplits(groupId);
        Map<UUID, BigDecimal> balances = new HashMap<>();
        Map<UUID, String> userNames = new HashMap<>();

        for (Expense expense : expenses) {
            UUID payerId = expense.getPaidBy().getId();
            userNames.putIfAbsent(payerId, expense.getPaidBy().getName());

            balances.merge(payerId, expense.getTotalAmount(), BigDecimal::add);

            for (ExpenseSplit split : expense.getSplits()) {
                UUID userId = split.getUser().getId();
                userNames.putIfAbsent(userId, split.getUser().getName());

                balances.merge(userId, split.getAmountOwed().negate(), BigDecimal::add);
            }
        }

        List<BalanceResponse.UserBalance> userBalances = balances.entrySet().stream()
                .map(entry -> {
                    BigDecimal balance = entry.getValue().setScale(MONEY_SCALE, ROUNDING_MODE);
                    BalanceResponse.BalanceStatus status = determineBalanceStatus(balance);

                    return BalanceResponse.UserBalance.builder()
                            .userId(entry.getKey())
                            .userName(userNames.get(entry.getKey()))
                            .balance(balance)
                            .status(status)
                            .build();
                })
                .sorted(Comparator.comparing(BalanceResponse.UserBalance::getBalance).reversed())
                .collect(Collectors.toList());

        log.debug("Calculated balances for {} users in group {}", userBalances.size(), groupId);

        return BalanceResponse.builder()
                .groupId(groupId)
                .balances(userBalances)
                .build();
    }

    private BalanceResponse.BalanceStatus determineBalanceStatus(BigDecimal balance) {
        int comparison = balance.compareTo(BigDecimal.ZERO);
        if (comparison > 0) {
            return BalanceResponse.BalanceStatus.GETS_BACK;
        } else if (comparison < 0) {
            return BalanceResponse.BalanceStatus.OWES_MONEY;
        }
        return BalanceResponse.BalanceStatus.SETTLED;
    }

    public SettlementResponse calculateSettlements(UUID groupId) {
        log.info("Calculating optimized settlements for group: {}", groupId);

        BalanceResponse balanceResponse = calculateBalances(groupId);

        Map<UUID, BigDecimal> netBalances = balanceResponse.getBalances().stream()
                .collect(Collectors.toMap(
                        BalanceResponse.UserBalance::getUserId,
                        BalanceResponse.UserBalance::getBalance
                ));

        Map<UUID, String> userNames = balanceResponse.getBalances().stream()
                .collect(Collectors.toMap(
                        BalanceResponse.UserBalance::getUserId,
                        BalanceResponse.UserBalance::getUserName
                ));

        List<SettlementResponse.Settlement> settlements = 
                settlementService.calculateOptimizedSettlements(netBalances, userNames);

        log.info("Generated {} optimized settlements for group {}", settlements.size(), groupId);

        return SettlementResponse.builder()
                .groupId(groupId)
                .settlements(settlements)
                .totalTransactions(settlements.size())
                .build();
    }
}
