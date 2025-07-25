package org.manganesium.ai.service

import mu.KotlinLogging
import org.manganesium.ai.config.AIConfig
import org.manganesium.ai.config.LLMConfig
import org.manganesium.ai.model.*
import org.mapdb.DB
import java.util.*

/**
 * Main AI service that orchestrates LLM interactions and conversation management
 */
class AIService(
    private val db: DB,
    private val config: AIConfig = AIConfig.fromEnvironment()
) {
    
    private val logger = KotlinLogging.logger {}
    
    private val llmService: LLMService = OpenAIService(config)
    private val conversationService = ConversationService(db)
    private val promptService = PromptService()
    
    /**
     * Process a user question and generate an AI response
     */
    suspend fun askQuestion(
        question: String,
        conversationId: String? = null,
        llmConfig: LLMConfig = LLMConfig.default()
    ): Result<AIResponse> {
        return try {
            logger.info { "Processing question: ${question.take(50)}..." }
            
            // Get or create conversation
            val conversation = if (conversationId != null) {
                conversationService.getConversation(conversationId)
            } else {
                null
            }
            
            // Prepare conversation context
            val context = conversation?.messages ?: emptyList()
            val limitedContext = promptService.prepareConversationContext(context, config.maxConversationHistory)
            
            // Create search prompt
            val searchPrompt = promptService.createSearchPrompt(question, limitedContext)
            
            // Generate response from LLM
            val result = llmService.generateResponse(searchPrompt, limitedContext, llmConfig)
            
            result.fold(
                onSuccess = { aiResponse ->
                    // If we have a conversation, add the messages
                    if (conversation != null) {
                        // Add user message
                        val userMessage = Message(
                            id = UUID.randomUUID().toString(),
                            conversationId = conversation.id,
                            role = MessageRole.USER,
                            content = question
                        )
                        conversationService.addMessage(conversation.id, userMessage)
                        
                        // Add assistant message
                        val assistantMessage = Message(
                            id = UUID.randomUUID().toString(),
                            conversationId = conversation.id,
                            role = MessageRole.ASSISTANT,
                            content = aiResponse.content,
                            sources = aiResponse.sources
                        )
                        conversationService.addMessage(conversation.id, assistantMessage)
                        
                        // Update response with conversation info
                        val updatedResponse = aiResponse.copy(
                            conversationId = conversation.id,
                            messageId = assistantMessage.id
                        )
                        
                        Result.success(updatedResponse)
                    } else {
                        Result.success(aiResponse)
                    }
                },
                onFailure = { exception ->
                    logger.error(exception) { "Failed to generate AI response" }
                    Result.failure(exception)
                }
            )
            
        } catch (e: Exception) {
            logger.error(e) { "Error in askQuestion" }
            Result.failure(e)
        }
    }
    
    /**
     * Create a new conversation
     */
    fun createConversation(title: String = "New Conversation"): Conversation {
        logger.info { "Creating new conversation: $title" }
        return conversationService.createConversation(title)
    }
    
    /**
     * Get a conversation by ID
     */
    fun getConversation(conversationId: String): Conversation? {
        return conversationService.getConversation(conversationId)
    }
    
    /**
     * Get list of conversations
     */
    fun getConversations(limit: Int = 50, offset: Int = 0): List<Conversation> {
        return conversationService.getConversations(limit, offset)
    }
    
    /**
     * Add a message to an existing conversation
     */
    suspend fun addMessageToConversation(
        conversationId: String,
        content: String,
        llmConfig: LLMConfig = LLMConfig.default()
    ): Result<AIResponse> {
        return askQuestion(content, conversationId, llmConfig)
    }
    
    /**
     * Delete a conversation
     */
    fun deleteConversation(conversationId: String): Boolean {
        logger.info { "Deleting conversation: $conversationId" }
        return conversationService.deleteConversation(conversationId)
    }
    
    /**
     * Generate embeddings for text
     */
    suspend fun generateEmbeddings(texts: List<String>): Result<List<FloatArray>> {
        return llmService.generateEmbeddings(texts)
    }
    
    /**
     * Get usage statistics
     */
    fun getUsageStats(): UsageStats {
        return llmService.getUsageStats()
    }
    
    /**
     * Check if AI service is healthy and properly configured
     */
    suspend fun isHealthy(): Boolean {
        return try {
            config.isValid() && llmService.isHealthy()
        } catch (e: Exception) {
            logger.error(e) { "Health check failed" }
            false
        }
    }
    
    /**
     * Get service information
     */
    fun getServiceInfo(): Map<String, Any> {
        return mapOf(
            "provider" to llmService.getProviderName(),
            "model" to config.defaultModel,
            "configured" to config.isValid(),
            "caching_enabled" to config.enableCaching,
            "max_conversation_history" to config.maxConversationHistory
        )
    }
    
    /**
     * Close the service and cleanup resources
     */
    fun close() {
        try {
            conversationService.close()
            if (llmService is OpenAIService) {
                llmService.close()
            }
            logger.info { "AI service closed successfully" }
        } catch (e: Exception) {
            logger.error(e) { "Error closing AI service" }
        }
    }
}