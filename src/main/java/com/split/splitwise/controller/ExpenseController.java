package com.split.splitwise.controller;

import com.split.splitwise.dto.request.CreateExpenseRequest;
import com.split.splitwise.dto.response.ApiResponse;
import com.split.splitwise.dto.response.BalanceResponse;
import com.split.splitwise.dto.response.ExpenseResponse;
import com.split.splitwise.dto.response.SettlementResponse;
import com.split.splitwise.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/groups/{groupId}")
@RequiredArgsConstructor
@Tag(name = "Expenses", description = "Expense management and settlement APIs")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping("/expenses")
    @Operation(summary = "Create an expense", 
               description = "Creates an expense with EQUAL or EXACT split among group members")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Expense created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or split mismatch"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Group not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Payer not a group member")
    })
    public ResponseEntity<ApiResponse<ExpenseResponse>> createExpense(
            @Parameter(description = "Group UUID") @PathVariable UUID groupId,
            @Valid @RequestBody CreateExpenseRequest request) {

        log.info("REST request to create expense in group {}: {}", groupId, request.getDescription());
        ExpenseResponse expense = expenseService.createExpense(groupId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Expense created successfully", expense));
    }

    @GetMapping("/balances")
    @Operation(summary = "Get group balances", 
               description = "Calculates net balance for each member. Positive = gets money, Negative = owes money")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalances(
            @Parameter(description = "Group UUID") @PathVariable UUID groupId) {

        log.info("REST request to get balances for group: {}", groupId);
        BalanceResponse balances = expenseService.calculateBalances(groupId);
        return ResponseEntity.ok(ApiResponse.success(balances));
    }

    @GetMapping("/settlements")
    @Operation(summary = "Get optimized settlements", 
               description = "Returns minimum transactions needed to settle all debts using O(n log n) algorithm")
    public ResponseEntity<ApiResponse<SettlementResponse>> getSettlements(
            @Parameter(description = "Group UUID") @PathVariable UUID groupId) {

        log.info("REST request to get settlements for group: {}", groupId);
        SettlementResponse settlements = expenseService.calculateSettlements(groupId);
        return ResponseEntity.ok(ApiResponse.success(settlements));
    }
}
