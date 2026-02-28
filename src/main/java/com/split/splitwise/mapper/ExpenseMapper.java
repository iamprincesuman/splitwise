package com.split.splitwise.mapper;

import com.split.splitwise.dto.response.ExpenseResponse;
import com.split.splitwise.entity.Expense;
import com.split.splitwise.entity.ExpenseSplit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ExpenseMapper {

    @Mapping(target = "paidBy", source = "paidBy.id")
    @Mapping(target = "paidByName", source = "paidBy.name")
    @Mapping(target = "groupId", source = "group.id")
    @Mapping(target = "splits", source = "splits")
    ExpenseResponse toResponse(Expense expense);

    default ExpenseResponse.SplitResponse toSplitResponse(ExpenseSplit split) {
        if (split == null) {
            return null;
        }
        return ExpenseResponse.SplitResponse.builder()
                .id(split.getId())
                .userId(split.getUser().getId())
                .userName(split.getUser().getName())
                .amountOwed(split.getAmountOwed())
                .build();
    }

    default List<ExpenseResponse.SplitResponse> toSplitResponseList(List<ExpenseSplit> splits) {
        if (splits == null) {
            return null;
        }
        return splits.stream()
                .map(this::toSplitResponse)
                .toList();
    }
}
