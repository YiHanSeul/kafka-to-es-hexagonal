package com.example.kafkaes

import com.example.kafkaes.adapter.inbound.kafka.KafkaConsumerAdapter
import com.example.kafkaes.adapter.outbound.elasticsearch.ElasticsearchAdapter
import com.example.kafkaes.config.ElasticsearchConfig
import com.example.kafkaes.config.KafkaConfig
import com.example.kafkaes.domain.service.MessageProcessingService
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import kotlin.system.exitProcess

/**
 * 메인 애플리케이션
 *
 * 역할:
 * 1. 설정 로드
 * 2. Adapter 생성
 * 3. Domain Service 생성
 * 4. 애플리케이션 시작
 * 5. Graceful Shutdown
 *
 * 헥사고날 아키텍처 조립:
 *
 *   Kafka (외부)
 *       ↓
 *   KafkaConsumerAdapter (Inbound)
 *       ↓ MessageConsumer 인터페이스
 *   MessageProcessingService (Domain)
 *       ↓ MessageIndexer 인터페이스
 *   ElasticsearchAdapter (Outbound)
 *       ↓
 *   Elasticsearch (외부)
 */
@SpringBootApplication
class Application {

    private val logger = LoggerFactory.getLogger(javaClass)
    /**
     * Kafka 설정 Bean
     */
    @Bean
    fun kafkaConfig(): KafkaConfig {
        logger.info("📋 Kafka 설정 로드 중...")
        val config = KafkaConfig()
        config.printConfig()
        return config
    }

    /**
     * Elasticsearch 설정 Bean
     */
    @Bean
    fun elasticsearchConfig(): ElasticsearchConfig {
        logger.info("📋 Elasticsearch 설정 로드 중...")
        val config = ElasticsearchConfig()
        config.printConfig()
        return config
    }

    /**
     * Elasticsearch Adapter Bean
     */
    @Bean
    fun elasticsearchAdapter(esConfig: ElasticsearchConfig): ElasticsearchAdapter {
        logger.info("📊 Elasticsearch Adapter 생성 중...")
        return ElasticsearchAdapter(esConfig)
    }

    /**
     * Message Processing Service Bean
     */
    @Bean
    fun messageProcessingService(
        kafkaConsumerAdapter: KafkaConsumerAdapter,
        elasticsearchAdapter: ElasticsearchAdapter
    ): MessageProcessingService {
        logger.info("🎯 Message Processing Service 생성 중...")
        return MessageProcessingService(
            messageConsumer = kafkaConsumerAdapter,
            messageIndexer = elasticsearchAdapter
        )
    }

    /**
     * 애플리케이션 시작 시 실행
     */
    @Bean
    fun runner(messageProcessingService: MessageProcessingService): CommandLineRunner {
        return CommandLineRunner {
            logger.info("=".repeat(50))
            logger.info("🚀 Kafka to Elasticsearch - Spring Boot")
            logger.info("=".repeat(50))

            try {
                // 서비스 시작 (코루틴)
                runBlocking {
                    messageProcessingService.start()
                }
            } catch (e: Exception) {
                logger.error("❌ 애플리케이션 실행 실패", e)
                throw e
            }
        }
    }
}

/**
 * 메인 함수 - Spring Boot 실행
 */
fun main(args: Array<String>) {
    runApplication<Application>(*args)
}