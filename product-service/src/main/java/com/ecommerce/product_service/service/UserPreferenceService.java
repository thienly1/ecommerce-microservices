package com.ecommerce.product_service.service;

import com.ecommerce.product_service.entity.UserPreference;
import com.ecommerce.product_service.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserPreferenceService {

    private final UserPreferenceRepository userPreferenceRepository;

    public UserPreference createUserPreference(Long userId, String userName, String email) {
        log.info("Creating user preference for user ID: {}", userId);

        // Check if already exists
        if (userPreferenceRepository.existsByUserId(userId)) {
            log.warn("User preference already exists for user ID: {}", userId);
            return userPreferenceRepository.findByUserId(userId).orElseThrow();
        }

        UserPreference preference = UserPreference.builder()
                .userId(userId)
                .userName(userName)
                .email(email)
                .favoriteCategories(new ArrayList<>())
                .wishlist(new ArrayList<>())
                .notificationsEnabled(true)
                .build();

        UserPreference saved = userPreferenceRepository.save(preference);
        log.info("User preference created successfully for user ID: {}", userId);

        return saved;
    }

    public void deleteUserPreference(Long userId) {
        log.info("Deleting user preference for user ID: {}", userId);

        if (!userPreferenceRepository.existsByUserId(userId)) {
            log.warn("User preference not found for user ID: {}", userId);
            return;
        }

        userPreferenceRepository.deleteByUserId(userId);
        log.info("User preference deleted successfully for user ID: {}", userId);
    }
}
