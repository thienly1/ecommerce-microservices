package com.ecommerce.order_service.client;

import com.ecommerce.order_service.dto.external.UserResponse;
import com.ecommerce.order_service.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class UserServiceClient {

    private final WebClient webClient;

    public UserServiceClient(@Qualifier("userWebClient") WebClient webClient) {  // Changed here
        this.webClient = webClient;
    }

    public UserResponse getUserById(Long userId) {
        log.info("Fetching user with id: {} from user-service", userId);

        return webClient.get()
                .uri("/api/users/{id}", userId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        Mono.error(new ServiceException("User not found: " + userId)))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new ServiceException("User service is unavailable")))
                .bodyToMono(UserResponse.class)
                .block();
    }

    public boolean userExists(Long userId) {
        log.info("Checking if user exists: {}", userId);

        try {
            Boolean exists = webClient.get()
                    .uri("/api/users/{id}/exists", userId)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Error checking user existence: {}", e.getMessage());
            return false;
        }
    }
}