package com.split.splitwise.repository;

import com.split.splitwise.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);

    @Query("SELECT gm.user.id FROM GroupMember gm WHERE gm.group.id = :groupId")
    Set<UUID> findUserIdsByGroupId(@Param("groupId") UUID groupId);

    @Query("SELECT gm FROM GroupMember gm " +
            "JOIN FETCH gm.user " +
            "WHERE gm.group.id = :groupId")
    List<GroupMember> findByGroupIdWithUser(@Param("groupId") UUID groupId);

    long countByGroupId(UUID groupId);
}
