# TranslationRedisFallbackIT - Integration Test Documentation

## Overview

`TranslationRedisFallbackIT` is a comprehensive integration test that verifies the **graceful degradation** behavior of the `TranslationService` when Redis is unavailable.

## Objective

Ensure that when Redis cache is configured but unreachable, the application:
1. ✅ **Continues to function** without throwing 500 errors
2. ✅ **Falls back to DB-only caching** (2-tier cache: Hot Redis fails → Warm DB succeeds)
3. ✅ **Calls the Translation API only once** (subsequent requests use DB cache)

## Test Scenarios

### 1. Redis Fallback - Service Works Without Errors
**When:** Redis is unavailable (misconfigured host/port)
**Then:** `getTranslation()` completes successfully without 500 errors

### 2. Translation Saved to DB Cache
**When:** Redis is down and translation is requested
**Then:** Translation is saved to `content_translation` table

### 3. API Called Only Once (DB Cache Hit)
**When:** Same translation requested twice with Redis down
**Then:**
- First call: Hits Translation API, saves to DB (cached = false)
- Second call: Returns from DB cache (cached = true)
- **Verify:** MockWebServer received exactly 1 request

### 4. Same Language No-Op
**When:** Source and target language are the same
**Then:** Original text returned without API call (model = "none")

## Technical Implementation

### Dependencies Added

```kotlin
// build.gradle.kts
testImplementation("org.testcontainers:testcontainers:1.19.0")
testImplementation("org.testcontainers:junit-jupiter:1.19.0")
testImplementation("org.testcontainers:postgresql:1.19.0")
testImplementation("com.squareup.okhttp3:mockwebserver:4.11.0")
```

### Test Configuration

- **Spring Context:** `@SpringBootTest(webEnvironment = NONE)` - No web server needed
- **Database:** PostgreSQL via Testcontainers (`PostgreSQLContainer`)
- **Redis:** Configured but **intentionally unavailable** (wrong host: `invalid-redis-host-does-not-exist`)
- **Translation API:** Mocked using OkHttp `MockWebServer`
- **Flyway:** Disabled (using `ddl-auto=create-drop` for test)

### Key Test Setup

```kotlin
@DynamicPropertySource
fun configureProperties(registry: DynamicPropertyRegistry) {
    // PostgreSQL (real database via Testcontainers)
    registry.add("spring.datasource.url", postgres::getJdbcUrl)

    // Redis (intentionally misconfigured to simulate unavailability)
    registry.add("spring.cache.type") { "redis" }
    registry.add("spring.data.redis.host") { "invalid-redis-host-does-not-exist" }
    registry.add("spring.data.redis.port") { "6390" }

    // Mock translation API
    registry.add("translation.base-url") { mockWebServer.url("/").toString() }
}
```

## Running the Test

### Prerequisites

1. **Docker Must Be Running**
   - Testcontainers requires Docker to spin up PostgreSQL
   - Verify: `docker ps` should work

2. **Network Access**
   - Test needs to pull `postgres:15-alpine` Docker image

### Run Command

```bash
# Run specific integration test
./gradlew test --tests TranslationRedisFallbackIT

# Run with verbose logging
./gradlew test --tests TranslationRedisFallbackIT --info

# Run all tests
./gradlew test
```

### Expected Output

```
TranslationRedisFallbackIT > Redis down → Service should fallback to DB cache PASSED
TranslationRedisFallbackIT > Translation should be saved to DB cache PASSED
TranslationRedisFallbackIT > Translation API should be called only ONCE PASSED
TranslationRedisFallbackIT > Same source and target language PASSED
```

## Troubleshooting

### Error: Docker not available

**Symptom:**
```
Could not find a valid Docker environment
org.testcontainers.dockerclient.DockerClientProviderStrategy
```

**Solution:**
1. Start Docker Desktop
2. Verify Docker is accessible: `docker ps`
3. Ensure Docker socket permissions are correct

### Error: Image pull failure

**Symptom:**
```
Unable to pull postgres:15-alpine
```

**Solution:**
1. Check internet connection
2. Manually pull image: `docker pull postgres:15-alpine`
3. Verify Docker Hub access

### Error: Port already in use

**Symptom:**
```
Bind for 0.0.0.0:5432 failed: port is already allocated
```

**Solution:**
- Testcontainers uses random ports by default
- If issue persists, stop other PostgreSQL containers: `docker stop $(docker ps -q --filter ancestor=postgres)`

## CI/CD Configuration

### GitHub Actions Example

```yaml
name: Integration Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    services:
      docker:
        image: docker:dind
        options: --privileged

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'corretto'

      - name: Run Integration Tests
        run: ./gradlew test --tests TranslationRedisFallbackIT
```

## Architecture Diagram

```
┌─────────────────────────────────────────────┐
│ TranslationRedisFallbackIT (JUnit5)         │
├─────────────────────────────────────────────┤
│ ┌─────────────────┐   ┌─────────────────┐  │
│ │ TranslationSvc  │   │ Redis (DOWN)    │  │
│ │                 │──>│ invalid-host    │  │
│ └────────┬────────┘   └─────────────────┘  │
│          │ fallback                         │
│          v                                  │
│ ┌─────────────────────────────────────────┐ │
│ │ PostgreSQL (Testcontainers)             │ │
│ │ content_translation table               │ │
│ └─────────────────────────────────────────┘ │
│          ^                                  │
│          │ API call (mocked)                │
│ ┌────────┴────────┐                        │
│ │ MockWebServer   │                        │
│ │ (OkHttp)        │                        │
│ └─────────────────┘                        │
└─────────────────────────────────────────────┘
```

## Performance Characteristics

- **Test Duration:** ~5-10 seconds (includes Docker container startup)
- **Resource Usage:** ~512MB RAM (PostgreSQL container)
- **Network:** Requires Docker image pull on first run (~50MB)

## Future Improvements

1. **Add Redis Recovery Test**
   - Start Redis mid-test
   - Verify cache warming from DB → Redis

2. **Concurrent Request Test**
   - Multiple threads requesting same translation
   - Verify DB constraint handles race conditions

3. **Batch Translation Test**
   - Request translations for 100+ posts
   - Verify DB caching reduces API calls

## Related Documentation

- [DEPLOYMENT.md](../../../../../../../../../DEPLOYMENT.md) - Redis Security & Fallback Testing section
- [TranslationService.kt](../../../../../../main/kotlin/com/example/backend/web/service/TranslationService.kt) - Implementation
- [Version-Based Cache Invalidation](../../../../../../main/resources/db/migration/V7__add_content_version.sql) - Migration

## Maintainer Notes

**When to Run:**
- Before merging PRs that modify caching logic
- After upgrading Redis/Spring Boot versions
- Monthly as part of regression test suite

**What This Test Does NOT Cover:**
- Actual Redis availability (Redis is intentionally unavailable)
- Network latency/timeout scenarios
- Redis connection pool exhaustion

For these scenarios, use manual testing or chaos engineering tools (e.g., Toxiproxy).

---

**Last Updated:** 2025-12-28
**Test Status:** ✅ Implementation Complete, ⚠️ Requires Docker Configuration
