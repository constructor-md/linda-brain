package com.awesome.lindabrain.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka配置类
 * 配置Kafka生产者、消费者和主题
 */
@Configuration
public class KafkaConfig {

    // Kafka服务器地址
    @Value("${spring.kafka.bootstrap-servers:192.168.1.22:9094}")
    private String bootstrapServers;

    // 消费者组ID
    @Value("${spring.kafka.consumer.group-id:linda-websocket-group}")
    private String groupId;

    /**
     * WebSocket消息主题名称
     */
    public static final String WEBSOCKET_MESSAGE_TOPIC = "linda-websocket-message";

    /**
     * WebSocket连接管理主题名称
     */
    public static final String WEBSOCKET_CONNECTION_TOPIC = "linda-websocket-connection";

    /**
     * 创建WebSocket消息主题
     */
    @Bean
    public NewTopic websocketMessageTopic() {
        // 创建一个分区数为3、副本因子为1的主题
        return TopicBuilder.name(WEBSOCKET_MESSAGE_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * 创建WebSocket连接管理主题
     */
    @Bean
    public NewTopic websocketConnectionTopic() {
        // 创建一个分区数为3、副本因子为1的主题
        return TopicBuilder.name(WEBSOCKET_CONNECTION_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * 配置生产者工厂
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        // 设置Kafka服务器地址
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // 设置键和值的序列化器
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * 创建KafkaTemplate用于发送消息
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * 配置消费者工厂
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        // 设置Kafka服务器地址
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // 设置消费者组ID
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        // 设置键和值的反序列化器
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // 设置自动提交偏移量
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * 创建Kafka监听器容器工厂
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}