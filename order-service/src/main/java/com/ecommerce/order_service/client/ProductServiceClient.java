package com.ecommerce.order_service.client;

import com.ecommerce.order_service.dto.external.ProductResponse;
import com.ecommerce.order_service.exception.ServiceException;
import com.ecommerce.order_service.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Slf4j
public class ProductServiceClient {

    private final WebClient webClient;

    public ProductServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("http://PRODUCT-SERVICE")
                .build();
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "getProductByIdFallback")
    @Retry(name = "productService")
    public ProductResponse getProductById(Long productId) {
        log.info("Fetching product with id: {} from product-service", productId);

        return webClient.get()
                .uri("/api/products/{id}", productId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        Mono.error(new ServiceException("Product not found: " + productId)))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new ServiceException("Product service is unavailable")))
                .bodyToMono(ProductResponse.class)
                .block();
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "isInStockFallback")
    @Retry(name = "productService")
    public boolean isInStock(Long productId, int quantity) {
        log.info("Checking stock for product: {}, quantity: {}", productId, quantity);

        Boolean inStock = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/products/{id}/in-stock")
                        .queryParam("quantity", quantity)
                        .build(productId))
                .retrieve()
                .bodyToMono(Boolean.class)
                .block();
        return Boolean.TRUE.equals(inStock);
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "reduceStockFallback")
    @Retry(name = "productService")
    public void reduceStock(Long productId, int quantity) {
        log.info("Reducing stock for product: {}, quantity: {}", productId, quantity);

        webClient.put()
                .uri("/api/products/{id}/reduce-stock", productId)
                .bodyValue(Map.of("quantity", quantity))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        Mono.error(new ServiceException("Failed to reduce stock")))
                .bodyToMono(Void.class)
                .block();
    }

    // FALLBACK METHODS
    // Throw ServiceUnavailableException instead of returning default values

    public ProductResponse getProductByIdFallback(Long productId, Throwable throwable) {
        log.error("CIRCUIT BREAKER OPEN: Cannot get product {}. Error: {}",
                productId, throwable.getMessage());
        throw new ServiceUnavailableException("Product Service is unavailable. Please try again later.");
    }

    public boolean isInStockFallback(Long productId, int quantity, Throwable throwable) {
        log.error("CIRCUIT BREAKER OPEN: Cannot check stock for product {}. Error: {}",
                productId, throwable.getMessage());
        throw new ServiceUnavailableException("Product Service is unavailable. Please try again later.");
    }

    public void reduceStockFallback(Long productId, int quantity, Throwable throwable) {
        log.error("CIRCUIT BREAKER OPEN: Cannot reduce stock for product {}. Error: {}",
                productId, throwable.getMessage());
        throw new ServiceUnavailableException("Product Service is unavailable. Please try again later.");
    }
}