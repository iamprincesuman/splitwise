package com.split.splitwise.controller;

import com.split.splitwise.dto.request.CreateExpenseRequest;
import com.split.splitwise.dto.response.ApiResponse;
import com.split.splitwise.dto.response.BalanceResponse;
import com.split.splitwise.dto.response.ExpenseResponse;
import com.split.splitwise.dto.response.SettlementResponse;
import com.split.splitwise.service.ExpenseService;
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
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping("/expenses")
    public ResponseEntity<ApiResponse<ExpenseResponse>> createExpense(
            @PathVariable UUID groupId,
            @Valid @RequestBody CreateExpenseRequest request) {

        log.info("REST request to create expense in group {}: {}", groupId, request.getDescription());
        ExpenseResponse expense = expenseService.createExpense(groupId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Expense created successfully", expense));
    }

    @GetMapping("/balances")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalances(@PathVariable UUID groupId) {
        log.info("REST request to get balances for group: {}", groupId);
        BalanceResponse balances = expenseService.calculateBalances(groupId);
        return ResponseEntity.ok(ApiResponse.success(balances));
    }

    @GetMapping("/settlements")
    public ResponseEntity<ApiResponse<SettlementResponse>> getSettlements(@PathVariable UUID groupId) {
        log.info("REST request to get settlements for group: {}", groupId);
        SettlementResponse settlements = expenseService.calculateSettlements(groupId);
        return ResponseEntity.ok(ApiResponse.success(settlements));
    }
}
