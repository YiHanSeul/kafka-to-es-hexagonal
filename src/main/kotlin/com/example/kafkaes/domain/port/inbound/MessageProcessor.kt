package com.example.kafkaes.domain.port.inbound

import com.example.kafkaes.domain.model.Message

/**
 * Inbound Port: 메시지 처리 요청을 받는 인터페이스
 *
 * "메시지를 처리해주세요"라는 명령을 정의
 *
 * 누가 이걸 구현하나?
 * → MessageProcessingService (비즈니스 로직)
 *
 * 누가 이걸 호출하나?
 * → MessageProcessingService 자기 자신.
 *   외부 어댑터(KafkaConsumerAdapter)가 전달한 메시지를 스트림(Flow)으로 받아,
 *   스스로 이 메소드를 호출하여 비즈니스 로직을 실행합니다.
 *   (개념적으로는 외부 어댑터가 도메인 로직 실행을 트리거하는 역할을 합니다.)
 */
interface MessageProcessor {
    /**
     * 단일 메세지 처리
     */
    suspend fun process(message: Message)

    /**
     * 다중 메세지 처리
     */
    suspend fun processBatch(messages: List<Message>):Int
}