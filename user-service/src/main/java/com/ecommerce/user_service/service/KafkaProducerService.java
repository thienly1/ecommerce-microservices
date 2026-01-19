package com.ecommerce.user_service.service;

import com.ecommerce.user_service.event.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, UserEvent> userEventKafkaTemplate;
    private static final String USER_TOPIC = "user-events";

    public void sendUserEvent(UserEvent event) {
        log.info("Sending USER event to Kafka: {}", event);
        userEventKafkaTemplate.send(USER_TOPIC, event.getUserId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("User event sent successfully - User ID: {}", event.getUserId());
                    } else {
                        log.error("Failed to send user event", ex);
                    }
                });
    }
}
