package com.split.splitwise.service.split;

import com.split.splitwise.dto.request.CreateExpenseRequest;
import com.split.splitwise.entity.Expense;
import com.split.splitwise.entity.ExpenseSplit;
import com.split.splitwise.entity.User;
import com.split.splitwise.exception.BusinessRuleException;
import com.split.splitwise.exception.ValidationException;
import com.split.splitwise.repository.GroupMemberRepository;
import com.split.splitwise.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Concrete Strategy: Splits expense based on exact amounts specified by user.
 * 
 * Validates:
 * - All specified users are group members
 * - Sum of splits equals total amount
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExactSplitStrategy implements SplitStrategy {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final GroupMemberRepository groupMemberRepository;
    private final UserService userService;

    @Override
    public List<ExpenseSplit> calculateSplits(Expense expense, UUID groupId, CreateExpenseRequest request) {
        if (request.getSplits() == null || request.getSplits().isEmpty()) {
            throw new ValidationException("Exact split requires split details");
        }

        Set<UUID> memberIds = groupMemberRepository.findUserIdsByGroupId(groupId);
        BigDecimal totalSplitAmount = BigDecimal.ZERO;
        List<ExpenseSplit> splits = new ArrayList<>();

        for (CreateExpenseRequest.SplitDetail splitDetail : request.getSplits()) {
            if (!memberIds.contains(splitDetail.getUserId())) {
                throw new BusinessRuleException(
                        String.format("User %s is not a member of the group", splitDetail.getUserId()));
            }

            User user = userService.findUserByIdOrThrow(splitDetail.getUserId());
            BigDecimal amount = splitDetail.getAmount().setScale(MONEY_SCALE, ROUNDING_MODE);
            totalSplitAmount = totalSplitAmount.add(amount);

            ExpenseSplit split = ExpenseSplit.builder()
                    .user(user)
                    .amountOwed(amount)
                    .build();
            splits.add(split);
        }

        if (totalSplitAmount.compareTo(expense.getTotalAmount()) != 0) {
            throw new ValidationException(
                    String.format("Split amounts (%s) do not equal total amount (%s)",
                            totalSplitAmount, expense.getTotalAmount()));
        }

        log.debug("ExactSplitStrategy: Created {} splits for expense", splits.size());
        return splits;
    }
}
