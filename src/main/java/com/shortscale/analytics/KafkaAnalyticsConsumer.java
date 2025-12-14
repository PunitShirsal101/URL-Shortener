package com.shortscale.analytics;

import com.shortscale.api.dto.AnalyticsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaAnalyticsConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaAnalyticsConsumer.class);

    @KafkaListener(topics = "url-analytics", groupId = "url-shortener-analytics")
    public void consumeAnalyticsEvent(AnalyticsEvent event) {
        logger.info("Consumed analytics event: {}", event);
        // Here, you could store in DB, send to another system, etc.
    }
}
