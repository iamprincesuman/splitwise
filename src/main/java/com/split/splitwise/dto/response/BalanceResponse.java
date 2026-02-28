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
public class BalanceResponse {

    private UUID groupId;
    private List<UserBalance> balances;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserBalance {
        private UUID userId;
        private String userName;
        private BigDecimal balance;
        private BalanceStatus status;
    }

    public enum BalanceStatus {
        OWES_MONEY,
        GETS_BACK,
        SETTLED
    }
}
