package com.example.backend.web.controller

import com.example.backend.web.model.TranslationRequest
import com.example.backend.web.model.TranslationResponse
import com.example.backend.web.service.TranslationService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/translation")
class TranslationController(
    private val translationService: TranslationService
) {
    @PostMapping
    fun translate(@RequestBody request: TranslationRequest): TranslationResponse {
        return translationService.translateText(request)
    }
}