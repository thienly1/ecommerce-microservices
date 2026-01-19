package com.ecommerce.product_service.consumer;

import com.ecommerce.product_service.service.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final UserPreferenceService userPreferenceService;

    @KafkaListener(topics = "user-events", groupId = "product-service-group")
    public void consumeUserEvent(Map<String, Object> event) {
        log.info("============================================");
        log.info("RECEIVED USER EVENT IN PRODUCT SERVICE");
        log.info("============================================");

        String eventType = (String) event.get("eventType");
        Long userId = ((Number) event.get("userId")).longValue();
        String firstName = (String) event.get("firstName");
        String lastName = (String) event.get("lastName");
        String email = (String) event.get("email");
        String status = (String) event.get("status");

        log.info("Event Type: {}", eventType);
        log.info("User ID: {}", userId);
        log.info("Name: {} {}", firstName, lastName);
        log.info("Email: {}", email);
        log.info("Status: {}", status);

        try {
            switch (eventType) {
                case "USER_CREATED":
                    handleUserCreated(userId, firstName, lastName, email);
                    break;
                case "USER_DELETED":
                    handleUserDeleted(userId);
                    break;

                default:
                    log.warn("Unknown event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing user event: ", e);
        }

        log.info("============================================");
    }

    private void handleUserCreated(Long userId, String firstName, String lastName, String email) {
        log.info("Processing USER_CREATED");
        log.info("Action: Initialize product recommendations and preferences");

        String userName = firstName + " " + lastName;
        userPreferenceService.createUserPreference(userId, userName, email);

        log.info("User preference profile created successfully");
        log.info("  - User can now save products to wishlist");
        log.info("  - User can mark favorite categories");
        log.info("  - Notifications enabled by default");
    }

    private void handleUserDeleted(Long userId) {
        log.info("Processing USER_DELETED");
        log.info("Action: Clean up user data and preferences");

        userPreferenceService.deleteUserPreference(userId);

        log.info("User data cleaned up successfully");
        log.info("  - Wishlist removed");
        log.info("  - Favorite categories cleared");
        log.info("  - User preferences deleted");
    }
}