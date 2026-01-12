package com.ecommerce.order_service.client;

import com.ecommerce.order_service.dto.external.ProductResponse;
import com.ecommerce.order_service.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Slf4j
public class ProductServiceClient {

    private final WebClient webClient;

    public ProductServiceClient(@Qualifier("productWebClient") WebClient webClient) {  // Changed here
        this.webClient = webClient;
    }

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

    public boolean isInStock(Long productId, int quantity) {
        log.info("Checking stock for product: {}, quantity: {}", productId, quantity);

        try {
            Boolean inStock = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/products/{id}/in-stock")
                            .queryParam("quantity", quantity)
                            .build(productId))
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();
            return Boolean.TRUE.equals(inStock);
        } catch (Exception e) {
            log.error("Error checking stock: {}", e.getMessage());
            return false;
        }
    }

    public void reduceStock(Long productId, int quantity) {
        log.info("Reducing stock for product: {}, quantity: {}", productId, quantity);

        webClient.put()
                .uri("/api/products/{id}/reduce-stock", productId)
                .bodyValue(Map.of("quantity", quantity))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        Mono.error(new ServiceException("Failed to reduce stock for product: " + productId)))
                .bodyToMono(Void.class)
                .block();
    }
}