package com.split.splitwise.repository;

import com.split.splitwise.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

    @Query("SELECT g FROM Group g " +
            "LEFT JOIN FETCH g.members m " +
            "LEFT JOIN FETCH m.user " +
            "LEFT JOIN FETCH g.createdBy " +
            "WHERE g.id = :groupId")
    Optional<Group> findByIdWithMembers(@Param("groupId") UUID groupId);
}
