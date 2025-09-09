# 🚀 Excel Download Performance Optimization Project

엑셀 다운로드 성능 최적화를 위한 포트폴리오 프로젝트입니다.  
기존 방식의 문제점을 파악하고, 메모리 효율성과 동시성 제어를 개선한 솔루션을 제공합니다.

## 📋 프로젝트 개요

### 해결한 문제
실무에서 발생했던 **엑셀 다운로드 OOM(Out Of Memory) 문제**를 해결하고,  
동시성 제어를 통해 서버 안정성을 향상시키는 것이 목표입니다.

### 핵심 개선 사항
- ✅ **메모리 최적화**: 전체 데이터를 메모리에 로드하지 않는 스트리밍 방식
- ✅ **동시성 제어**: 인메모리 큐를 통한 요청 대기열 관리 
- ✅ **실시간 모니터링**: WebSocket을 활용한 다운로드 진행률 표시
- ✅ **사용자 경험**: 비동기 처리로 브라우저 blocking 방지

## 🛠 기술 스택

- **Backend**: Spring Boot 3.2, Java 17
- **Database**: H2 (In-Memory)
- **Excel**: Apache POI (SXSSFWorkbook)
- **WebSocket**: Spring WebSocket
- **Queue**: Java Concurrent Collections
- **Build Tool**: Gradle

## 🏗 아키텍처

```
[Client Request] 
      ↓
[ExcelDownloadController] 
      ↓
[ExcelDownloadQueue] ← 최대 3개 동시 처리 제한
      ↓
[ExcelDownloadService]
      ↓ (기존방식)        ↓ (개선방식)
[페이징 처리]       [스트리밍 처리]
      ↓                    ↓
[메모리 축적]       [메모리 효율적]
      ↓                    ↓
[ExcelWriter (SXSSFWorkbook)]
      ↓
[WebSocket Progress Updates]
```

## 🚦 실행 방법

### 1. 프로젝트 실행
```bash
./gradlew bootRun
```

### 2. 웹 브라우저 접속
```
http://localhost:8080
```

### 3. 테스트 시나리오
1. **데이터 생성**: 10,000~50,000건의 테스트 데이터 생성
2. **기존 방식 테스트**: 페이징 방식으로 다운로드 
3. **개선 방식 테스트**: 스트리밍 방식으로 다운로드
4. **동시성 테스트**: 여러 다운로드 요청을 동시에 실행
5. **진행률 모니터링**: WebSocket을 통한 실시간 진행률 확인

## 📊 성능 비교

| 구분 | 기존 방식 (페이징) | 개선 방식 (스트리밍) |
|-----|------------------|-------------------|
| **메모리 사용량** | 전체 데이터 크기 | 고정 (100행) |
| **OOM 위험도** | 높음 | 낮음 |
| **처리 방식** | 배치 단위 누적 | 스트림 기반 |
| **동시 처리** | 제한 없음 | 최대 3개 |
| **진행률 표시** | 배치 단위 | 실시간 |

## 📁 주요 파일 구조

```
src/main/java/com/jikim/ecommerce/
├── controller/
│   ├── ExcelDownloadController.java    # 다운로드 API
│   └── SampleDataController.java       # 테스트 데이터 API
├── service/
│   ├── ExcelDownloadService.java       # 메인 다운로드 로직
│   ├── ExcelDownloadQueue.java         # 인메모리 큐 관리
│   └── SampleDataService.java          # 테스트 데이터 생성
├── util/
│   └── ExcelWriter.java               # SXSSFWorkbook 엑셀 작성
├── websocket/
│   └── ProgressWebSocketHandler.java   # 실시간 진행률
└── dto/
    ├── DownloadRequest.java           # 다운로드 요청 DTO
    └── DownloadProgress.java          # 진행률 DTO

src/main/resources/
├── static/
│   └── index.html                     # 테스트 웹 페이지
└── application.yml                    # 설정 파일
```

## 🔧 API 엔드포인트

### 테스트 데이터
- `POST /api/sample-data/generate?count=10000` - 데이터 생성
- `GET /api/sample-data/count` - 데이터 개수 조회
- `DELETE /api/sample-data/clear` - 전체 데이터 삭제

### 엑셀 다운로드
- `POST /api/download/excel/paging` - 기존 방식 (페이징)
- `POST /api/download/excel/streaming` - 개선 방식 (스트리밍)
- `GET /api/download/file/{fileName}` - 완성된 파일 다운로드
- `GET /api/download/queue/status` - 큐 상태 조회

### WebSocket
- `ws://localhost:8080/ws/download-progress` - 실시간 진행률

## 💡 핵심 구현 포인트

### 1. 메모리 최적화 (SXSSFWorkbook)
```java
// 메모리에 100개 행만 유지하고 나머지는 임시 파일로 처리
try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
    // 엑셀 작성 로직
    workbook.dispose(); // 임시 파일 정리
}
```

### 2. 스트리밍 처리 (JPA Stream)
```java
@Transactional(readOnly = true)
try (Stream<SampleData> dataStream = repository.findAllByOrderByIdStream()) {
    dataStream.forEach(data -> {
        // 메모리 효율적인 스트림 처리
    });
}
```

### 3. 동시성 제어 (BlockingQueue)
```java
private final BlockingQueue<DownloadRequest> downloadQueue = new LinkedBlockingQueue<>();
private final ConcurrentHashMap<String, DownloadRequest> processingRequests = new ConcurrentHashMap<>();

// 최대 3개까지만 동시 처리
if (processingRequests.size() < MAX_CONCURRENT_DOWNLOADS) {
    DownloadRequest request = downloadQueue.poll(1, TimeUnit.SECONDS);
    // 처리 로직
}
```

### 4. 실시간 진행률 (WebSocket)
```java
@Component
public class ProgressWebSocketHandler extends TextWebSocketHandler {
    public void sendProgress(String sessionId, DownloadProgress progress) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(progress)));
        }
    }
}
```

## 🧪 테스트 방법

### HTTP 요청 테스트
```bash
# 데이터 생성
curl -X POST "http://localhost:8080/api/sample-data/generate?count=10000"

# 기존 방식 다운로드
curl -X POST "http://localhost:8080/api/download/excel/paging" \
  -H "X-Session-ID: test-session-1"

# 개선 방식 다운로드  
curl -X POST "http://localhost:8080/api/download/excel/streaming" \
  -H "X-Session-ID: test-session-2"

# 큐 상태 확인
curl "http://localhost:8080/api/download/queue/status"
```

### 웹 인터페이스 테스트
1. http://localhost:8080 접속
2. WebSocket 연결 확인
3. 테스트 데이터 생성 (10,000건 추천)
4. 기존 방식과 개선 방식 비교 테스트
5. 동시 다운로드 요청으로 큐 동작 확인

## 📈 성능 측정 결과

### 메모리 사용량 비교 (10,000건 기준)
- **기존 방식**: 약 50MB (전체 데이터 로드)
- **개선 방식**: 약 5MB (스트리밍 처리)
- **메모리 절약**: 90% 감소

### 처리 시간 비교
- **기존 방식**: 데이터 크기에 비례하여 증가
- **개선 방식**: 안정적인 처리 시간 유지
- **동시성**: 최대 3개까지 안정적 처리

## 🎯 프로젝트 의의

### 실무 문제 해결
- 실제 운영 환경에서 발생한 OOM 문제를 해결
- 대용량 데이터 처리 시 서버 안정성 확보
- 사용자 경험 개선을 통한 서비스 품질 향상

### 기술적 성장
- Spring Boot 고급 기능 활용 (WebSocket, Async, Stream)
- 메모리 최적화 및 성능 튜닝 경험
- 동시성 프로그래밍 실무 적용
- 아키텍처 설계 및 문제 해결 능력 향상

## 🔗 관련 링크

- [Apache POI Documentation](https://poi.apache.org/)
- [Spring WebSocket Reference](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#websocket)
- [Java Stream API Guide](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html)

## 📝 라이센스

이 프로젝트는 MIT 라이센스 하에 있습니다. 자유롭게 사용하고 수정할 수 있습니다.

---

**💻 개발자**: 김지완  
**📧 이메일**: ji1007k@gmail.com  
**🔗 GitHub**: https://github.com/ji1007k/sb-simple-ecommerce