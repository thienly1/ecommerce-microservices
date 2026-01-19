package com.ecommerce.user_service.service;

import com.ecommerce.user_service.client.OrderServiceClient;
import com.ecommerce.user_service.dto.UserRequest;
import com.ecommerce.user_service.dto.UserResponse;
import com.ecommerce.user_service.entity.User;
import com.ecommerce.user_service.event.UserEvent;
import com.ecommerce.user_service.exception.UserNotFoundException;
import com.ecommerce.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final KafkaProducerService kafkaProducerService;
    private final OrderServiceClient orderServiceClient;

    public UserResponse createUser(UserRequest request) {
        log.info("Creating user with email: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(request.getPassword()) // In production, hash this!
                .phone(request.getPhone())
                .address(request.getAddress())
                .status(User.UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created with id: {}", savedUser.getId());
        // Publish event to Kafka
        UserEvent event = UserEvent.builder()
                .userId(savedUser.getId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .address(savedUser.getAddress())
                .status(savedUser.getStatus().name())
                .createdAt(savedUser.getCreatedAt())
                .eventType("USER_CREATED")
                .message("New user registered: " + savedUser.getEmail())
                .eventTimestamp(LocalDateTime.now())
                .build();

        kafkaProducerService.sendUserEvent(event);

        return UserResponse.fromEntity(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.info("Fetching user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        return UserResponse.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users");

        return userRepository.findAll()
                .stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    public UserResponse updateUser(Long id, UserRequest request) {
        log.info("Updating user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        // Check if new email is already taken by another user
        if (!user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully");
        UserEvent event = UserEvent.builder()
                .userId(updatedUser.getId())
                .firstName(updatedUser.getFirstName())
                .lastName(updatedUser.getLastName())
                .email(updatedUser.getEmail())
                .phone(updatedUser.getPhone())
                .address(updatedUser.getAddress())
                .status(updatedUser.getStatus().name())
                .createdAt(updatedUser.getCreatedAt())
                .eventType("USER_UPDATED")
                .message("User profile updated: " + updatedUser.getEmail())
                .eventTimestamp(LocalDateTime.now())
                .build();
        kafkaProducerService.sendUserEvent(event);

        return UserResponse.fromEntity(updatedUser);
    }

    public void deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (orderServiceClient.hasActiveOrders(id)) {
            throw new IllegalStateException(
                    "Cannot delete user with ID " + id +
                            ". User has active orders (PENDING, CONFIRMED, PROCESSING, or SHIPPED). " +
                            "Please wait until all orders are DELIVERED or CANCELLED.");
        }

        //send the event to kafka with minimal fields
        UserEvent event = UserEvent.builder()
                .userId(user.getId())
                .email(user.getEmail())  // For logging purposes
                .eventType("USER_DELETED")
                .message("User account deleted: " + user.getEmail())
                .eventTimestamp(LocalDateTime.now())
                .build();

        kafkaProducerService.sendUserEvent(event);
        userRepository.deleteById(id);
        log.info("User deleted successfully");
    }
    @Transactional(readOnly = true)
    public boolean userExists(Long id) {
        return userRepository.existsById(id);
    }
}