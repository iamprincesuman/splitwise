package com.split.splitwise.mapper;

import com.split.splitwise.dto.response.GroupResponse;
import com.split.splitwise.entity.Group;
import com.split.splitwise.entity.GroupMember;
import com.split.splitwise.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GroupMapper {

    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "members", source = "members")
    GroupResponse toResponse(Group group);

    default GroupResponse.UserSummary toUserSummary(User user) {
        if (user == null) {
            return null;
        }
        return GroupResponse.UserSummary.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    default GroupResponse.MemberResponse toMemberResponse(GroupMember member) {
        if (member == null) {
            return null;
        }
        return GroupResponse.MemberResponse.builder()
                .id(member.getId())
                .userId(member.getUser().getId())
                .userName(member.getUser().getName())
                .userEmail(member.getUser().getEmail())
                .joinedAt(member.getJoinedAt())
                .build();
    }

    default List<GroupResponse.MemberResponse> toMemberResponseList(List<GroupMember> members) {
        if (members == null) {
            return null;
        }
        return members.stream()
                .map(this::toMemberResponse)
                .toList();
    }
}
