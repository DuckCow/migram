# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Migram is a social platform with a Flutter mobile app frontend and a Kotlin Spring Boot backend. The system includes a Python-based translation service for multilingual support.

## Build & Development Commands

### Backend (Kotlin/Spring Boot)
```bash
cd server/backend

# Build
./gradlew clean build --no-daemon

# Run all tests
./gradlew test --no-daemon

# Run specific test class
./gradlew test --tests TranslationRedisFallbackIT --info --no-daemon

# Run development server (uses application-dev.yml with H2)
./gradlew bootRun

# Cleanup Gradle processes
./gradlew --stop
```

### Frontend (Flutter)
```bash
cd frontend/migram_app

# Get dependencies
flutter pub get

# Run app
flutter run

# Build Android APK
flutter build apk

# Build iOS
flutter build ios
```

### Translation Service (Python)
```bash
cd server/translation
python3 -m venv venv && source venv/bin/activate
pip install -r requirements.txt
python translation.py  # Runs on localhost:5001
```

### Docker
```bash
cd server
docker-compose up --build
```

## Architecture

### Backend Structure (`server/backend/`)
- **Controllers** (`controller/`): REST API endpoints
- **Services** (`service/`): Business logic layer
- **Repositories** (`repository/`): JPA data access
- **Models** (`model/`): JPA entities with auditing support
- **Config** (`config/`): Security, Cache, JPA, AWS, WebClient configurations
- **DTOs** (`dto/`): Data transfer objects for API responses

### Frontend Structure (`frontend/migram_app/lib/`)
- **Screens** (`screens/`): UI pages (feed, chat, profile, etc.)
- **Services** (`services/`): API communication layer using Dio
- **Models** (`models/`): Data models
- **Widgets** (`widgets/`): Reusable UI components
- **L10n** (`l10n/`): Internationalization (intl)

### Key Architectural Patterns
1. **Authentication**: JWT tokens + OAuth2 (Google) with deep-link based mobile auth
2. **Caching**: Redis for hot scores, vote counts, feeds; Database for translation cache
3. **Database**: PostgreSQL (prod) / H2 (dev) with Flyway migrations
4. **File Storage**: AWS S3 (prod) / Local filesystem (dev)

## Database Migrations

Flyway migrations in `server/backend/src/main/resources/db/migration/`:
- V1: 모든 base tables DDL (users, post, comment, boards, follow, etc.)
- V2: votes 테이블 + CHECK constraint (post_id XOR comment_id)
- V3: likes → votes 데이터 이관 (idempotent, Fresh DB에선 no-op)
- V4: 성능 인덱스 (placeholder - JPA @Index로 대체)
- V5: content_translation 테이블 (번역 캐시)

**Fresh DB setup**: `rm -rf ./data/migram*` 후 서버 재시작

**주의**: `ddl-auto: validate` 사용. 모든 DDL은 Flyway가 관리.

## Testing

- **Framework**: JUnit 5 + Testcontainers (PostgreSQL)
- **Mocking**: Mockito + MockWebServer for HTTP
- **Test Profile**: Uses `application-test.yml`
- **CI**: GitHub Actions runs integration tests with Testcontainers

## Configuration Profiles

- `application.yml`: Base configuration
- `application-dev.yml`: H2 database, Redis localhost, local file storage
- `application-prod.yml`: PostgreSQL, Redis pool, S3 storage
- `application-test.yml`: Test profile for integration tests

## Environment Variables

See `server/.env.example` for required variables:
- Database credentials (POSTGRES_*)
- AWS S3 configuration
- OAuth2 client credentials (Google)
- JWT secret key

## Key Dependencies

**Backend**: Spring Boot 3.1.3, Kotlin 1.8.22, JDK 17, JJWT 0.12.3, Spring Cloud AWS 3.0.1
**Frontend**: Flutter 3.4.4+, Dio, WebView Flutter
**Translation**: Python 3.9, Flask 2.0.1, Helsinki-NLP OPUS-MT models
