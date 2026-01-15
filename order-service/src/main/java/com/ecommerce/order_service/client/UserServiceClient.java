package com.ecommerce.order_service.client;

import com.ecommerce.order_service.dto.external.UserResponse;
import com.ecommerce.order_service.exception.ServiceException;
import com.ecommerce.order_service.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class UserServiceClient {

    private final WebClient webClient;

    public UserServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("http://USER-SERVICE")
                .build();
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByIdFallback")
    @Retry(name = "userService")
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

    @CircuitBreaker(name = "userService", fallbackMethod = "userExistsFallback")
    @Retry(name = "userService")
    public boolean userExists(Long userId) {
        log.info("Checking if user exists: {}", userId);

        Boolean exists = webClient.get()
                .uri("/api/users/{id}/exists", userId)
                .retrieve()
                .bodyToMono(Boolean.class)
                .block();
        return Boolean.TRUE.equals(exists);
    }

    public UserResponse getUserByIdFallback(Long userId, Throwable throwable) {
        log.error("CIRCUIT BREAKER OPEN: Cannot get user {}. Error: {}",
                userId, throwable.getMessage());
        throw new ServiceUnavailableException("User Service is unavailable. Please try again later.");
    }

    public boolean userExistsFallback(Long userId, Throwable throwable) {
        log.error("CIRCUIT BREAKER OPEN: Cannot verify user {}. Error: {}",
                userId, throwable.getMessage());
        throw new ServiceUnavailableException("User Service is unavailable. Please try again later.");
    }
}