package com.example.withers_backend.web.controller

import com.example.backend.WithersBackendApplication
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

/*
@SpringBootTest(classes = [WithersBackendApplication::class])
class PostControllerTest {

    @Autowired
    private lateinit var context: WebApplicationContext

    private val mockMvc: MockMvc by lazy {
        MockMvcBuilders.webAppContextSetup(context).build()
    }

    @Test
    @WithMockUser(username = "user", roles = ["USER"])
    fun `should return all posts`() {
        mockMvc.perform(get("/posts"))
            .andExpect(status().isOk)
    }
}

*/
