package com.ecommerce.product_service.consumer;

import com.ecommerce.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final ProductService productService;

    @KafkaListener(topics = "order-events", groupId = "product-service-group")
    public void consumeOrderEvent(Map<String, Object> event) {
        log.info("-----------------------------------------");
        log.info("RECEIVED ORDER EVENT IN PRODUCT SERVICE");
        log.info("-------------------------------------------");

        String eventType = (String) event.get("eventType");
        Object orderId = event.get("orderId");
        String orderNumber = (String) event.get("orderNumber");
        Object userId = event.get("userId");
        Object totalAmount = event.get("totalAmount");
        String status = (String) event.get("status");
        List<Map<String, Object>> items = (List<Map<String, Object>>) event.get("items");

        log.info("Event Type: {}", eventType);
        log.info("Order ID: {}", orderId);
        log.info("Order Number: {}", orderNumber);
        log.info("User ID: {}", userId);
        log.info("Total Amount: {}", totalAmount);
        log.info("Status: {}", status);
        log.info("Number of Items: {}", items != null ? items.size() : 0);

        try {
            switch (eventType) {
                case "ORDER_CREATED":
                    log.info("Processing ORDER_CREATED");
                    handleOrderCreated(items);
                    break;

                case "ORDER_CANCELLED":
                    log.info("Processing ORDER_CANCELLED");
                    handleOrderCancelled(items);
                    break;

                case "ORDER_SHIPPED":
                    log.info("Processing ORDER_SHIPPED");
                    log.info("Action: Track shipment (no inventory changes needed)");
                    break;

                default:
                    log.warn("Unknown event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing order event: ", e);
        }

        log.info("---------------------------------------------------");
    }

    private void handleOrderCreated(List<Map<String, Object>> items) {
        log.info("Reducing inventory for {} products", items.size());

        for (Map<String, Object> item : items) {
            try {
                Long productId = ((Number) item.get("productId")).longValue();
                Integer quantity = (Integer) item.get("quantity");
                String productName = (String) item.get("productName");

                log.info("Product: {} (ID: {}), Reducing by: {}", productName, productId, quantity);

                productService.reduceStock(productId, quantity);

                log.info("Stock reduced successfully for product {}", productId);
            } catch (Exception e) {
                log.error("Failed to reduce stock for product: {}", item.get("productId"), e);
            }
        }

        log.info("Inventory reduction completed");
    }

    private void handleOrderCancelled(List<Map<String, Object>> items) {
        log.info("Restoring inventory for {} products", items.size());

        for (Map<String, Object> item : items) {
            try {
                Long productId = ((Number) item.get("productId")).longValue();
                Integer quantity = (Integer) item.get("quantity");
                String productName = (String) item.get("productName");

                log.info("Product: {} (ID: {}), Restoring: {}", productName, productId, quantity);

                productService.restoreStock(productId, quantity);

                log.info("Stock restored successfully for product {}", productId);
            } catch (Exception e) {
                log.error("Failed to restore stock for product: {}", item.get("productId"), e);
            }
        }

        log.info("Inventory restoration completed");
    }
}