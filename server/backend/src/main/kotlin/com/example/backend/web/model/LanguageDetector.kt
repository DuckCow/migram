package com.example.backend.web.model

import com.example.backend.web.service.LanguageDetectionException
import com.optimaize.langdetect.LanguageDetectorBuilder
import com.optimaize.langdetect.ngram.NgramExtractors
import com.optimaize.langdetect.profiles.LanguageProfileReader
import org.springframework.stereotype.Component

@Component("customLanguageDetector")
class LanguageDetector {
    private val detector = run {
        val languageProfileReader = LanguageProfileReader()
        val languageProfiles = languageProfileReader.readAllBuiltIn().filter { profile ->
            profile.locale.language in setOf("ko", "en", "ja", "zh", "vi", "th", "id", "ms")
        }

        LanguageDetectorBuilder.create(NgramExtractors.standard())
            .withProfiles(languageProfiles)
            .build()
    }

    fun detect(text: String): String {
        return try {
            detector.detect(text)
                ?.get()?.language
                ?: throw LanguageDetectionException("Cannot detect language for text: $text")
        } catch (e: Exception) {
            throw LanguageDetectionException("Language detection failed: ${e.message}")
        }
    }
}