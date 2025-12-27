package com.example.backend.web.service


import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import java.util.*

@SpringBootTest(
    classes = [S3Service::class, com.example.backend.web.config.TestAwsConfig::class])
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class S3ServiceIntegrationTest {
    @Autowired
    private lateinit var s3Service: S3Service



    @Test
    fun `실제 S3 파일 업로드 및 삭제 테스트`() {

        // Given
        // SecurityContext 설정
        val authentication = TestingAuthenticationToken("user", "password", "ROLE_USER")
        SecurityContextHolder.getContext().authentication = authentication

        val fileName = "test_${System.currentTimeMillis()}.txt"  // 유니크한 파일명
        val content = "test content"
        val mockFile = MockMultipartFile(
            "file",
            fileName,
            "text/plain",
            content.toByteArray()
        )

        // When - 파일 업로드
        val uploadedUrl = s3Service.uploadFile(mockFile, fileName)
        println("Uploaded file URL: $uploadedUrl")

        // Then - 업로드 확인
        assertTrue(uploadedUrl?.contains(fileName) == true)

        // 파일 삭제
        //s3Service.deleteFile(fileName)
    }

    @Test
    fun `큰 파일 업로드 테스트`() {
        // Given
        val authentication = TestingAuthenticationToken("user", "password", "ROLE_USER")
        SecurityContextHolder.getContext().authentication = authentication


        val fileName = "large_test_${System.currentTimeMillis()}.txt"
        val content = ByteArray(5 * 1024 * 1024) // 5MB
        Random().nextBytes(content)

        val mockFile = MockMultipartFile(
            "file",
            fileName,
            "application/octet-stream",
            content
        )

        // When
        val uploadedUrl = s3Service.uploadFile(mockFile, fileName)
        println("Uploaded large file URL: $uploadedUrl")

        // Then
        assertTrue(uploadedUrl?.contains(fileName) == true)

        // Cleanup
        uploadedUrl?.let { s3Service.deleteFile(fileName) }
    }
}