package com.example.kafkaes.adapter.inbound.kafka

import com.example.kafkaes.domain.model.Message
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory


/**
 * Kafka 메시지 파서
 *
 * JSON 문자열을 도메인 모델(Message)로 변환
 *
 * 역할:
 * - JSON 파싱
 * - 도메인 모델 생성
 * - 파싱 에러 처리
 */
object KafkaMessageParser {
    private val logger = LoggerFactory.getLogger(KafkaMessageParser::class.java)

    // JSON 문자열을 도메인 모델로 변환
    private val objectMapper = jacksonObjectMapper()

    /**
     * JSON 문자열 → Message 도메인 모델
     *
     * 입력 예시:
     * {
     *   "id": "msg-001",
     *   "content": "안녕하세요",
     *   "timestamp": 1699520000000,
     *   "type": "notification",
     *   "metadata": {
     *     "source": "mobile-app",
     *     "version": "1.0"
     *   }
     * }
     *
     * @param json JSON 문자열
     * @return Message 도메인 모델
     * @throws MessageParsingException 파싱 실패 시
     */
    fun parse(json: String): Message{
        try{
            logger.debug("Parsing JSON message: {}", json)
            // JSON 문자열 -> JSON 노드
            val jsonNode: JsonNode = objectMapper.readTree(json)

            val id = jsonNode.get("id")?.asText() ?: throw MessageParsingException("Missing 'id' field")
            val content = jsonNode.get("content")?.asText() ?: throw MessageParsingException("Missing 'content' field")
            val timeStamp = jsonNode.get("timestamp")?.asLong() ?: throw MessageParsingException("Missing 'timestamp' field")
            val type = jsonNode.get("type")?.asText()
            val metadata = parseMetadata(jsonNode.get("metadata"))

            val message =Message(
                id = id,
                content = content,
                timeStamp = timeStamp,
                type = type,
                metadata = metadata
            )
            logger.debug("Parsed Message object: {}", message)
            return message
        }catch (e: MessageParsingException){
            throw e

        }catch (e: Exception){
            logger.error("Failed to parse JSON message: {}", json, e)
            throw MessageParsingException("Failed to parse message", e)
        }
    }

    // metadata 필드 파싱
    private fun parseMetadata(node: JsonNode?): Map<String, String>{
        if(node == null || node.isNull){
            return emptyMap()
        }
        val metadata = mutableMapOf<String, String>()

        // JsonNode의 모든 필드를 순회
        node.fields().forEach { (key, value) ->
            metadata[key] = value.asText()
        }

        return metadata
    }
    /**
     * 다중 JSON 문자열 → Message 도메인 모델 리스트
     *
     * @param jsonList JSON 문자열 리스트
     * @return Message 도메인 모델 리스트
     */
    fun parseAll(jsonList: List<String>): List<Message>{
        return jsonList.mapNotNull{ json ->
            try {
                parse(json)
            }catch (e: MessageParsingException){
                logger.error("메시지 파싱 실패: json=$json, error=${e.message}")
                null
            }
        }
    }
}

class MessageParsingException(message: String, cause: Throwable?= null): Exception(message, cause)