package com.example.backend.web.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Paths

@Configuration
class WebConfig(
    @Value("\${storage.local.upload-dir:./uploads}") private val uploadDir: String
) : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // Serve uploaded files as static resources
        val uploadPath = Paths.get(uploadDir).toAbsolutePath().toUri().toString()

        registry.addResourceHandler("/uploads/**")
            .addResourceLocations(uploadPath)

        println("Static resource handler configured: /uploads/** -> $uploadPath")
    }
}
