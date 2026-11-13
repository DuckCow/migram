# 번역 DB 캐시 통합 테스트

[TranslationRedisFallbackIT.kt](TranslationRedisFallbackIT.kt)는 현재 [TranslationService](../../../../../../../main/kotlin/com/example/backend/web/service/TranslationService.kt)의 **DB 캐시 동작**을 확인합니다. 클래스 이름은 이전 명칭을 유지하고 있으며, 현재 서비스는 Spring Cache나 Redis를 통한 2단계 캐시를 사용하지 않습니다.

## 검증 시나리오

| 시나리오 | 코드에 정의된 확인 내용 |
| --- | --- |
| 첫 번역 요청 | 모의 번역 API 응답과 `cached=false` |
| 번역 저장 | `content_translation` 조회로 저장 결과 확인 |
| 동일 요청 재호출 | JPA flush/clear 후 DB 캐시 사용, HTTP 요청 수 증가가 1회인지 확인 |
| 원문·대상 언어 동일 | 원문 반환, HTTP 요청 수 증가가 0회인지 확인 |

## 테스트 환경

- JDK 17, JUnit 5, Testcontainers, MockWebServer
- 테스트 전용 `postgres:15-alpine` 컨테이너
- Spring `test` 프로필, Flyway 비활성화, `ddl-auto=create-drop`
- 테스트의 `@DynamicPropertySource`가 DB 연결과 번역 URL을 컨테이너 및 MockWebServer로 대체
- [테스트 설정](../../../../../../resources/application-test.yml)에서 Spring Cache 비활성화

의존성 버전은 [build.gradle.kts](../../../../../../../../build.gradle.kts)를 기준으로 합니다.

## 실행

Docker가 실행 중이고 테스트 이미지 다운로드가 가능한 환경에서, 저장소 루트 기준으로 실행합니다.

```bash
cd server/backend
./gradlew test --tests '*TranslationRedisFallbackIT' --no-daemon
```

결과는 `server/backend/build/reports/tests/test/index.html`과 `server/backend/build/test-results/test/`에서 확인합니다.

[현재 CI](../../../../../../../../../../.github/workflows/integration-test.yml)에도 이 테스트를 실행하는 단계가 있지만 `continue-on-error: true`가 설정되어 있습니다. 워크플로의 성공 표시만으로 테스트 성공을 판단하지 않고 개별 결과와 로그를 확인해야 합니다.

## 검증 범위

이 테스트는 DB 캐시와 모의 HTTP 응답의 연동을 다룹니다. Redis 장애 복구, 실제 번역 품질, 운영 데이터베이스 마이그레이션, 캐시 TTL·원문 수정 후 무효화, 동시 삽입 충돌과 대규모 성능까지 입증하지는 않습니다.

문서 수정 시 테스트를 새로 실행한 것은 아니므로 통과 결과나 실행 시간을 기재하지 않습니다.

[프로젝트 개요로 돌아가기](../../../../../../../../../../README.md)
