package com.example.backend.web

import com.example.backend.WithersBackendApplication
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles


@SpringBootTest(classes = [WithersBackendApplication::class])
@ActiveProfiles("test")
class WithersBackendApplicationTests {

	@Test
	fun contextLoads() {
	}

}
