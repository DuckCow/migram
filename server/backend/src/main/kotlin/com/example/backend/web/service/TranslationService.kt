package com.example.backend.web.service
import com.example.backend.web.model.LanguageDetector
import com.example.backend.web.model.TranslationRequest
import com.example.backend.web.model.TranslationResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient



// OpusMT Translator 구현
@Component
class OpusMTTranslator(
    private val webClient: WebClient
) {
    @Value("\${translation.opusmt.url}")
    private lateinit var translationUrl: String

    fun translate(text: String, sourceLang: String, targetLang: String): String {
        val request = TranslationApiRequest(
            text = text,
            sourceLang = sourceLang,
            targetLang = targetLang
        )

        return webClient.post()
            .uri(translationUrl)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(TranslationApiResponse::class.java)
            .map { it.translatedText }
            .block() ?: throw TranslationException("Translation failed for text: $text")
    }
}

// 필요한 데이터 클래스들
data class TranslationApiRequest(
    val text: String,
    val sourceLang: String,
    val targetLang: String
)

data class TranslationApiResponse(
    val translatedText: String
)

@Service
class TranslationService(
    @Qualifier("customLanguageDetector") private val languageDetector: LanguageDetector,
    private val translator: OpusMTTranslator
) {
    fun translateText(request: TranslationRequest): TranslationResponse {
        // 1. 언어 감지
        val detectedLang = languageDetector.detect(request.text)

        // 2. 목표 언어 결정 (미지정시 영어로 기본 설정)
        val targetLang = request.targetLang ?: "en"

        // 3. 번역 수행
        val translatedText = translator.translate(
            text = request.text,
            sourceLang = detectedLang,
            targetLang = targetLang
        )

        return TranslationResponse(
            originalText = request.text,
            detectedLanguage = detectedLang,
            translatedText = translatedText,
            targetLanguage = targetLang
        )
    }
}

// 커스텀 예외 클래스들
class TranslationException(message: String) : RuntimeException(message)