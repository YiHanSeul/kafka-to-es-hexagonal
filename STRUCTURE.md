# 📂 상세 디렉토리 구조 및 설명

## 전체 구조
```
kafka-to-es-hexagonal/
├── 📄 docker-compose.yml          # 도커 환경 설정
├── 📄 Dockerfile                  # Elasticsearch + Nori
├── 📄 README.md                   # 프로젝트 전체 가이드
├── 📄 STRUCTURE.md               # 이 파일 (구조 상세 설명)
├── 📄 build.gradle.kts           # Gradle 빌드 설정
├── 📄 settings.gradle.kts        # Gradle 프로젝트 설정
└── 📁 src/
    └── 📁 main/
        ├── 📁 kotlin/
        │   └── 📁 com/example/kafkaes/
        │       │
        │       ├── 📁 domain/                    # 🎯 핵심 비즈니스 로직
        │       │   │
        │       │   ├── 📁 model/                 # 도메인 모델
        │       │   │   └── Message.kt           # 메시지 엔티티
        │       │   │
        │       │   ├── 📁 port/                  # 포트 (인터페이스)
        │       │   │   ├── 📁 inbound/          # 들어오는 요청
        │       │   │   │   └── MessageProcessor.kt
        │       │   │   │
        │       │   │   └── 📁 outbound/         # 나가는 요청
        │       │   │       ├── MessageConsumer.kt
        │       │   │       └── MessageIndexer.kt
        │       │   │
        │       │   └── 📁 service/              # 비즈니스 서비스
        │       │       └── MessageProcessingService.kt
        │       │
        │       ├── 📁 adapter/                   # 🔌 기술 구현
        │       │   │
        │       │   ├── 📁 inbound/              # 외부 → 도메인
        │       │   │   └── 📁 kafka/
        │       │   │       └── KafkaConsumerAdapter.kt
        │       │   │
        │       │   └── 📁 outbound/             # 도메인 → 외부
        │       │       └── 📁 elasticsearch/
        │       │           └── ElasticsearchAdapter.kt
        │       │
        │       ├── 📁 config/                    # ⚙️ 설정
        │       │   ├── KafkaConfig.kt
        │       │   └── ElasticsearchConfig.kt
        │       │
        │       └── Application.kt               # 🚀 메인
        │
        └── 📁 resources/
            └── application.yml                  # 설정값
```

---

## 🎯 Domain Layer (도메인 계층)

### 역할
- **순수한 비즈니스 로직**만 포함
- 외부 기술(Kafka, ES, DB 등)에 대해 모름
- 테스트하기 가장 쉬운 계층

### 파일별 설명

#### 📄 `domain/model/Message.kt`
```kotlin
// 메시지를 표현하는 도메인 모델
// 순수한 데이터 클래스, 기술 의존성 없음
data class Message(
    val id: String,
    val content: String,
    val timestamp: Long
)
```

#### 📄 `domain/port/inbound/MessageProcessor.kt`
```kotlin
// 도메인으로 들어오는 요청을 정의하는 인터페이스
// "메시지를 처리해주세요"라는 계약
interface MessageProcessor {
    suspend fun process(message: Message)
}
```

#### 📄 `domain/port/outbound/MessageConsumer.kt`
```kotlin
// 메시지를 받아오는 인터페이스
// 도메인은 "어디서" 받아오는지 몰라요
interface MessageConsumer {
    suspend fun consume(): Flow<Message>
}
```

#### 📄 `domain/port/outbound/MessageIndexer.kt`
```kotlin
// 메시지를 색인하는 인터페이스
// 도메인은 "어디로" 보내는지 몰라요
interface MessageIndexer {
    suspend fun index(message: Message)
}
```

#### 📄 `domain/service/MessageProcessingService.kt`
```kotlin
// 실제 비즈니스 로직 구현
// Port(인터페이스)에만 의존
class MessageProcessingService(
    private val consumer: MessageConsumer,
    private val indexer: MessageIndexer
) : MessageProcessor {
    // 메시지 받아서 처리하고 색인
}
```

---

## 🔌 Adapter Layer (어댑터 계층)

### 역할
- **기술적인 세부 구현** 담당
- Port(인터페이스)를 실제로 구현
- 외부 시스템과 통신

### 파일별 설명

#### 📄 `adapter/inbound/kafka/KafkaConsumerAdapter.kt`
```kotlin
// MessageConsumer 인터페이스의 Kafka 구현
// Kafka에서 메시지를 받아오는 실제 코드
class KafkaConsumerAdapter : MessageConsumer {
    // Kafka 클라이언트 사용
    // Kafka 토픽 구독
    // 메시지를 도메인 모델로 변환
}
```

#### 📄 `adapter/outbound/elasticsearch/ElasticsearchAdapter.kt`
```kotlin
// MessageIndexer 인터페이스의 ES 구현
// Elasticsearch로 색인하는 실제 코드
class ElasticsearchAdapter : MessageIndexer {
    // ES 클라이언트 사용
    // 인덱스에 문서 저장
}
```

---

## ⚙️ Config Layer (설정 계층)

### 역할
- **의존성 주입** (Dependency Injection)
- 객체 생성 및 조립
- 설정값 관리

#### 📄 `config/KafkaConfig.kt`
```kotlin
// Kafka 관련 설정
// Consumer 생성 및 설정
```

#### 📄 `config/ElasticsearchConfig.kt`
```kotlin
// Elasticsearch 관련 설정
// Client 생성 및 연결
```

---

## 🚀 Application Layer

#### 📄 `Application.kt`
```kotlin
// 메인 진입점
// 모든 컴포넌트를 조립하고 실행
fun main() {
    // 1. Config 로드
    // 2. Adapter 생성
    // 3. Service 생성
    // 4. 애플리케이션 실행
}
```

---

## 🔄 데이터 흐름

```
1. Kafka Topic
   ↓
2. KafkaConsumerAdapter (Adapter)
   ↓ (Message 도메인 모델로 변환)
3. MessageConsumer Interface (Port)
   ↓
4. MessageProcessingService (Domain)
   ↓ (비즈니스 로직 처리)
5. MessageIndexer Interface (Port)
   ↓
6. ElasticsearchAdapter (Adapter)
   ↓
7. Elasticsearch Index
```

---

## 🧪 테스트 전략

### Domain 테스트
```kotlin
// Mock 객체 사용 - 매우 쉬움!
val mockConsumer = mock<MessageConsumer>()
val mockIndexer = mock<MessageIndexer>()
val service = MessageProcessingService(mockConsumer, mockIndexer)
```

### Adapter 테스트
```kotlin
// Testcontainers 사용
// 실제 Kafka, ES 컨테이너로 통합 테스트
```

---

## 💡 왜 이렇게 복잡하게?

### 장점

1. **테스트 용이성**
    - 도메인 로직을 독립적으로 테스트
    - Mock 객체로 빠른 테스트

2. **유지보수성**
    - 비즈니스 로직과 기술 분리
    - 한 부분 수정이 다른 부분에 영향 ❌

3. **확장성**
    - Kafka → RabbitMQ 교체? Adapter만 바꾸면 됨
    - ES → MongoDB 교체? Adapter만 바꾸면 됨

4. **명확성**
    - 각 계층의 책임이 명확
    - 코드 읽기 쉬움

### 작은 프로젝트에는?

- 처음엔 과하게 느껴질 수 있어요
- 하지만 **학습 목적**으로는 최고!
- 큰 프로젝트로 성장할 때 유리해요

---

## 📝 다음 단계

지금까지는 "설계"만 했어요.
이제 **실제 코드**를 작성할 차례입니다!

**2단계: 도메인 모델부터 시작**해볼까요?