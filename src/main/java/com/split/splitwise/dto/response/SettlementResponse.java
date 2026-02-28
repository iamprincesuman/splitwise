package com.split.splitwise.dto.response;

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
public class SettlementResponse {

    private UUID groupId;
    private List<Settlement> settlements;
    private int totalTransactions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Settlement {
        private UUID fromUserId;
        private String fromUserName;
        private UUID toUserId;
        private String toUserName;
        private BigDecimal amount;
    }
}
