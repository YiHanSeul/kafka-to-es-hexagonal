package com.example.kafkaes.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

/**
 * Elasticsearch 설정 클래스
 *
 * elasticsearch.yml 파일을 읽어서 설정 제공
 *
 * 역할:
 * - YAML 파일 로드
 * - 타입 안전한 설정 제공
 * - ES 클라이언트 설정 생성
 */
class ElasticsearchConfig {

    // YAML 파일 위치
    private val configFile = File("src/main/resources/elasticsearch.yml")

    // YAML 파서 (kebab-case → camelCase 자동 변환)
    private val yamlMapper = ObjectMapper(YAMLFactory()).apply {
        findAndRegisterModules()
        propertyNamingStrategy = com.fasterxml.jackson.databind.PropertyNamingStrategies.KEBAB_CASE
    }

    // 설정 데이터
    private val config: ElasticsearchConfigData = loadConfig()

    /**
     * YAML 파일 로드
     */
    private fun loadConfig(): ElasticsearchConfigData {
        return yamlMapper.readValue(configFile)
    }

    /**
     * 연결 정보
     */
    val host: String
        get() = config.elasticsearch.connection.host

    val port: Int
        get() = config.elasticsearch.connection.port

    val scheme: String
        get() = config.elasticsearch.connection.scheme

    /**
     * 인덱스 설정
     */
    val indexName: String
        get() = config.elasticsearch.index.name

    val indexDateBased: Boolean
        get() = config.elasticsearch.index.dateBased

    val indexShards: Int
        get() = config.elasticsearch.index.shards

    val indexReplicas: Int
        get() = config.elasticsearch.index.replicas

    val refreshInterval: String
        get() = config.elasticsearch.index.refreshInterval

    /**
     * Bulk 설정
     */
    val bulkActions: Int
        get() = config.elasticsearch.bulk.actions

    val bulkSizeMb: Int
        get() = config.elasticsearch.bulk.sizeMb

    val bulkFlushIntervalMs: Long
        get() = config.elasticsearch.bulk.flushIntervalMs

    val bulkConcurrentRequests: Int
        get() = config.elasticsearch.bulk.concurrentRequests

    /**
     * 타임아웃 설정
     */
    val connectTimeoutMs: Int
        get() = config.elasticsearch.timeout.connectMs

    val socketTimeoutMs: Int
        get() = config.elasticsearch.timeout.socketMs

    /**
     * 재시도 설정
     */
    val retryMaxAttempts: Int
        get() = config.elasticsearch.retry.maxAttempts

    val retryDelayMs: Long
        get() = config.elasticsearch.retry.delayMs

    /**
     * 서버 URL 생성
     */
    fun getServerUrl(): String {
        return "$scheme://$host:$port"
    }

    /**
     * 설정 출력 (디버깅용)
     */
    fun printConfig() {
        println("""
            ===== Elasticsearch Configuration =====
            Server: ${getServerUrl()}
            Index: $indexName
            Date-based: $indexDateBased
            Shards: $indexShards
            Replicas: $indexReplicas
            Bulk Actions: $bulkActions
            =======================================
        """.trimIndent())
    }
}

/**
 * YAML 파일 구조를 매핑하는 데이터 클래스들
 */
data class ElasticsearchConfigData(
    val elasticsearch: ElasticsearchSettings
)

data class ElasticsearchSettings(
    val connection: ConnectionSettings,
    val auth: AuthSettings,
    val index: IndexSettings,
    val bulk: BulkSettings,
    val timeout: TimeoutSettings,
    val retry: RetrySettings
)

data class ConnectionSettings(
    val host: String,
    val port: Int,
    val scheme: String
)

data class AuthSettings(
    val enabled: Boolean,
    val username: String,
    val password: String
)

data class IndexSettings(
    val name: String,
    val dateBased: Boolean,
    val shards: Int,
    val replicas: Int,
    val refreshInterval: String
)

data class BulkSettings(
    val actions: Int,
    val sizeMb: Int,
    val flushIntervalMs: Long,
    val concurrentRequests: Int
)

data class TimeoutSettings(
    val connectMs: Int,
    val socketMs: Int
)

data class RetrySettings(
    val maxAttempts: Int,
    val delayMs: Long
)