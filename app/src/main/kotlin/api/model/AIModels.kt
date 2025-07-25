package app.api.model

import org.manganesium.ai.model.Source

/**
 * Request model for AI ask endpoint
 */
data class AIAskRequest(
    val question: String,
    val conversationId: String? = null,
    val model: String? = null,
    val temperature: Double? = null,
    val maxTokens: Int? = null
)

/**
 * Response model for AI ask endpoint
 */
data class AIAskResponse(
    val answer: String,
    val sources: List<Source> = emptyList(),
    val conversationId: String? = null,
    val messageId: String? = null,
    val tokensUsed: Int = 0,
    val responseTime: Long = 0,
    val model: String = "",
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Request model for creating conversations
 */
data class CreateConversationRequest(
    val title: String = "New Conversation"
)

/**
 * Request model for adding messages to conversations
 */
data class AddMessageRequest(
    val content: String,
    val model: String? = null,
    val temperature: Double? = null,
    val maxTokens: Int? = null
)

/**
 * Response model for conversation operations
 */
data class ConversationResponse(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int = 0
)

/**
 * Response model for conversation with messages
 */
data class ConversationWithMessagesResponse(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messages: List<MessageResponse>
)

/**
 * Response model for individual messages
 */
data class MessageResponse(
    val id: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val sources: List<Source> = emptyList()
)

/**
 * Response model for AI service health and statistics
 */
data class AIHealthResponse(
    val status: String,
    val configured: Boolean,
    val provider: String,
    val model: String,
    val usageStats: AIUsageStats? = null
)

/**
 * Usage statistics for API responses
 */
data class AIUsageStats(
    val totalRequests: Long,
    val totalTokens: Long,
    val averageResponseTime: Double,
    val requestsToday: Long,
    val tokensToday: Long
)