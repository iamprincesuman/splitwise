package com.split.splitwise.dto.request;

import com.split.splitwise.entity.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateExpenseRequest {

    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Total amount must be greater than 0")
    @Digits(integer = 17, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal totalAmount;

    @NotNull(message = "Payer user ID is required")
    private UUID paidBy;

    @NotNull(message = "Split type is required")
    private SplitType splitType;

    @Valid
    private List<SplitDetail> splits;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SplitDetail {

        @NotNull(message = "User ID is required in split")
        private UUID userId;

        @NotNull(message = "Amount is required in split")
        @DecimalMin(value = "0.00", inclusive = true, message = "Split amount cannot be negative")
        @Digits(integer = 17, fraction = 2, message = "Amount must have at most 2 decimal places")
        private BigDecimal amount;
    }
}
