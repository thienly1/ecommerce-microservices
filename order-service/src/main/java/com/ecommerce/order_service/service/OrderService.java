package com.ecommerce.order_service.service;

import com.ecommerce.order_service.client.ProductServiceClient;
import com.ecommerce.order_service.client.UserServiceClient;
import com.ecommerce.order_service.dto.OrderItemRequest;
import com.ecommerce.order_service.dto.OrderRequest;
import com.ecommerce.order_service.dto.OrderResponse;
import com.ecommerce.order_service.dto.external.ProductResponse;
import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.entity.OrderItem;
import com.ecommerce.order_service.event.OrderEvent;
import com.ecommerce.order_service.exception.OrderNotFoundException;
import com.ecommerce.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;
    private final KafkaProducerService kafkaProducerService;

    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());

        // 1. Validate user exists
        if (!userServiceClient.userExists(request.getUserId())) {
            throw new IllegalArgumentException("User not found: " + request.getUserId());
        }

        // 2. Validate products and check stock
        for (OrderItemRequest item : request.getItems()) {
            if (!productServiceClient.isInStock(item.getProductId(), item.getQuantity())) {
                throw new IllegalArgumentException(
                        "Product not in stock: " + item.getProductId());
            }
        }

        // 3. Create order
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .userId(request.getUserId())
                .shippingAddress(request.getShippingAddress())
                .status(Order.OrderStatus.PENDING)
                .build();

        // 4. Add order items and calculate total
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            ProductResponse product = productServiceClient.getProductById(itemRequest.getProductId());

            BigDecimal subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .subtotal(subtotal)
                    .build();

            order.addItem(orderItem);
            totalAmount = totalAmount.add(subtotal);
        }

        order.setTotalAmount(totalAmount);

        // 5. Save order
        Order savedOrder = orderRepository.save(order);
        log.info("Order created with number: {}", savedOrder.getOrderNumber());

        // Publish event to Kafka
        OrderEvent event = OrderEvent.builder()
                .orderId(savedOrder.getId())
                .orderNumber(savedOrder.getOrderNumber())
                .userId(savedOrder.getUserId())
                .totalAmount(savedOrder.getTotalAmount())
                .status(savedOrder.getStatus().name())
                .shippingAddress(savedOrder.getShippingAddress())
                .createdAt(savedOrder.getCreatedAt())
                .items(savedOrder.getItems().stream()
                        .map(item -> OrderEvent.OrderItemDTO.builder()
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .subtotal(item.getSubtotal())
                                .build())
                        .collect(Collectors.toList()))
                .eventType("ORDER_CREATED")
                .message("New order created with number: " + savedOrder.getOrderNumber())
                .eventTimestamp(LocalDateTime.now())
                .build();

        kafkaProducerService.sendOrderEvent(event);

        // 6. Reduce stock by webclient(send direct to product service
        //for (OrderItemRequest item : request.getItems()) {
        //    productServiceClient.reduceStock(item.getProductId(), item.getQuantity());
        //}

        // 7. Update order status to CONFIRMED
        savedOrder.setStatus(Order.OrderStatus.CONFIRMED);
        savedOrder = orderRepository.save(savedOrder);

        return OrderResponse.fromEntity(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        log.info("Fetching order with id: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        return OrderResponse.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        log.info("Fetching order with number: {}", orderNumber);

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found with number: " + orderNumber));

        return OrderResponse.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        log.info("Fetching orders for user: {}", userId);

        return orderRepository.findByUserId(userId)
                .stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        log.info("Fetching all orders");

        return orderRepository.findAll()
                .stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    public OrderResponse updateOrderStatus(Long id, Order.OrderStatus status) {
        log.info("Updating order {} status to {}", id, status);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);

        return OrderResponse.fromEntity(updatedOrder);
    }

    public OrderResponse cancelOrder(Long id) {
        log.info("Cancelling order: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getStatus() == Order.OrderStatus.SHIPPED ||
                order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new IllegalArgumentException(
                    "Cannot cancel order that has been shipped or delivered");
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(order);
        //Kafka public event to restore product stock.
        OrderEvent event = OrderEvent.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .items(order.getItems().stream()
                        .map(item -> OrderEvent.OrderItemDTO.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .eventType("ORDER_CANCELLED")
                .message("Order cancelled with order number: " + cancelledOrder.getOrderNumber())
                .eventTimestamp(LocalDateTime.now())
                .build();
        kafkaProducerService.sendOrderEvent(event);
        return OrderResponse.fromEntity(cancelledOrder);
    }
    @Transactional(readOnly = true)
    public boolean hasActiveOrders(Long userId) {
        List<Order> activeOrders = orderRepository.findByUserId(userId).stream()
                .filter(order -> order.getStatus() != Order.OrderStatus.CANCELLED
                        && order.getStatus() != Order.OrderStatus.DELIVERED)
                .toList();
        return !activeOrders.isEmpty();
    }

    @Transactional(readOnly = true)
    public boolean hasActiveOrdersForProduct(Long productId) {
        return orderRepository.findAll().stream()
                .filter(order -> order.getStatus() != Order.OrderStatus.CANCELLED
                        && order.getStatus() != Order.OrderStatus.DELIVERED)
                .anyMatch(order -> order.getItems()
                        .stream().anyMatch(item -> item.getProductId().equals(productId)));

    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
