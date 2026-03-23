package com.shopsphere.orderservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic orderPlacedTopic() {
        return TopicBuilder.name("order.placed")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCancelledTopic() {
        return TopicBuilder.name("order.cancelled")
                .partitions(1)
                .replicas(1)
                .build();
    }
    @Bean
    public NewTopic orderFailedTopic() {
        return TopicBuilder.name("order.failed")
                .partitions(1)
                .replicas(1)
                .build();
    }
}