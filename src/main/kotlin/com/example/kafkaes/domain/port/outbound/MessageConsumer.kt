package com.example.kafkaes.domain.port.outbound

import com.example.kafkaes.domain.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * Outbound Port: 메시지를 가져오는 인터페이스
 *
 * "메시지를 받아올 수 있어야 해요"라는 요구사항 정의
 *
 * 누가 이걸 구현하나?
 * → KafkaConsumerAdapter (Kafka에서 메시지 가져오기)
 *
 * 누가 이걸 사용하나?
 * → MessageProcessingService (도메인 서비스)
 *
 * 핵심: 도메인은 "어디서" 메시지가 오는지 몰라요!
 *       Kafka든, RabbitMQ든, HTTP든 상관없어요
 */
interface MessageConsumer {
    /**
     * 메시지 스트림 구독
     *
     * Flow를 사용해서 연속적으로 메시지를 받아요
     *
     * Flow란?
     * - 비동기 스트림 (물 흐르듯이 계속 데이터가 옴)
     * - 코루틴 기반
     * - collect { } 로 하나씩 처리
     *
     * @return 메시지 Flow
     */
    suspend fun consume(): Flow<Message>

    suspend fun start()

    suspend fun stop()
}