package com.split.splitwise.service;

import com.split.splitwise.dto.request.AddMemberRequest;
import com.split.splitwise.dto.request.CreateGroupRequest;
import com.split.splitwise.dto.response.GroupResponse;
import com.split.splitwise.entity.Group;
import com.split.splitwise.entity.GroupMember;
import com.split.splitwise.entity.User;
import com.split.splitwise.exception.BusinessRuleException;
import com.split.splitwise.exception.DuplicateResourceException;
import com.split.splitwise.exception.ResourceNotFoundException;
import com.split.splitwise.mapper.GroupMapper;
import com.split.splitwise.repository.GroupMemberRepository;
import com.split.splitwise.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserService userService;
    private final GroupMapper groupMapper;

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request) {
        log.info("Creating group: {} by user: {}", request.getName(), request.getCreatedBy());

        User creator = userService.findUserByIdOrThrow(request.getCreatedBy());

        Group group = Group.builder()
                .name(request.getName())
                .createdBy(creator)
                .build();

        Group savedGroup = groupRepository.save(group);

        GroupMember creatorMember = GroupMember.builder()
                .group(savedGroup)
                .user(creator)
                .build();
        savedGroup.addMember(creatorMember);

        groupMemberRepository.save(creatorMember);

        log.info("Group created successfully with ID: {}", savedGroup.getId());

        return groupRepository.findByIdWithMembers(savedGroup.getId())
                .map(groupMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", savedGroup.getId()));
    }

    @Transactional
    public GroupResponse addMember(UUID groupId, AddMemberRequest request) {
        log.info("Adding user {} to group {}", request.getUserId(), groupId);

        Group group = findGroupByIdOrThrow(groupId);
        User user = userService.findUserByIdOrThrow(request.getUserId());

        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, request.getUserId())) {
            throw new DuplicateResourceException(
                    String.format("User %s is already a member of group %s", request.getUserId(), groupId));
        }

        GroupMember member = GroupMember.builder()
                .group(group)
                .user(user)
                .build();

        groupMemberRepository.save(member);

        log.info("User {} added to group {} successfully", request.getUserId(), groupId);

        return groupRepository.findByIdWithMembers(groupId)
                .map(groupMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));
    }

    public GroupResponse getGroupById(UUID groupId) {
        log.debug("Fetching group with ID: {}", groupId);

        return groupRepository.findByIdWithMembers(groupId)
                .map(groupMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));
    }

    public Group findGroupByIdOrThrow(UUID groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));
    }

    public void validateUserIsMember(UUID groupId, UUID userId) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new BusinessRuleException(
                    String.format("User %s is not a member of group %s", userId, groupId));
        }
    }

    public Set<UUID> getGroupMemberIds(UUID groupId) {
        return groupMemberRepository.findUserIdsByGroupId(groupId);
    }

    public long getMemberCount(UUID groupId) {
        return groupMemberRepository.countByGroupId(groupId);
    }
}
