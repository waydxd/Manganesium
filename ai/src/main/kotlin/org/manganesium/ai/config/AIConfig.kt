package org.manganesium.ai.config

import kotlinx.serialization.Serializable

/**
 * Configuration for LLM services
 */
@Serializable
data class LLMConfig(
    val model: String = "gpt-4",
    val maxTokens: Int = 1000,
    val temperature: Double = 0.7,
    val topP: Double = 1.0,
    val presencePenalty: Double = 0.0,
    val frequencyPenalty: Double = 0.0,
    val timeout: Long = 30000L, // 30 seconds
    val retryAttempts: Int = 3,
    val rateLimitPerMinute: Int = 60
) {
    companion object {
        fun default() = LLMConfig()
        
        fun conversational() = LLMConfig(
            model = "gpt-4",
            maxTokens = 2000,
            temperature = 0.8
        )
        
        fun factual() = LLMConfig(
            model = "gpt-4",
            maxTokens = 1500,
            temperature = 0.2
        )
    }
}

/**
 * AI service configuration
 */
data class AIConfig(
    val openAIApiKey: String = System.getenv("OPENAI_API_KEY") ?: "",
    val openAIBaseUrl: String = "https://api.openai.com/v1",
    val defaultModel: String = "gpt-4",
    val enableCaching: Boolean = true,
    val cacheExpirationMinutes: Long = 60,
    val maxConversationHistory: Int = 20,
    val enableUsageTracking: Boolean = true,
    val logLevel: String = "INFO"
) {
    
    fun isValid(): Boolean {
        return openAIApiKey.isNotBlank()
    }
    
    companion object {
        fun fromEnvironment(): AIConfig {
            return AIConfig(
                openAIApiKey = System.getenv("OPENAI_API_KEY") ?: "",
                openAIBaseUrl = System.getenv("OPENAI_BASE_URL") ?: "https://api.openai.com/v1",
                defaultModel = System.getenv("OPENAI_DEFAULT_MODEL") ?: "gpt-4",
                enableCaching = System.getenv("AI_ENABLE_CACHING")?.toBoolean() ?: true,
                cacheExpirationMinutes = System.getenv("AI_CACHE_EXPIRATION_MINUTES")?.toLong() ?: 60,
                maxConversationHistory = System.getenv("AI_MAX_CONVERSATION_HISTORY")?.toInt() ?: 20
            )
        }
    }
}