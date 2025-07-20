package org.manganesium.ai.service

import org.manganesium.ai.config.LLMConfig
import org.manganesium.ai.model.AIResponse
import org.manganesium.ai.model.Message
import org.manganesium.ai.model.UsageStats

/**
 * Abstract service interface for all LLM providers
 */
interface LLMService {
    
    /**
     * Generate a response from the LLM based on the prompt and context
     */
    suspend fun generateResponse(
        prompt: String, 
        context: List<Message> = emptyList(),
        config: LLMConfig = LLMConfig.default()
    ): Result<AIResponse>
    
    /**
     * Generate embeddings for the provided texts
     */
    suspend fun generateEmbeddings(texts: List<String>): Result<List<FloatArray>>
    
    /**
     * Get current usage statistics
     */
    fun getUsageStats(): UsageStats
    
    /**
     * Check if the service is available and properly configured
     */
    suspend fun isHealthy(): Boolean
    
    /**
     * Get the name of the LLM provider
     */
    fun getProviderName(): String
}