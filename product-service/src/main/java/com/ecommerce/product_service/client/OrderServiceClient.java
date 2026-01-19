package com.ecommerce.product_service.client;

import com.ecommerce.product_service.exception.ServiceUnavailableException;
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

    @CircuitBreaker(name = "orderService", fallbackMethod = "hasActiveOrdersForProductFallback")
    @Retry(name = "orderService")
    public boolean hasActiveOrdersForProduct(Long productId) {
        log.info("Checking if product {} is in active orders via Order Service", productId);

        Boolean hasActive = webClient.get()
                .uri("/api/orders/product/{productId}/has-active", productId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        Mono.error(new ServiceUnavailableException("Failed to check active orders for product: " + productId)))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new ServiceUnavailableException("Order Service is unavailable")))
                .bodyToMono(Boolean.class)
                .block();

        log.info("Product {} is in active orders: {}", productId, hasActive);
        return Boolean.TRUE.equals(hasActive);
    }

    // FALLBACK METHOD
    public boolean hasActiveOrdersForProductFallback(Long productId, Throwable throwable) {
        log.error("CIRCUIT BREAKER OPEN: Cannot check active orders for product {}. Error: {}",
                productId, throwable.getMessage());
        log.warn("FALLBACK: Assuming product has active orders to prevent accidental deletion");

        // Fail-safe: assume product is in active orders
        // This prevents accidental deletion when Order Service is down
        return true;
    }
}