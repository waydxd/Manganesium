package org.manganesium.ai.service

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import org.manganesium.ai.client.*
import org.manganesium.ai.config.AIConfig
import org.manganesium.ai.config.LLMConfig
import org.manganesium.ai.model.*
import java.util.*
import java.util.concurrent.atomic.AtomicLong

/**
 * OpenAI implementation of LLMService
 */
class OpenAIService(private val config: AIConfig) : LLMService {
    
    private val logger = KotlinLogging.logger {}
    private val client = OpenAIClient(config)
    private val promptService = PromptService()
    
    // Usage tracking
    private val totalRequests = AtomicLong(0)
    private val totalTokens = AtomicLong(0)
    private val totalResponseTime = AtomicLong(0)
    private val mutex = Mutex()
    
    override suspend fun generateResponse(
        prompt: String,
        context: List<Message>,
        config: LLMConfig
    ): Result<AIResponse> {
        if (!this.config.isValid()) {
            return Result.failure(IllegalStateException("OpenAI API key not configured"))
        }
        
        val startTime = System.currentTimeMillis()
        
        return try {
            // Prepare messages for OpenAI API
            val messages = buildList {
                // Add system message
                add(ChatMessage("system", promptService.createSystemPrompt()))
                
                // Add conversation context
                context.forEach { message ->
                    val role = when (message.role) {
                        MessageRole.USER -> "user"
                        MessageRole.ASSISTANT -> "assistant"
                        MessageRole.SYSTEM -> "system"
                    }
                    add(ChatMessage(role, message.content))
                }
                
                // Add current prompt
                add(ChatMessage("user", prompt))
            }
            
            val request = ChatCompletionRequest(
                model = config.model,
                messages = messages,
                maxTokens = config.maxTokens,
                temperature = config.temperature,
                topP = config.topP,
                presencePenalty = config.presencePenalty,
                frequencyPenalty = config.frequencyPenalty
            )
            
            val result = client.createChatCompletion(request)
            
            result.fold(
                onSuccess = { response ->
                    val responseTime = System.currentTimeMillis() - startTime
                    val tokensUsed = response.usage.totalTokens
                    
                    // Update usage stats
                    updateUsageStats(tokensUsed, responseTime)
                    
                    val aiResponse = AIResponse(
                        content = response.choices.firstOrNull()?.message?.content ?: "",
                        tokensUsed = tokensUsed,
                        responseTime = responseTime,
                        model = response.model,
                        metadata = mapOf(
                            "finish_reason" to (response.choices.firstOrNull()?.finishReason ?: ""),
                            "id" to response.id
                        )
                    )
                    
                    Result.success(aiResponse)
                },
                onFailure = { exception ->
                    logger.error(exception) { "Failed to generate response" }
                    Result.failure(exception)
                }
            )
            
        } catch (e: Exception) {
            logger.error(e) { "Error in generateResponse" }
            Result.failure(e)
        }
    }
    
    override suspend fun generateEmbeddings(texts: List<String>): Result<List<FloatArray>> {
        if (!config.isValid()) {
            return Result.failure(IllegalStateException("OpenAI API key not configured"))
        }
        
        return try {
            val request = EmbeddingRequest(input = texts)
            val result = client.createEmbeddings(request)
            
            result.fold(
                onSuccess = { response ->
                    val embeddings = response.data.map { embeddingData ->
                        embeddingData.embedding.map { it.toFloat() }.toFloatArray()
                    }
                    Result.success(embeddings)
                },
                onFailure = { exception ->
                    logger.error(exception) { "Failed to generate embeddings" }
                    Result.failure(exception)
                }
            )
            
        } catch (e: Exception) {
            logger.error(e) { "Error in generateEmbeddings" }
            Result.failure(e)
        }
    }
    
    override fun getUsageStats(): UsageStats {
        val avgResponseTime = if (totalRequests.get() > 0) {
            totalResponseTime.get().toDouble() / totalRequests.get()
        } else 0.0
        
        return UsageStats(
            totalRequests = totalRequests.get(),
            totalTokens = totalTokens.get(),
            averageResponseTime = avgResponseTime,
            totalCost = calculateCost(totalTokens.get()), // Approximate cost calculation
            requestsToday = totalRequests.get(), // Simplified - would need proper day tracking
            tokensToday = totalTokens.get()
        )
    }
    
    override suspend fun isHealthy(): Boolean {
        return try {
            client.testConnection()
        } catch (e: Exception) {
            logger.error(e) { "Health check failed" }
            false
        }
    }
    
    override fun getProviderName(): String = "OpenAI"
    
    private suspend fun updateUsageStats(tokens: Int, responseTime: Long) {
        mutex.withLock {
            totalRequests.incrementAndGet()
            totalTokens.addAndGet(tokens.toLong())
            totalResponseTime.addAndGet(responseTime)
        }
    }
    
    private fun calculateCost(tokens: Long): Double {
        // Approximate cost calculation for GPT-4 (this would need to be more sophisticated)
        val costPerToken = 0.00003 // $0.03 per 1K tokens (approximate)
        return tokens * costPerToken
    }
    
    fun close() {
        client.close()
    }
}