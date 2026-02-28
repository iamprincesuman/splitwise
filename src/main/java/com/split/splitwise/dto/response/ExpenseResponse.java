package com.split.splitwise.dto.response;

import com.split.splitwise.entity.SplitType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseResponse {

    private UUID id;
    private String description;
    private BigDecimal totalAmount;
    private UUID paidBy;
    private String paidByName;
    private UUID groupId;
    private SplitType splitType;
    private LocalDateTime createdAt;
    private List<SplitResponse> splits;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SplitResponse {
        private UUID id;
        private UUID userId;
        private String userName;
        private BigDecimal amountOwed;
    }
}
