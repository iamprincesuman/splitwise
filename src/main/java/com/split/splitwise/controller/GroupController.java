package com.split.splitwise.controller;

import com.split.splitwise.dto.request.AddMemberRequest;
import com.split.splitwise.dto.request.CreateGroupRequest;
import com.split.splitwise.dto.response.ApiResponse;
import com.split.splitwise.dto.response.GroupResponse;
import com.split.splitwise.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Groups", description = "Group management APIs")
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    @Operation(summary = "Create a new group", description = "Creates a group and adds creator as first member")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Group created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Creator user not found")
    })
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(
            @Valid @RequestBody CreateGroupRequest request) {

        log.info("REST request to create group: {}", request.getName());
        GroupResponse group = groupService.createGroup(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Group created successfully", group));
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Add member to group", description = "Adds an existing user to the group")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Member added"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Group or user not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "User already a member")
    })
    public ResponseEntity<ApiResponse<GroupResponse>> addMember(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request) {

        log.info("REST request to add member {} to group {}", request.getUserId(), id);
        GroupResponse group = groupService.addMember(id, request);
        return ResponseEntity.ok(ApiResponse.success("Member added successfully", group));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get group details", description = "Retrieves group with all members")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Group found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Group not found")
    })
    public ResponseEntity<ApiResponse<GroupResponse>> getGroupById(
            @Parameter(description = "Group UUID") @PathVariable UUID id) {

        log.info("REST request to get group: {}", id);
        GroupResponse group = groupService.getGroupById(id);
        return ResponseEntity.ok(ApiResponse.success(group));
    }
}
