package com.example.kafkaes.domain.model

import java.time.Instant
import java.time.ZoneId


/**
 * 메시지 도메인 모델
 *
 * 핵심 비즈니스 개념을 표현하는 순수한 데이터 클래스
 * - Kafka, Elasticsearch 등 기술적 세부사항을 몰라요
 * - 불변 객체 (val만 사용)
 * - 비즈니스 로직만 포함
 */
data class Message(
    val id: String,
    val content: String,
    val timeStamp: Long,
    val type: String?= null,
    val metadata: Map<String, String> =emptyMap()
){
    /**
     * 비즈니스 검직 로직
     * 도메인 규칙
     */
    fun isValid(): Boolean{
        // 예시: id와 content는 비어있지 않아야 함
        return id.isNotBlank() && content.isNotBlank() && timeStamp >0
    }

    /**
     * 메시지가 오래됐는지 확인
     */
    fun isExpired(maxAgeMillis: Long=24*60*60*1000):Boolean{
        val currentTime = System.currentTimeMillis()
        return (currentTime - timeStamp) > maxAgeMillis
    }

    /**
     * 사람이 읽기 쉬운 시간 표현
     */
    fun getFormattedTime(): String{
        return Instant.ofEpochMilli(timeStamp).atZone(ZoneId.systemDefault()).toLocalDateTime().toString()
    }
    /**
     * Elasticsearch 인덱스 이름 생성
     * 날짜별로 인덱스를 나누고 싶을 때 사용
     */
    fun getIndexName(prefix: String = "messages"): String {
        val instant = Instant.ofEpochMilli(timeStamp)
        val date = instant.toString().substring(0, 10) // YYYY-MM-DD
        return "$prefix-$date"
    }
}
