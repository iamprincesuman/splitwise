package com.split.splitwise.repository;

import com.split.splitwise.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    @Query("SELECT DISTINCT e FROM Expense e " +
            "JOIN FETCH e.paidBy " +
            "JOIN FETCH e.splits s " +
            "JOIN FETCH s.user " +
            "WHERE e.group.id = :groupId")
    List<Expense> findByGroupIdWithSplits(@Param("groupId") UUID groupId);

    List<Expense> findByGroupId(UUID groupId);
}
