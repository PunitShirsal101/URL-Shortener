package com.shortscale.analytics;

import com.shortscale.api.dto.AnalyticsEvent;
import org.junit.jupiter.api.Test;

public class KafkaAnalyticsConsumerTest {

    @Test
    public void shouldConsumeAnalyticsEvent() {
        KafkaAnalyticsConsumer consumer = new KafkaAnalyticsConsumer();
        AnalyticsEvent event = new AnalyticsEvent();
        event.setShortCode("abc");
        event.setTimestamp(java.time.LocalDateTime.now());
        // Just call it, since it logs
        consumer.consumeAnalyticsEvent(event);
    }
}
