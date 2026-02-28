package com.split.splitwise.service;

import com.split.splitwise.dto.request.CreateUserRequest;
import com.split.splitwise.dto.response.UserResponse;
import com.split.splitwise.entity.User;
import com.split.splitwise.exception.DuplicateResourceException;
import com.split.splitwise.exception.ResourceNotFoundException;
import com.split.splitwise.mapper.UserMapper;
import com.split.splitwise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        User user = userMapper.toEntity(request);
        User savedUser = userRepository.save(user);

        log.info("User created successfully with ID: {}", savedUser.getId());
        return userMapper.toResponse(savedUser);
    }

    public UserResponse getUserById(UUID userId) {
        log.debug("Fetching user with ID: {}", userId);

        User user = findUserByIdOrThrow(userId);
        return userMapper.toResponse(user);
    }

    public List<UserResponse> getAllUsers() {
        log.debug("Fetching all users");

        List<User> users = userRepository.findAll();
        return userMapper.toResponseList(users);
    }

    public User findUserByIdOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}
