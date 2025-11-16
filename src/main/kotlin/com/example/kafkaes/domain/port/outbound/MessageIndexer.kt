package com.example.kafkaes.domain.port.outbound

import com.example.kafkaes.domain.model.Message

/**
 * Outbound Port: 메시지를 색인하는 인터페이스
 *
 * "메시지를 저장/색인할 수 있어야 해요"라는 요구사항 정의
 *
 * 누가 이걸 구현하나?
 * → ElasticsearchAdapter (ES로 색인)
 *
 * 누가 이걸 사용하나?
 * → MessageProcessingService (도메인 서비스)
 *
 * 핵심: 도메인은 "어디에" 저장하는지 몰라요!
 *       Elasticsearch든, MongoDB든, MySQL이든 상관없어요
 */
interface MessageIndexer {
    //단일 메세지
    suspend fun index(message: Message): Boolean

    //다중 메세지
    suspend fun indexBatch(message: List<Message>): Int

    //메시지 삭제
    suspend fun delete(messageId: String): Boolean

    //인덱서 초기화/ 연결설정, 인덱스 생성
    suspend fun initialize()

    //인덱서 종료/ 리소스 정리(연결해제)
    suspend fun close()
}
// 색인 실패 시 발생하는 예외
class IndexingException(message: String, cause: Throwable? = null) : Exception(message, cause)