package com.split.splitwise.controller;

import com.split.splitwise.dto.request.AddMemberRequest;
import com.split.splitwise.dto.request.CreateGroupRequest;
import com.split.splitwise.dto.response.ApiResponse;
import com.split.splitwise.dto.response.GroupResponse;
import com.split.splitwise.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(
            @Valid @RequestBody CreateGroupRequest request) {

        log.info("REST request to create group: {}", request.getName());
        GroupResponse group = groupService.createGroup(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Group created successfully", group));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<GroupResponse>> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request) {

        log.info("REST request to add member {} to group {}", request.getUserId(), id);
        GroupResponse group = groupService.addMember(id, request);
        return ResponseEntity.ok(ApiResponse.success("Member added successfully", group));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GroupResponse>> getGroupById(@PathVariable UUID id) {
        log.info("REST request to get group: {}", id);
        GroupResponse group = groupService.getGroupById(id);
        return ResponseEntity.ok(ApiResponse.success(group));
    }
}
