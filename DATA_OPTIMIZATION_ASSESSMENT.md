# Data Optimization Assessment – Satellite Platform Application

**Date:** April 14, 2026  
**Focus:** Architectural data handling patterns for large satellite datasets  
**Scope:** Design & architectural choices, not small function tweaks

---

## Executive Summary

**Does it handle large amounts of data?**  
Partially. The system *can* handle large data but uses patterns that scale poorly under concurrent load or massive payloads. Data flows through memory frequently; caching is shallow; and storage decisions are made ad-hoc.

**Major weaknesses:**
1. **In-memory data accumulation** – GEE returns entire payloads to RAM; no chunking/streaming.
2. **Synchronous blocking** – Heavy I/O (image processing, file downloads) blocks thread pools.
3. **Write amplification** – Each workflow log, status, and result triggers separate DB writes.
4. **No layered caching strategy** – Redis caching exists but is not semantically organized (one-size-fits-all prefix).
5. **Uncontrolled concurrency** – Task executor pool is small (10 max) with no backpressure.
6. **Eagerly materialized collections** – All images/results fetched as full lists instead of paginated streams.

---

## 1. Current Data Flow & Bottlenecks

### 1.1 GEE Service → Backend Data Path

**Current pattern:**
```
Frontend request
  ↓
Backend REST endpoint
  ↓
GEE Python service (port 5000)
  ↓ [LARGE PAYLOAD in memory]
Backend receives response (100MB+ image bytes)
  ↓ [Held in memory or written eagerly to disk]
Response sent to Frontend OR stored to MongoDB
```

**Problems:**
- **No streaming:** The backend does `geeService.downloadFileBytes(exportedFilePath)` which loads the entire file into RAM.
- **No chunking:** No progress tracking, no resumable downloads, no partial re-requests.
- **Redis caching ignores data size:** Cache stores full results with TTL, ignoring memory pressure.
- **Workflow logs aren't batched properly:** Buffering exists but only for *count* (10 events), not for *size*.

---

### 1.2 Image Processing Path

**Current pattern (FastAPI):**
```
Frontend multipart upload
  ↓
Backend multipart relay to Python
  ↓ [Entire image in memory while calculating NDVI/EVI]
  ↓ [Result (PNG) also in memory]
  ↓ [Multipart response sent back]
Backend stores to MongoDB or filesystem
```

**Problems:**
- **Single-pass processing:** File is read entire before processing; no pipelining.
- **Large temp files:** All intermediate results stay on disk until explicitly cleaned.
- **No streaming response:** Full PNG buffered before sending; ties up thread.
- **Serialization overhead:** Array-to-JSON-to-PNG has multiple conversions.

---

### 1.3 Database Patterns

**MongoDB:**
- Full collection hydration (no lazy loading).
- No aggregation pipeline for filtering/projection at DB level.
- Logs stored as arrays in execution docs → unbounded growth.

**Redis:**
- Single prefix namespace (`cache:data:`).
- TTL-only eviction; no LRU or size-based strategy.
- No differentiation between hot/cold data.

---

### 1.4 Frontend Data Handling

**Current pattern:**
```
Frontend calls workflow.service.executeWorkflow(id)
  ↓
GET /api/workflows/{id}/executions (entire list)
  ↓ [All executions in memory, including full logs/results]
  ↓
Display in UI (no virtual scrolling observed)
```

**Problems:**
- No pagination in workflow execution list.
- WebSocket broadcasts full execution state, not delta.
- No lazy loading of results/logs on demand.
- No client-side caching strategy.

---

## 2. Architectural Data Optimization Opportunities

### **TIER 1: High Impact, Medium Effort**

#### **2.1 Streaming & Chunked Data Transfer**

**Problem:** GEE downloads load entire files into RAM.

**Pattern:** **Output Stream + Chunked Transfer**

```java
// Instead of:
byte[] rawFile = geeService.downloadFileBytes(exportedFilePath); // entire file in RAM

// Use:
try (InputStream fileStream = geeService.downloadFileStream(exportedFilePath)) {
    // Stream to file incrementally
    try (FileOutputStream fos = new FileOutputStream(destFile)) {
        byte[] buffer = new byte[8192]; // 8KB chunks
        int read;
        while ((read = fileStream.read(buffer)) != -1) {
            fos.write(buffer, 0, read);
        }
    }
    // OR: stream directly to HTTP response for frontend
    response.setHeader("Content-Type", "application/octet-stream");
    fileStream.transferTo(response.getOutputStream());
}
```

**Benefits:**
- O(1) memory for any file size.
- Resumable downloads via Range headers.
- Progress tracking via byte counters.

**Implementation locations:**
- `DataInputNodeExecutor.java` (GEE file download)
- Image Processing response handling
- Workflow result export endpoints

---

#### **2.2 Durable Write Buffering (Buffer-on-Write Pattern)**

**Problem:** Workflow logs trigger individual DB writes; high I/O contention.

**Pattern:** **Batched Async Flush**

```java
// Instead of writing every log immediately:
private static final int BATCH_SIZE = 100; // logs
private static final long FLUSH_INTERVAL_MS = 5000; // 5s

private List<WorkflowLog> logBuffer = new CopyOnWriteArrayList<>();

public void addLog(WorkflowLog log) {
    logBuffer.add(log);
    if (logBuffer.size() >= BATCH_SIZE) {
        flushLogs();
    }
}

@Scheduled(fixedRate = FLUSH_INTERVAL_MS)
public void flushLogs() {
    if (!logBuffer.isEmpty()) {
        List<WorkflowLog> toFlush = new ArrayList<>(logBuffer);
        logBuffer.clear();
        // Single bulk update to MongoDB
        executionRepository.bulkUpdateLogs(executionId, toFlush);
    }
}
```

**Status:** Already partially implemented in `WorkflowExecutionService` (LOG_BUFFER_FLUSH_SIZE=10, LOG_BUFFER_FLUSH_INTERVAL_MS=2000), but:
- Only tracks *count*, not *size*.
- Not applied to status/results updates.
- Missing synchronization protection in high-concurrency scenarios.

**Benefits:**
- 10x reduction in write operations.
- Predictable I/O patterns.
- Easier to audit/rollback batch failures.

---

#### **2.3 Layered Caching Strategy**

**Problem:** Redis cache treats all data equally; no semantic distinction between hot/cold, small/large, or TTL-sensitive.

**Pattern:** **Multi-tier Cache with Policies**

```java
// Tier 1: L1 Cache (Frontend/Client-side) — immutable reference data
// Tier 2: L2 Cache (In-process, small) — hot metadata
// Tier 3: L3 Cache (Redis, medium) — frequently accessed results
// Tier 4: L4 Cache (MongoDB, large) — cold storage

// Example: Cache manager per data type
public interface CacheTier {
    Optional<T> get(String key);
    void put(String key, T value, Duration ttl);
    void invalidate(String key);
}

// Differentiate caching by payload size and access pattern
if (data.size() < 1MB && accessFrequency > 10) {
    cache.putL3(key, data, Duration.ofHours(1)); // Redis
} else if (data.size() < 50MB && accessFrequency > 1) {
    cache.putL4(key, data, Duration.ofDays(7)); // MongoDB (tagged)
} else {
    storageManager.store(data); // Filesystem/S3
}
```

**Benefits:**
- Smart memory allocation.
- Reduced Redis memory pressure.
- Separation of concerns (metadata vs. blobs).

**Implementation locations:**
- `SatelliteProcessingCacheHandler.java` (extend with tier logic)
- GEE result caching layer
- Workflow execution result caching

---

### **TIER 2: Medium Impact, Lower Effort**

#### **2.4 Pagination & Lazy Loading (All List Endpoints)**

**Problem:** All collections fetched as complete lists.

**Current:**
```java
// ImageService.java
List<Image> images = imageRepository.findByProject(projectId); // entire collection
```

**Pattern:** **Cursor-based Pagination**

```java
public Page<ImageDTO> getImages(String projectId, Pageable pageable) {
    return imageRepository.findByProject_Id(projectId, pageable)
        .map(imageMapper::toDTO);
}

// Frontend: useEffect with infinite scroll or paginated requests
const [page, setPage] = useState(0);
const { data, hasMore } = useQuery(['images', page], 
    () => fetchImages({ page, size: 20 })
);
```

**Locations to fix:**
- Workflow execution history list (no pagination observed).
- Image list endpoints.
- Project resources/results listing.
- GEE result galleries.

**Benefits:**
- O(1) response time regardless of total data size.
- Bounded memory on frontend.
- Scalable to millions of records.

---

#### **2.5 Unbuffered Logging (Stream-based Audit)**

**Problem:** Logs stored in arrays on execution documents; unbounded growth.

**Pattern:** **Separate Log Collection with TTL Index**

```java
@Document(collection = "workflow_logs")
public class WorkflowLogEntry {
    @Id
    private String id;
    private String executionId;
    private LocalDateTime timestamp;
    private String message;
    private LogLevel level;
    
    @Indexed(expireAfterSeconds = 2592000) // 30 days TTL
    private LocalDateTime createdAt;
}

// Query logs separately:
List<WorkflowLogEntry> logs = logRepository.findByExecutionId(executionId, 
    PageRequest.of(0, 100, Sort.by("timestamp").descending())
);
```

**Benefits:**
- Execution document stays small and fast to fetch/update.
- Automatic cleanup via MongoDB TTL index.
- Query efficiency (dedicated index on executionId + timestamp).
- Decoupled from execution lifecycle.

---

#### **2.6 Connection Pooling & Resource Limits**

**Problem:** Task executor pool is small; no backpressure on incoming requests.

**Current:**
```java
executor.setCorePoolSize(5);
executor.setMaxPoolSize(10);
executor.setQueueCapacity(25);
```

**Pattern:** **Adaptive Thread Pool + Rejection Policy**

```java
@Bean
public TaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(Runtime.getRuntime().availableProcessors()); // 4-32 cores
    executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
    executor.setQueueCapacity(100); // allow more queuing, not blocking
    executor.setRejectedExecutionHandler(new ThreadPoolTaskExecutor.CallerRunsPolicy()); 
        // or: new ThreadPoolTaskExecutor.AbortPolicy() + circuit breaker
    executor.setThreadNamePrefix("WorkflowExecution-");
    executor.initialize();
    return executor;
}
```

**Benefits:**
- Scales with actual hardware capacity.
- Queue provides backpressure without dropping requests.
- Observable via actuator metrics.

---

### **TIER 3: Lower Impact, Quick Wins**

#### **2.7 GZIP Compression for Large Responses**

**Problem:** Large GEE/image results sent uncompressed.

**Pattern:** Content negotiation + automatic compression

```java
// In application.properties
spring.http.encoding.enabled=true
spring.http.encoding.charset=UTF-8
spring.http.encoding.force=true

// For large streaming responses:
response.setHeader("Content-Encoding", "gzip");
// Spring automatically compresses if client accepts it
```

**Benefits:**
- 3-10x reduction in network bandwidth for typical geospatial JSON.
- Transparent; no code changes needed.

---

#### **2.8 Database Indexing Strategy**

**Problem:** Queries lack proper indexes; no compound indexes for common filters.

**Example missing indexes:**
```java
@Document
@CompoundIndex(name = "execution_project_user", def = "{'workflowId': 1, 'triggeredBy': 1, 'startedAt': -1}")
@CompoundIndex(name = "image_project_name", def = "{'project._id': 1, 'name': 1}")
public class WorkflowExecution {
    // ...
}
```

**Benefits:**
- Query response time drops 10-100x for filtered/sorted lists.
- Automatic via Spring Data MongoDB annotations.

---

## 3. Data-Specific Architectural Patterns to Adopt

### **Pattern A: Event Sourcing (for Audit/Reproducibility)**

**Use case:** Workflows and GEE analyses should be reproducible.

**Current state:** Status and logs mixed; hard to trace decisions.

**Approach:**
```
Every state change (Workflow.QUEUED → RUNNING → COMPLETED) → immutable event
Events stored in append-only log, never mutated
Projections (current status view) rebuilt from event stream on demand
```

**Benefit:** Full audit trail, time-travel debugging, deterministic replay.

**Priority:** Medium (nice-to-have for compliance; required for reproducible science).

---

### **Pattern B: Data Lake / Staging Area**

**Use case:** GEE downloads huge amounts of raw satellite data; need intermediate processing.

**Current state:** Files go directly to user project storage or MongoDB.

**Approach:**
```
GEE download → Temporary staging (S3/MinIO or temp filesystem)
                ↓
            Transform (chunking, format conversion)
                ↓
            Index metadata in MongoDB
                ↓
            Move to permanent storage (if retained) or delete
```

**Benefit:**
- Decouples ingestion from storage decisions.
- Enables batch processing of multiple downloads.
- Reduces peak memory usage.

**Priority:** High (directly addresses GEE data spike problem).

---

### **Pattern C: Bloom Filters / Deduplication**

**Use case:** Same GEE query from multiple users; don't recompute.

**Current state:** Cache exists, but requests are deduplicated manually.

**Approach:**
```
Hash incoming request (collection_id, region, date_range, etc.)
Check Bloom filter: "have we seen this?" → yes → await cached result
If no → process, store result with dedup key
```

**Benefit:** Prevents thundering herd; reduced GEE API quota burn.

**Priority:** Medium (significant for multi-user scenarios).

---

### **Pattern D: Backpressure & Graceful Degradation**

**Use case:** Sudden spike in workflow submissions overwhelms backend.

**Current state:** RabbitMQ queue exists but no size limits or priority.

**Approach:**
```
Monitor queue depth; if > threshold:
  - Reject low-priority requests with 429 Too Many Requests
  - Offer user: "queue for later" vs "try in 5 mins"
  - Track SLA metrics (queue wait time, processing time)
```

**Benefit:** Predictable behavior under load; prevents cascading failures.

**Priority:** High (encadrant's main concern).

---

## 4. Quick-Wins Checklist (Immediate Implementation)

| Item | Effort | Impact | Status |
|------|--------|--------|--------|
| Add pagination to execution history list | 2 hrs | High | Not done |
| Implement streaming for GEE file downloads | 4 hrs | High | Not done |
| Separate logs into own collection with TTL | 3 hrs | Medium | Not done |
| Fix write buffering (size + time) | 2 hrs | Medium | Partial |
| Add GZIP compression config | 30 min | Low | Not done |
| Add missing database indexes | 1 hr | Medium | Not done |
| Increase thread pool to CPU count | 30 min | Medium | Not done |
| Implement data deduplication cache layer | 6 hrs | High | Not done |

---

## 5. Recommended 3-Month Roadmap

### **Month 1: Write Buffering & Streaming**
- Week 1-2: Implement buffered writes for logs/status (TIER 2.2).
- Week 2-3: Add streaming for GEE downloads (TIER 1.1).
- Week 4: Pagination on all list endpoints (TIER 2.4).

### **Month 2: Caching & Deduplication**
- Week 1-2: Layered cache strategy (TIER 1.3).
- Week 2-3: Implement Bloom filter dedup (Pattern C).
- Week 4: Separate log collection (TIER 2.5).

### **Month 3: Backpressure & Monitoring**
- Week 1-2: Backpressure/queue monitoring (Pattern D).
- Week 2-3: Event sourcing for audit trail (Pattern A).
- Week 4: Data lake staging area (Pattern B).

---

## 6. Metrics to Track

```
Before → After

Write operations / minute: 1000 → 100 (10x reduction)
Avg memory per GEE download: 500MB → 50MB (streaming)
P99 workflow execution list load: 2s → 200ms (pagination)
Cache hit rate: 20% → 70% (layered caching)
Queue rejection rate: 0% → <1% (backpressure)
```

---

## 7. No Code, But Patterns Clear

This assessment provides **architectural guidance** without implementation. The next step is to decide which pattern(s) align with your team's priorities and encadrant's requirements.

**Key takeaway:** The app *can* handle large data, but it does so inefficiently. The fixes are **design patterns**, not algorithmic tweaks—they require thoughtful restructuring of how data flows through the system.

