package com.split.splitwise.service.split;

import com.split.splitwise.dto.request.CreateExpenseRequest;
import com.split.splitwise.entity.Expense;
import com.split.splitwise.entity.ExpenseSplit;
import com.split.splitwise.entity.GroupMember;
import com.split.splitwise.exception.BusinessRuleException;
import com.split.splitwise.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Concrete Strategy: Splits expense equally among all group members.
 * 
 * Handles rounding by assigning remainder to the last member.
 * Example: $100 / 3 = $33.33, $33.33, $33.34
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EqualSplitStrategy implements SplitStrategy {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final GroupMemberRepository groupMemberRepository;

    @Override
    public List<ExpenseSplit> calculateSplits(Expense expense, UUID groupId, CreateExpenseRequest request) {
        List<GroupMember> members = groupMemberRepository.findByGroupIdWithUser(groupId);

        if (members.isEmpty()) {
            throw new BusinessRuleException("Cannot split expense: group has no members");
        }

        int memberCount = members.size();
        BigDecimal totalAmount = expense.getTotalAmount();

        BigDecimal perPersonAmount = totalAmount
                .divide(BigDecimal.valueOf(memberCount), MONEY_SCALE, ROUNDING_MODE);

        BigDecimal allocatedAmount = perPersonAmount.multiply(BigDecimal.valueOf(memberCount - 1));
        BigDecimal lastPersonAmount = totalAmount.subtract(allocatedAmount);

        List<ExpenseSplit> splits = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            GroupMember member = members.get(i);
            BigDecimal amount = (i == members.size() - 1) ? lastPersonAmount : perPersonAmount;

            ExpenseSplit split = ExpenseSplit.builder()
                    .user(member.getUser())
                    .amountOwed(amount)
                    .build();
            splits.add(split);
        }

        log.debug("EqualSplitStrategy: Created {} splits for expense", splits.size());
        return splits;
    }
}
