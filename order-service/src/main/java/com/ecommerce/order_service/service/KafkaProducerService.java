package com.ecommerce.order_service.service;

import com.ecommerce.order_service.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private static final String ORDER_TOPIC = "order-events";

    public void sendOrderEvent(OrderEvent event) {
        log.info("Sending ORDER event to Kafka: {}", event);
        kafkaTemplate.send(ORDER_TOPIC, event.getOrderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Order event sent successfully - Order ID: {}", event.getOrderId());
                    } else {
                        log.error("Failed to send order event", ex);
                    }
                });
    }
}