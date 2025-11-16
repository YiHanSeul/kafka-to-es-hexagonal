package com.example.kafkaes.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import java.io.File

/**
 * Kafka 설정 클래스
 */
@Configuration
class KafkaConfig {

    private val configFile = File("src/main/resources/kafka.yml")

    private val yamlMapper = ObjectMapper(YAMLFactory()).apply {
        findAndRegisterModules()
        propertyNamingStrategy = com.fasterxml.jackson.databind.PropertyNamingStrategies.KEBAB_CASE
    }

    private val config: KafkaConfigData = loadConfig()

    private fun loadConfig(): KafkaConfigData {
        return yamlMapper.readValue(configFile)
    }

    val bootstrapServers: String
        get() = config.kafka.bootstrapServers

    val consumerTopic: String
        get() = config.kafka.consumer.topic

    val consumerGroupId: String
        get() = config.kafka.consumer.groupId

    val pollTimeoutMs: Long
        get() = config.kafka.consumer.pollTimeoutMs
    @Bean
    fun consumerFactory(): ConsumerFactory<String, String> {
        val props = mutableMapOf<String, Any>(
            org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to config.kafka.bootstrapServers,
            org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG to config.kafka.consumer.groupId,
            org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to config.kafka.consumer.keyDeserializer,
            org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to config.kafka.consumer.valueDeserializer,
            org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to config.kafka.consumer.autoOffsetReset,
            org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to config.kafka.consumer.enableAutoCommit,
            org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG to config.kafka.consumer.maxPollRecords,
            org.apache.kafka.clients.consumer.ConsumerConfig.FETCH_MIN_BYTES_CONFIG to config.kafka.consumer.fetchMinBytes,
            org.apache.kafka.clients.consumer.ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG to config.kafka.consumer.fetchMaxWaitMs,
            org.apache.kafka.clients.consumer.ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG to config.kafka.consumer.sessionTimeoutMs,
            org.apache.kafka.clients.consumer.ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG to config.kafka.consumer.heartbeatIntervalMs
        )

        return DefaultKafkaConsumerFactory(props)
    }
    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = consumerFactory()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        return factory
    }

    fun printConfig() {
        println("""
            ===== Kafka Configuration =====
            Bootstrap Servers: $bootstrapServers
            Topic: $consumerTopic
            Group ID: $consumerGroupId
            Poll Timeout: ${pollTimeoutMs}ms
            Auto Offset Reset: ${config.kafka.consumer.autoOffsetReset}
            ==============================
        """.trimIndent())
    }
}

data class KafkaConfigData(
    val kafka: KafkaSettings
)

data class KafkaSettings(
    val bootstrapServers: String,
    val consumer: ConsumerSettings,
    val producer: ProducerSettings
)

data class ConsumerSettings(
    val topic: String,
    val groupId: String,
    val autoOffsetReset: String,
    val enableAutoCommit: Boolean,
    val pollTimeoutMs: Long,
    val maxPollRecords: Int,
    val fetchMinBytes: Int,
    val fetchMaxWaitMs: Int,
    val sessionTimeoutMs: Int,
    val heartbeatIntervalMs: Int,
    val keyDeserializer: String,
    val valueDeserializer: String
)

data class ProducerSettings(
    val acks: String,
    val retries: Int,
    val batchSize: Int,
    val lingerMs: Int,
    val bufferMemory: Long
)