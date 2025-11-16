package com.example.kafkaes.adapter.outbound.elasticsearch

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.Refresh
import co.elastic.clients.elasticsearch.core.BulkRequest
import co.elastic.clients.elasticsearch.core.BulkResponse
import co.elastic.clients.elasticsearch.core.IndexRequest
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest
import co.elastic.clients.elasticsearch.indices.ExistsRequest
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import com.example.kafkaes.config.ElasticsearchConfig
import com.example.kafkaes.domain.model.Message
import com.example.kafkaes.domain.port.outbound.IndexingException
import com.example.kafkaes.domain.port.outbound.MessageIndexer
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.slf4j.LoggerFactory

/**
 * Elasticsearch Adapter
 *
 * MessageIndexer 인터페이스의 Elasticsearch 구현체
 */
class ElasticsearchAdapter(
    private val config: ElasticsearchConfig
) : MessageIndexer {

    private val logger = LoggerFactory.getLogger(javaClass)

    private lateinit var client: ElasticsearchClient
    private lateinit var restClient: RestClient
    private lateinit var transport: RestClientTransport


    /**
     * 인덱서 초기화
     */
    override suspend fun initialize() {
        logger.info("Elasticsearch Adapter 초기화 시작")

        try {
            restClient = RestClient.builder(
                HttpHost(config.host, config.port, config.scheme)
            ).build()

            // 🔹 transport를 지역 변수가 아닌 필드로 유지해야 연결 유지됨
            transport = RestClientTransport(restClient, JacksonJsonpMapper())

            // 🔹 client도 transport 기반으로 생성
            client = ElasticsearchClient(transport)

            // 연결 테스트
            val info = client.info()
            logger.info("✅ Elasticsearch 연결 성공: version=${info.version().number()}")

            createIndexIfNotExists()
            logger.info("Elasticsearch Adapter 초기화 완료")

        } catch (e: Exception) {
            logger.error("❌ Elasticsearch Adapter 초기화 실패", e)
            throw IndexingException("Elasticsearch Adapter 초기화 실패: ${e.message}", e)
        }
    }

    /**
     * 인덱스 생성 (존재하지 않으면)
     */
    private fun createIndexIfNotExists() {
        val indexName = config.indexName

        // 인덱스 존재 확인
        val exists = client.indices().exists(
            ExistsRequest.Builder().index(indexName).build()
        ).value()

        if (!exists) {
            logger.info("인덱스 생성 중: $indexName")

            // 인덱스 생성
            client.indices().create { c ->
                c.index(indexName)
                    .settings { s ->
                        s.numberOfShards(config.indexShards.toString())
                            .numberOfReplicas(config.indexReplicas.toString())
                            .refreshInterval { t -> t.time(config.refreshInterval) }
                    }
                    .mappings { m ->
                        m.properties("id") { p ->
                            p.keyword { k -> k }
                        }
                            .properties("content") { p ->
                                p.text { t ->
                                    t.analyzer("nori")
                                }
                            }
                            .properties("timestamp") { p ->
                                p.date { d -> d }
                            }
                            .properties("type") { p ->
                                p.keyword { k -> k }
                            }
                            .properties("metadata") { p ->
                                p.flattened { f -> f }
                            }
                    }
            }

            logger.info("인덱스 생성 완료: $indexName")
        } else {
            logger.info("인덱스 이미 존재: $indexName")
        }
    }

    /**
     * 단일 메시지 색인
     */
    override suspend fun index(message: Message): Boolean {
        return try {
            val indexName = getIndexName(message)
            logger.debug("메시지 색인 시작: id=${message.id}, index=$indexName")

            // 색인 요청
            val response = client.index { idx ->
                idx.index(indexName)
                    .id(message.id)
                    .document(message)
                    .refresh(Refresh.False)
            }

            val success = response.result().name == "Created" || response.result().name == "Updated"

            if (success) {
                logger.info("메시지 색인 완료: id=${message.id}, result=${response.result()}")
            } else {
                logger.warn("메시지 색인 실패: id=${message.id}, result=${response.result()}")
            }

            success

        } catch (e: Exception) {
            logger.error("메시지 색인 중 에러: id=${message.id}", e)
            throw IndexingException("메시지 색인 실패: ${e.message}", e)
        }
    }

    /**
     * 배치 색인 (Bulk API)
     */
    override suspend fun indexBatch(messages: List<Message>): Int {
        if (messages.isEmpty()) {
            return 0
        }

        return try {
            logger.info("배치 색인 시작: ${messages.size}개 메시지")

            // Bulk 요청 생성
            val bulkRequest = BulkRequest.Builder()

            messages.forEach { message ->
                val indexName = getIndexName(message)

                bulkRequest.operations { op ->
                    op.index { idx ->
                        idx.index(indexName)
                            .id(message.id)
                            .document(message)
                    }
                }
            }

            // Bulk 실행
            val response: BulkResponse = client.bulk(bulkRequest.build())

            // 결과 분석 (Kotlin nullable 방식)
            val successCount = response.items().count { it.error() == null }
            val failCount = response.items().count { it.error() != null }

            if (failCount > 0) {
                logger.warn("배치 색인 일부 실패: 성공=$successCount, 실패=$failCount")

                // 실패한 항목 로깅
                response.items()
                    .filter { it.error() != null }
                    .forEach { item ->
                        val errorReason = item.error()?.reason() ?: "Unknown error"
                        logger.error("색인 실패: id=${item.id()}, error=$errorReason")
                    }
            } else {
                logger.info("배치 색인 완료: ${successCount}개 성공")
            }

            successCount

        } catch (e: Exception) {
            logger.error("배치 색인 중 에러", e)
            throw IndexingException("배치 색인 실패: ${e.message}", e)
        }
    }

    /**
     * 메시지 삭제
     */
    override suspend fun delete(messageId: String): Boolean {
        return try {
            val indexName = config.indexName

            logger.debug("메시지 삭제 시작: id=$messageId")

            val response = client.delete { d ->
                d.index(indexName).id(messageId)
            }

            val success = response.result().name == "Deleted"

            if (success) {
                logger.info("메시지 삭제 완료: id=$messageId")
            } else {
                logger.warn("메시지 삭제 실패: id=$messageId, result=${response.result()}")
            }

            success

        } catch (e: Exception) {
            logger.error("메시지 삭제 중 에러: id=$messageId", e)
            false
        }
    }

    /**
     * 인덱서 종료
     */
    override suspend fun close() {
        logger.info("Elasticsearch Adapter 종료 중...")
        try {
            if (::transport.isInitialized) {
                transport.close()
            }
            if (::restClient.isInitialized) {
                restClient.close()
            }
            logger.info("Elasticsearch 연결 종료 완료")
        } catch (e: Exception) {
            logger.error("Elasticsearch 종료 중 에러", e)
        }
    }

    /**
     * 인덱스 이름 결정
     */
    private fun getIndexName(message: Message): String {
        return if (config.indexDateBased) {
            message.getIndexName(config.indexName)
        } else {
            config.indexName
        }
    }
}