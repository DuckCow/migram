package com.example.backend

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.stereotype.Component

@SpringBootApplication
@EnableJpaAuditing
class WithersBackendApplication

fun main(args: Array<String>) {
	runApplication<com.example.backend.WithersBackendApplication>(*args)
}


