package com.example.kafkaes.adapter.inbound.kafka

import com.example.kafkaes.domain.model.Message
import com.example.kafkaes.domain.port.outbound.MessageConsumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.KafkaMessageListenerContainer
import org.springframework.kafka.listener.MessageListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

/**
 * Kafka Consumer Adapter
 *
 * MessageConsumer 인터페이스의 Kafka 구현체
 *
 * 역할:
 * 1. Kafka 토픽 구독
 * 2. 메시지 폴링 (계속 가져오기)
 * 3. JSON → Message 변환
 * 4. Flow로 스트리밍
 *
 * 핵심: 도메인은 이게 Kafka인지 몰라요!
 *       MessageConsumer 인터페이스만 알고 있어요
 */
@Component
class KafkaConsumerAdapter: MessageConsumer {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val messageChannel = Channel<Message>(Channel.UNLIMITED)

    @KafkaListener(
        topics = ["\${kafka.consumer.topic}"],
        groupId = "\${kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun listen(
        value: String,
        acknowledgment: Acknowledgment
    ){
        try {
            logger.debug("Kafka에서 메시지 수신: $value")
            //여기서 JSON 파싱하여 Message 객체로 변환
            val message = KafkaMessageParser.parse(value)

            logger.info("메세징 파싱 완료 id=${message.id}")

            //채널에 메시지 전송 (비동기)
            //채널에 전송하게 되면 MessageProcessingService에서 consume()을 통해 수신 가능
            messageChannel.trySend(message).also{
                if (it.isSuccess) {
                    logger.debug("메시지 채널에 전송 시도 성공: ${message.id}")
                } else {
                    logger.error("메시지 채널에 전송 시도 실패: ${message.id}", it.exceptionOrNull())
                }
            }
            acknowledgment.acknowledge()
        }catch (ex: MessageParsingException){
                logger.error("메시징 파싱 실패 : $value", ex)
                //파싱 실패 시에도 오프셋 커밋
                acknowledgment.acknowledge()
        } catch (ex: Exception) {
            logger.error("메시지 처리 중 오류 발생: $value", ex)
        }

    }
    /**
     * Consumer 시작
     *
     * Kafka 연결 및 토픽 구독
     */
    override suspend fun start() {
        logger.info("✨ Kafka Consumer Adapter 준비 완료 (Spring 관리)")
    }

    /**
     * Consumer 중지
     *
     * 리소스 정리
     */
    override suspend fun stop() {
        logger.info("Kafka Consumer 중지 중...")
        messageChannel.close()
    }

    /**
     * 메시지 스트림 구독
     */
    override suspend fun consume(): Flow<Message>{
        logger.info("📡 메시지 스트림 구독 시작")
        return messageChannel.receiveAsFlow()
    }

}