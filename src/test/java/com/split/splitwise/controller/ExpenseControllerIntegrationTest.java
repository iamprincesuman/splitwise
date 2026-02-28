package com.split.splitwise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.split.splitwise.dto.request.AddMemberRequest;
import com.split.splitwise.dto.request.CreateExpenseRequest;
import com.split.splitwise.dto.request.CreateGroupRequest;
import com.split.splitwise.dto.request.CreateUserRequest;
import com.split.splitwise.dto.response.ApiResponse;
import com.split.splitwise.dto.response.GroupResponse;
import com.split.splitwise.dto.response.UserResponse;
import com.split.splitwise.entity.SplitType;
import com.split.splitwise.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ExpenseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ExpenseSplitRepository expenseSplitRepository;

    private UUID aliceId;
    private UUID bobId;
    private UUID charlieId;
    private UUID groupId;

    @BeforeEach
    void setUp() throws Exception {
        expenseSplitRepository.deleteAll();
        expenseRepository.deleteAll();
        groupMemberRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();

        aliceId = createUser("Alice", "alice@test.com");
        bobId = createUser("Bob", "bob@test.com");
        charlieId = createUser("Charlie", "charlie@test.com");

        groupId = createGroup("Trip", aliceId);
        addMemberToGroup(groupId, bobId);
        addMemberToGroup(groupId, charlieId);
    }

    @Test
    @DisplayName("Should create expense with equal split")
    void shouldCreateExpenseWithEqualSplit() throws Exception {
        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .description("Dinner")
                .totalAmount(new BigDecimal("90.00"))
                .paidBy(aliceId)
                .splitType(SplitType.EQUAL)
                .build();

        mockMvc.perform(post("/api/v1/groups/{groupId}/expenses", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.description").value("Dinner"))
                .andExpect(jsonPath("$.data.totalAmount").value(90.00))
                .andExpect(jsonPath("$.data.splits", hasSize(3)));
    }

    @Test
    @DisplayName("Should create expense with exact split")
    void shouldCreateExpenseWithExactSplit() throws Exception {
        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .description("Custom expense")
                .totalAmount(new BigDecimal("100.00"))
                .paidBy(aliceId)
                .splitType(SplitType.EXACT)
                .splits(List.of(
                        CreateExpenseRequest.SplitDetail.builder()
                                .userId(aliceId).amount(new BigDecimal("50.00")).build(),
                        CreateExpenseRequest.SplitDetail.builder()
                                .userId(bobId).amount(new BigDecimal("30.00")).build(),
                        CreateExpenseRequest.SplitDetail.builder()
                                .userId(charlieId).amount(new BigDecimal("20.00")).build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/groups/{groupId}/expenses", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.splits", hasSize(3)));
    }

    @Test
    @DisplayName("Should reject expense when payer is not group member")
    void shouldRejectExpenseWhenPayerNotGroupMember() throws Exception {
        UUID outsiderId = createUser("Outsider", "outsider@test.com");

        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .description("Dinner")
                .totalAmount(new BigDecimal("90.00"))
                .paidBy(outsiderId)
                .splitType(SplitType.EQUAL)
                .build();

        mockMvc.perform(post("/api/v1/groups/{groupId}/expenses", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    @DisplayName("Should calculate balances correctly")
    void shouldCalculateBalancesCorrectly() throws Exception {
        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .description("Dinner")
                .totalAmount(new BigDecimal("90.00"))
                .paidBy(aliceId)
                .splitType(SplitType.EQUAL)
                .build();

        mockMvc.perform(post("/api/v1/groups/{groupId}/expenses", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/groups/{groupId}/balances", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.balances", hasSize(3)))
                .andExpect(jsonPath("$.data.balances[?(@.userName=='Alice')].balance").value(60.00))
                .andExpect(jsonPath("$.data.balances[?(@.userName=='Bob')].balance").value(-30.00))
                .andExpect(jsonPath("$.data.balances[?(@.userName=='Charlie')].balance").value(-30.00));
    }

    @Test
    @DisplayName("Should calculate settlements correctly")
    void shouldCalculateSettlementsCorrectly() throws Exception {
        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .description("Dinner")
                .totalAmount(new BigDecimal("90.00"))
                .paidBy(aliceId)
                .splitType(SplitType.EQUAL)
                .build();

        mockMvc.perform(post("/api/v1/groups/{groupId}/expenses", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/groups/{groupId}/settlements", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settlements", hasSize(2)))
                .andExpect(jsonPath("$.data.totalTransactions").value(2));
    }

    @Test
    @DisplayName("Should reject exact split when amounts don't match total")
    void shouldRejectExactSplitWhenAmountsDontMatchTotal() throws Exception {
        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .description("Custom expense")
                .totalAmount(new BigDecimal("100.00"))
                .paidBy(aliceId)
                .splitType(SplitType.EXACT)
                .splits(List.of(
                        CreateExpenseRequest.SplitDetail.builder()
                                .userId(aliceId).amount(new BigDecimal("40.00")).build(),
                        CreateExpenseRequest.SplitDetail.builder()
                                .userId(bobId).amount(new BigDecimal("30.00")).build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/groups/{groupId}/expenses", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("do not equal total amount")));
    }

    private UUID createUser(String name, String email) throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .name(name).email(email).build();

        MvcResult result = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), ApiResponse.class);
        return UUID.fromString(((java.util.Map<?, ?>) response.getData()).get("id").toString());
    }

    private UUID createGroup(String name, UUID createdBy) throws Exception {
        CreateGroupRequest request = CreateGroupRequest.builder()
                .name(name).createdBy(createdBy).build();

        MvcResult result = mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), ApiResponse.class);
        return UUID.fromString(((java.util.Map<?, ?>) response.getData()).get("id").toString());
    }

    private void addMemberToGroup(UUID groupId, UUID userId) throws Exception {
        AddMemberRequest request = AddMemberRequest.builder()
                .userId(userId).build();

        mockMvc.perform(post("/api/v1/groups/{groupId}/members", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
