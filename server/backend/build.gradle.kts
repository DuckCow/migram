import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.Duration



plugins {
	id("org.springframework.boot") version "3.1.3"
	id("io.spring.dependency-management") version "1.1.3"
	kotlin("jvm") version "1.8.22"
	kotlin("plugin.spring") version "1.8.22"
	kotlin("plugin.jpa") version "1.8.22"
}

extra["testcontainers.version"] = "1.20.4"

dependencyManagement {
	imports {
		mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainers.version")}")
	}
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
	sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
	mavenCentral()
}


dependencies {

	// .env 파일 지원
	implementation("me.paulschwarz:spring-dotenv:4.0.0")

	// Redis 캐싱
	implementation("org.springframework.boot:spring-boot-starter-data-redis")


	implementation("io.jsonwebtoken:jjwt-api:0.12.3")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3") // JSON 처리를 위함

	// 언어 감지를 위한 의존성 (langid 대신 사용할 수 있는 라이브러리)
	implementation("com.optimaize.languagedetector:language-detector:0.6")

	// Coroutines 지원 (비동기 처리용)
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.1")

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.7.1")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:3.0.1"))
	implementation("io.awspring.cloud:spring-cloud-aws-starter")
	implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")
	implementation("software.amazon.awssdk:s3:2.20.26")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("com.nimbusds:nimbus-jose-jwt:9.15.2")

	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
	implementation("org.flywaydb:flyway-core")
	//developmentOnly("org.springframework.boot:spring-boot-devtools")
	runtimeOnly("com.h2database:h2")
	runtimeOnly("org.postgresql:postgresql")


	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
	testImplementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:3.0.1"))
	testImplementation("io.awspring.cloud:spring-cloud-aws-starter")
	testImplementation("io.awspring.cloud:spring-cloud-aws-starter-s3")
	testImplementation("org.junit.jupiter:junit-jupiter-api")
	testImplementation("org.mockito:mockito-core")
	testImplementation("org.mockito.kotlin:mockito-kotlin")
	testImplementation("org.mockito:mockito-junit-jupiter")
	testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
	testImplementation("org.assertj:assertj-core")
	testImplementation("com.h2database:h2")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework.security:spring-security-test")

	// Testcontainers for integration tests
	// ✅ Testcontainers BOM (버전은 여기서만 관리)
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:postgresql")

	// Redis 쓰면 이것도 추가
	// testImplementation("org.testcontainers:redis")

	// MockWebServer for mocking HTTP endpoints in tests
	testImplementation("com.squareup.okhttp3:mockwebserver:4.11.0")

	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.Embeddable")
	annotation("jakarta.persistence.MappedSuperclass")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs += "-Xjsr305=strict"
		jvmTarget = "17"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()


	// CI supplies the running Docker daemon API; preserve the local default.
	systemProperty("api.version", providers.environmentVariable("DOCKER_API_VERSION").getOrElse("1.52"))


	// Prevent test worker JVM shutdown hang (force cleanup after timeout)
	maxParallelForks = 1
	forkEvery = 1
	timeout.set(Duration.ofMinutes(5))

	// Enable detailed logging for debugging worker lifecycle
	testLogging {
		events("started", "passed", "skipped", "failed")
		showStandardStreams = true
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
	}
}

tasks.test {
	useJUnitPlatform()
	systemProperty("spring.profiles.active", "test")
}