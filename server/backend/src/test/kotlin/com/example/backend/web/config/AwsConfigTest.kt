package com.example.backend.web.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import software.amazon.awssdk.services.s3.S3Client

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "S3_ACCESS_KEY", matches = ".+")
class S3Test {
    @Autowired
    private lateinit var s3Client: S3Client

    @Test
    fun testS3Connection() {
        val buckets = s3Client.listBuckets()
        println("Buckets: ${buckets.buckets()}")
    }
}