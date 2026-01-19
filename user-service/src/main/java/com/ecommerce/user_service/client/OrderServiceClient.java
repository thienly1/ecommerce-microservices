package com.ecommerce.user_service.client;

import com.ecommerce.user_service.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class OrderServiceClient {

    private final WebClient webClient;

    public OrderServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("http://ORDER-SERVICE")
                .build();
    }

    @CircuitBreaker(name = "orderService", fallbackMethod = "hasActiveOrdersFallback")
    @Retry(name = "orderService")
    public boolean hasActiveOrders(Long userId) {
        log.info("Checking if user {} has active orders via Order Service", userId);

        Boolean hasActive = webClient.get()
                .uri("/api/orders/user/{userId}/has-active", userId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        Mono.error(new ServiceUnavailableException("Failed to check active orders for user: " + userId)))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new ServiceUnavailableException("Order Service is unavailable")))
                .bodyToMono(Boolean.class)
                .block();

        log.info("User {} has active orders: {}", userId, hasActive);
        return Boolean.TRUE.equals(hasActive);
    }

    // FALLBACK METHOD
    public boolean hasActiveOrdersFallback(Long userId, Throwable throwable) {
        log.error("CIRCUIT BREAKER OPEN: Cannot check active orders for user {}. Error: {}",
                userId, throwable.getMessage());
        log.warn("FALLBACK: Assuming user has active orders to prevent accidental deletion");

        // Fail-safe: assume user has active orders
        // This prevents accidental deletion when Order Service is down
        return true;
    }
}