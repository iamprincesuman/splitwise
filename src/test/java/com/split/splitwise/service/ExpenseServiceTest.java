package com.split.splitwise.service;

import com.split.splitwise.dto.request.CreateExpenseRequest;
import com.split.splitwise.dto.response.BalanceResponse;
import com.split.splitwise.dto.response.ExpenseResponse;
import com.split.splitwise.entity.*;
import com.split.splitwise.exception.BusinessRuleException;
import com.split.splitwise.exception.ValidationException;
import com.split.splitwise.mapper.ExpenseMapper;
import com.split.splitwise.repository.ExpenseRepository;
import com.split.splitwise.repository.GroupMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupService groupService;

    @Mock
    private UserService userService;

    @Mock
    private ExpenseMapper expenseMapper;

    @Mock
    private SettlementService settlementService;

    @InjectMocks
    private ExpenseService expenseService;

    private UUID groupId;
    private UUID user1Id;
    private UUID user2Id;
    private UUID user3Id;
    private Group group;
    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        user1Id = UUID.randomUUID();
        user2Id = UUID.randomUUID();
        user3Id = UUID.randomUUID();

        user1 = User.builder().id(user1Id).name("Alice").email("alice@test.com").build();
        user2 = User.builder().id(user2Id).name("Bob").email("bob@test.com").build();
        user3 = User.builder().id(user3Id).name("Charlie").email("charlie@test.com").build();

        group = Group.builder().id(groupId).name("Test Group").createdBy(user1).build();
    }

    @Nested
    @DisplayName("Equal Split Tests")
    class EqualSplitTests {

        @Test
        @DisplayName("Should split equally among 3 members")
        void shouldSplitEquallyAmongThreeMembers() {
            BigDecimal totalAmount = new BigDecimal("100.00");
            CreateExpenseRequest request = CreateExpenseRequest.builder()
                    .description("Dinner")
                    .totalAmount(totalAmount)
                    .paidBy(user1Id)
                    .splitType(SplitType.EQUAL)
                    .build();

            List<GroupMember> members = List.of(
                    GroupMember.builder().user(user1).build(),
                    GroupMember.builder().user(user2).build(),
                    GroupMember.builder().user(user3).build()
            );

            when(groupService.findGroupByIdOrThrow(groupId)).thenReturn(group);
            when(userService.findUserByIdOrThrow(user1Id)).thenReturn(user1);
            doNothing().when(groupService).validateUserIsMember(groupId, user1Id);
            when(groupMemberRepository.findByGroupIdWithUser(groupId)).thenReturn(members);
            when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> {
                Expense e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });
            when(expenseMapper.toResponse(any())).thenReturn(ExpenseResponse.builder().build());

            expenseService.createExpense(groupId, request);

            ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
            verify(expenseRepository).save(expenseCaptor.capture());

            Expense savedExpense = expenseCaptor.getValue();
            List<ExpenseSplit> splits = savedExpense.getSplits();

            assertThat(splits).hasSize(3);

            BigDecimal totalSplit = splits.stream()
                    .map(ExpenseSplit::getAmountOwed)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(totalSplit).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("Should handle rounding correctly for uneven split")
        void shouldHandleRoundingCorrectlyForUnevenSplit() {
            BigDecimal totalAmount = new BigDecimal("100.00");
            CreateExpenseRequest request = CreateExpenseRequest.builder()
                    .description("Dinner")
                    .totalAmount(totalAmount)
                    .paidBy(user1Id)
                    .splitType(SplitType.EQUAL)
                    .build();

            List<GroupMember> members = List.of(
                    GroupMember.builder().user(user1).build(),
                    GroupMember.builder().user(user2).build(),
                    GroupMember.builder().user(user3).build()
            );

            when(groupService.findGroupByIdOrThrow(groupId)).thenReturn(group);
            when(userService.findUserByIdOrThrow(user1Id)).thenReturn(user1);
            doNothing().when(groupService).validateUserIsMember(groupId, user1Id);
            when(groupMemberRepository.findByGroupIdWithUser(groupId)).thenReturn(members);
            when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));
            when(expenseMapper.toResponse(any())).thenReturn(ExpenseResponse.builder().build());

            expenseService.createExpense(groupId, request);

            ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
            verify(expenseRepository).save(expenseCaptor.capture());

            List<ExpenseSplit> splits = expenseCaptor.getValue().getSplits();

            long regularSplits = splits.stream()
                    .filter(s -> s.getAmountOwed().compareTo(new BigDecimal("33.33")) == 0)
                    .count();
            long adjustedSplits = splits.stream()
                    .filter(s -> s.getAmountOwed().compareTo(new BigDecimal("33.34")) == 0)
                    .count();

            assertThat(regularSplits).isEqualTo(2);
            assertThat(adjustedSplits).isEqualTo(1);
        }

        @Test
        @DisplayName("Should throw exception for empty group")
        void shouldThrowExceptionForEmptyGroup() {
            CreateExpenseRequest request = CreateExpenseRequest.builder()
                    .description("Dinner")
                    .totalAmount(new BigDecimal("100.00"))
                    .paidBy(user1Id)
                    .splitType(SplitType.EQUAL)
                    .build();

            when(groupService.findGroupByIdOrThrow(groupId)).thenReturn(group);
            when(userService.findUserByIdOrThrow(user1Id)).thenReturn(user1);
            doNothing().when(groupService).validateUserIsMember(groupId, user1Id);
            when(groupMemberRepository.findByGroupIdWithUser(groupId)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> expenseService.createExpense(groupId, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("no members");
        }
    }

    @Nested
    @DisplayName("Exact Split Tests")
    class ExactSplitTests {

        @Test
        @DisplayName("Should create exact splits when amounts match total")
        void shouldCreateExactSplitsWhenAmountsMatchTotal() {
            CreateExpenseRequest request = CreateExpenseRequest.builder()
                    .description("Custom split")
                    .totalAmount(new BigDecimal("100.00"))
                    .paidBy(user1Id)
                    .splitType(SplitType.EXACT)
                    .splits(List.of(
                            CreateExpenseRequest.SplitDetail.builder()
                                    .userId(user1Id).amount(new BigDecimal("50.00")).build(),
                            CreateExpenseRequest.SplitDetail.builder()
                                    .userId(user2Id).amount(new BigDecimal("30.00")).build(),
                            CreateExpenseRequest.SplitDetail.builder()
                                    .userId(user3Id).amount(new BigDecimal("20.00")).build()
                    ))
                    .build();

            Set<UUID> memberIds = Set.of(user1Id, user2Id, user3Id);

            when(groupService.findGroupByIdOrThrow(groupId)).thenReturn(group);
            when(userService.findUserByIdOrThrow(user1Id)).thenReturn(user1);
            when(userService.findUserByIdOrThrow(user2Id)).thenReturn(user2);
            when(userService.findUserByIdOrThrow(user3Id)).thenReturn(user3);
            doNothing().when(groupService).validateUserIsMember(groupId, user1Id);
            when(groupService.getGroupMemberIds(groupId)).thenReturn(memberIds);
            when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));
            when(expenseMapper.toResponse(any())).thenReturn(ExpenseResponse.builder().build());

            expenseService.createExpense(groupId, request);

            ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
            verify(expenseRepository).save(expenseCaptor.capture());

            List<ExpenseSplit> splits = expenseCaptor.getValue().getSplits();
            assertThat(splits).hasSize(3);

            BigDecimal totalSplit = splits.stream()
                    .map(ExpenseSplit::getAmountOwed)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(totalSplit).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("Should reject exact split when amounts don't match total")
        void shouldRejectExactSplitWhenAmountsDontMatchTotal() {
            CreateExpenseRequest request = CreateExpenseRequest.builder()
                    .description("Custom split")
                    .totalAmount(new BigDecimal("100.00"))
                    .paidBy(user1Id)
                    .splitType(SplitType.EXACT)
                    .splits(List.of(
                            CreateExpenseRequest.SplitDetail.builder()
                                    .userId(user1Id).amount(new BigDecimal("50.00")).build(),
                            CreateExpenseRequest.SplitDetail.builder()
                                    .userId(user2Id).amount(new BigDecimal("30.00")).build()
                    ))
                    .build();

            Set<UUID> memberIds = Set.of(user1Id, user2Id);

            when(groupService.findGroupByIdOrThrow(groupId)).thenReturn(group);
            when(userService.findUserByIdOrThrow(user1Id)).thenReturn(user1);
            when(userService.findUserByIdOrThrow(user2Id)).thenReturn(user2);
            doNothing().when(groupService).validateUserIsMember(groupId, user1Id);
            when(groupService.getGroupMemberIds(groupId)).thenReturn(memberIds);

            assertThatThrownBy(() -> expenseService.createExpense(groupId, request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("do not equal total amount");
        }

        @Test
        @DisplayName("Should reject split for non-member user")
        void shouldRejectSplitForNonMemberUser() {
            UUID nonMemberId = UUID.randomUUID();

            CreateExpenseRequest request = CreateExpenseRequest.builder()
                    .description("Custom split")
                    .totalAmount(new BigDecimal("100.00"))
                    .paidBy(user1Id)
                    .splitType(SplitType.EXACT)
                    .splits(List.of(
                            CreateExpenseRequest.SplitDetail.builder()
                                    .userId(nonMemberId).amount(new BigDecimal("100.00")).build()
                    ))
                    .build();

            Set<UUID> memberIds = Set.of(user1Id, user2Id);

            when(groupService.findGroupByIdOrThrow(groupId)).thenReturn(group);
            when(userService.findUserByIdOrThrow(user1Id)).thenReturn(user1);
            doNothing().when(groupService).validateUserIsMember(groupId, user1Id);
            when(groupService.getGroupMemberIds(groupId)).thenReturn(memberIds);

            assertThatThrownBy(() -> expenseService.createExpense(groupId, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("not a member");
        }

        @Test
        @DisplayName("Should throw validation exception when splits list is empty")
        void shouldThrowValidationExceptionWhenSplitsListIsEmpty() {
            CreateExpenseRequest request = CreateExpenseRequest.builder()
                    .description("Custom split")
                    .totalAmount(new BigDecimal("100.00"))
                    .paidBy(user1Id)
                    .splitType(SplitType.EXACT)
                    .splits(Collections.emptyList())
                    .build();

            when(groupService.findGroupByIdOrThrow(groupId)).thenReturn(group);
            when(userService.findUserByIdOrThrow(user1Id)).thenReturn(user1);
            doNothing().when(groupService).validateUserIsMember(groupId, user1Id);

            assertThatThrownBy(() -> expenseService.createExpense(groupId, request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("requires split details");
        }
    }

    @Nested
    @DisplayName("Balance Calculation Tests")
    class BalanceCalculationTests {

        @Test
        @DisplayName("Should calculate correct balances for single expense")
        void shouldCalculateCorrectBalancesForSingleExpense() {
            Expense expense = Expense.builder()
                    .id(UUID.randomUUID())
                    .totalAmount(new BigDecimal("90.00"))
                    .paidBy(user1)
                    .group(group)
                    .splitType(SplitType.EQUAL)
                    .splits(new ArrayList<>())
                    .build();

            ExpenseSplit split1 = ExpenseSplit.builder()
                    .user(user1).amountOwed(new BigDecimal("30.00")).build();
            ExpenseSplit split2 = ExpenseSplit.builder()
                    .user(user2).amountOwed(new BigDecimal("30.00")).build();
            ExpenseSplit split3 = ExpenseSplit.builder()
                    .user(user3).amountOwed(new BigDecimal("30.00")).build();
            expense.getSplits().addAll(List.of(split1, split2, split3));

            when(groupService.findGroupByIdOrThrow(groupId)).thenReturn(group);
            when(expenseRepository.findByGroupIdWithSplits(groupId)).thenReturn(List.of(expense));

            BalanceResponse response = expenseService.calculateBalances(groupId);

            assertThat(response.getGroupId()).isEqualTo(groupId);
            assertThat(response.getBalances()).hasSize(3);

            Map<UUID, BigDecimal> balanceMap = new HashMap<>();
            response.getBalances().forEach(b -> balanceMap.put(b.getUserId(), b.getBalance()));

            assertThat(balanceMap.get(user1Id)).isEqualByComparingTo("60.00");
            assertThat(balanceMap.get(user2Id)).isEqualByComparingTo("-30.00");
            assertThat(balanceMap.get(user3Id)).isEqualByComparingTo("-30.00");
        }

        @Test
        @DisplayName("Should calculate correct balances for multiple expenses")
        void shouldCalculateCorrectBalancesForMultipleExpenses() {
            Expense expense1 = Expense.builder()
                    .id(UUID.randomUUID())
                    .totalAmount(new BigDecimal("60.00"))
                    .paidBy(user1)
                    .group(group)
                    .splitType(SplitType.EQUAL)
                    .splits(new ArrayList<>())
                    .build();
            expense1.getSplits().addAll(List.of(
                    ExpenseSplit.builder().user(user1).amountOwed(new BigDecimal("20.00")).build(),
                    ExpenseSplit.builder().user(user2).amountOwed(new BigDecimal("20.00")).build(),
                    ExpenseSplit.builder().user(user3).amountOwed(new BigDecimal("20.00")).build()
            ));

            Expense expense2 = Expense.builder()
                    .id(UUID.randomUUID())
                    .totalAmount(new BigDecimal("30.00"))
                    .paidBy(user2)
                    .group(group)
                    .splitType(SplitType.EQUAL)
                    .splits(new ArrayList<>())
                    .build();
            expense2.getSplits().addAll(List.of(
                    ExpenseSplit.builder().user(user1).amountOwed(new BigDecimal("10.00")).build(),
                    ExpenseSplit.builder().user(user2).amountOwed(new BigDecimal("10.00")).build(),
                    ExpenseSplit.builder().user(user3).amountOwed(new BigDecimal("10.00")).build()
            ));

            when(groupService.findGroupByIdOrThrow(groupId)).thenReturn(group);
            when(expenseRepository.findByGroupIdWithSplits(groupId))
                    .thenReturn(List.of(expense1, expense2));

            BalanceResponse response = expenseService.calculateBalances(groupId);

            Map<UUID, BigDecimal> balanceMap = new HashMap<>();
            response.getBalances().forEach(b -> balanceMap.put(b.getUserId(), b.getBalance()));

            assertThat(balanceMap.get(user1Id)).isEqualByComparingTo("30.00");
            assertThat(balanceMap.get(user2Id)).isEqualByComparingTo("0.00");
            assertThat(balanceMap.get(user3Id)).isEqualByComparingTo("-30.00");
        }

        @Test
        @DisplayName("Should return empty balances for group with no expenses")
        void shouldReturnEmptyBalancesForGroupWithNoExpenses() {
            when(groupService.findGroupByIdOrThrow(groupId)).thenReturn(group);
            when(expenseRepository.findByGroupIdWithSplits(groupId)).thenReturn(Collections.emptyList());

            BalanceResponse response = expenseService.calculateBalances(groupId);

            assertThat(response.getGroupId()).isEqualTo(groupId);
            assertThat(response.getBalances()).isEmpty();
        }
    }
}
