package com.example.backend.web.service

import com.example.backend.web.repository.UserRepository
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class NicknameGeneratorService {
    private val adjectives = listOf(
        "빠른", "느린", "행복한", "슬픈", "귀여운", "용감한", "똑똑한", "친절한",
        "활발한", "조용한", "멋진", "재밌는", "신나는", "차분한", "밝은", "어두운",
        "강한", "약한", "큰", "작은", "긴", "짧은", "뜨거운", "차가운",
        "달콤한", "쓴", "매운", "신", "부드러운", "거친", "예쁜", "못생긴",
        "날씬한", "뚱뚱한", "키큰", "키작은", "젊은", "늙은", "새로운", "오래된",
        "깨끗한", "더러운", "맑은", "흐린", "향기로운", "시끄러운", "고요한", "화려한"
    )

    private val animals = listOf(
        "고양이", "강아지", "토끼", "햄스터", "판다", "코알라", "펭귄", "돌고래",
        "사자", "호랑이", "곰", "여우", "늑대", "다람쥐", "수달", "너구리",
        "기린", "코끼리", "하마", "코뿔소", "얼룩말", "캥거루", "원숭이", "침팬지",
        "앵무새", "독수리", "부엉이", "참새", "비둘기", "까마귀", "까치", "백조",
        "거북이", "악어", "뱀", "도마뱀", "개구리", "올챙이", "물고기", "상어",
        "고래", "문어", "오징어", "새우", "게", "소", "돼지", "말",
        "양", "염소", "닭", "오리", "거위", "칠면조", "타조", "쥐"
    )

    /**
     * Generate a random nickname with pattern: 형용사 + 동물
     * Example: "빠른고양이", "행복한판다"
     */
    fun generateNickname(): String {
        val adjective = adjectives[Random.nextInt(adjectives.size)]
        val animal = animals[Random.nextInt(animals.size)]
        return "$adjective$animal"
    }

    /**
     * Generate a unique nickname by checking against existing users
     * If duplicate, appends a number (e.g., "빠른고양이2")
     */
    fun generateUniqueNickname(userRepository: UserRepository): String {
        var nickname = generateNickname()
        var counter = 2

        // Keep trying until we find a unique nickname
        while (userRepository.existsByNickname(nickname)) {
            nickname = "${generateNickname()}$counter"
            counter++

            // Safety check: if counter gets too high, just use a timestamp
            if (counter > 100) {
                nickname = "${generateNickname()}${System.currentTimeMillis() % 10000}"
                break
            }
        }

        return nickname
    }
}
