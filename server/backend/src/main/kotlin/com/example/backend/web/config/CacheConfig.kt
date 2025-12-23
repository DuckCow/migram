package com.example.backend.web.config

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfig {

    companion object {
        // Cache names
        const val HOT_SCORE_CACHE = "hotScores"
        const val VOTE_COUNT_CACHE = "voteCounts"
        const val FEED_CACHE = "feeds"
        const val USER_VOTES_CACHE = "userVotes"

        // TTL values
        private val HOT_SCORE_TTL = Duration.ofMinutes(15)
        private val VOTE_COUNT_TTL = Duration.ofMinutes(5)
        private val FEED_TTL = Duration.ofMinutes(10)
        private val USER_VOTES_TTL = Duration.ofMinutes(30)
    }

    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): CacheManager {
        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    GenericJackson2JsonRedisSerializer()
                )
            )
            .disableCachingNullValues()

        val cacheConfigurations = mapOf(
            HOT_SCORE_CACHE to defaultConfig.entryTtl(HOT_SCORE_TTL),
            VOTE_COUNT_CACHE to defaultConfig.entryTtl(VOTE_COUNT_TTL),
            FEED_CACHE to defaultConfig.entryTtl(FEED_TTL),
            USER_VOTES_CACHE to defaultConfig.entryTtl(USER_VOTES_TTL)
        )

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(15)))
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }
}
