package com.example.kafkaes.domain.service

import com.example.kafkaes.domain.model.Message
import com.example.kafkaes.domain.port.inbound.MessageProcessor
import com.example.kafkaes.domain.port.outbound.MessageConsumer
import com.example.kafkaes.domain.port.outbound.MessageIndexer
import org.slf4j.LoggerFactory
import kotlinx.coroutines.flow.catch


/**
 * 메시지 처리 비즈니스 서비스
 *
 * 역할:
 * 1. MessageConsumer로부터 메시지 받기
 * 2. 비즈니스 로직 적용 (검증, 변환, 처리)
 * 3. MessageIndexer로 색인 요청
 *
 * 핵심: 이 클래스는 Kafka, Elasticsearch를 몰라요!
 *       인터페이스(Port)만 알고 있어요
 */
class MessageProcessingService(
    /**
     * 메세지를 받아오는 인터페이스
     * 실제로 kafka인지, RabbitMQ인지, HTTP인지 모름
     */
    private val messageConsumer : MessageConsumer,
    /**
     * 메시지를 색인하는 인터페이스
     * 실제로 ES인지, MongoDB인지 모름
     */
    private val messageIndexer : MessageIndexer
): MessageProcessor{
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 서비스 시작
     */
    suspend fun start(){
        //인덱서 초기화
        messageIndexer.initialize()
        //메시지 소비 시작
        messageConsumer.start()
        //메시지 스트림 구독
        messageConsumer.consume()
            .catch { e ->
                logger.error("Error consuming messages:", e)
            }
            .collect { message ->
                //메세지 하나씩 처리
                process(message)
            }
    }
    override suspend fun process(message: Message) {

        logger.debug("Process message:{}", message)
        logger.debug("메시지 처리 시작: id=${message.id}")

        try {
            //1단계 : 검증
            validateMessage(message)

            //2단계 : 비즈니스 구현
            val processedMessage = applyBusinessRules(message)

            //3단계 : 색인
            val success =  messageIndexer.index(processedMessage)

            if(success){
                logger.debug("메시지 색인 성공: id=${message.id}")
            }else{
                logger.error("메시지 색인 실패: id=${message.id}")
            }
        }catch (e:Exception){
            logger.error(e.message)
        }
    }



    override suspend fun processBatch(messages: List<Message>) :Int{
        logger.debug("Process message batch: size= ${messages.size}")
        //검증된 메시지만 핉터링
        val validMessage = messages.filter { message ->
            try {
                validateMessage(message)
                true
            } catch (e: Exception) {
                logger.error("메시지 검증 실패: id=${message.id}, error=${e.message}")
                false
            }
        }
        val processedMessage = validMessage.map { applyBusinessRules(it)}

        val successCount = messageIndexer.indexBatch(processedMessage)

        logger.debug("메시지 배치 색인 완료: 성공=${successCount}, 실패=${messages.size - successCount}")
        return successCount
    }

    private fun validateMessage(message: Message) {
        require(message.isValid()){
            "Invalid message: id=${message.id}"
        }
        require(message.isExpired()){
            "Expired message: id=${message.id}, timestamp=${message.getFormattedTime()}"
        }
        require(message.content.length <= 10000){
            "Message content too long: id=${message.id}, length=${message.content.length}"
        }
    }

    private fun applyBusinessRules(message: Message): Message {
        // 예시: 메타데이터 추가
        val enrichedMetadata = message.metadata.toMutableMap().apply {
            put("processed_at", System.currentTimeMillis().toString())
            put("service", "message-processing-service")
        }

        return message.copy(metadata = enrichedMetadata)
    }

    suspend fun stop(){
        logger.debug("<UNK> <UNK> <UNK> <UNK>")
        messageConsumer.stop()
        messageIndexer.close()
        logger.debug("MessageProcessingService stopped.")
    }
}