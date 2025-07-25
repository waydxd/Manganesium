package org.manganesium.ai.model

import kotlinx.serialization.Serializable

/**
 * Represents a conversation session between users and the AI system
 */
@Serializable
data class Conversation(
    val id: String,
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messages: List<Message> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Represents individual messages within a conversation
 */
@Serializable
data class Message(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sources: List<Source> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Message roles in conversation
 */
@Serializable
enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

/**
 * Enhanced source attribution for citations
 */
@Serializable
data class Source(
    val id: String,
    val url: String,
    val title: String,
    val snippet: String = "",
    val relevanceScore: Double = 0.0,
    val lastModified: String = "",
    val contentType: String = "text/html",
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Structured AI responses with metadata
 */
@Serializable
data class AIResponse(
    val content: String,
    val sources: List<Source> = emptyList(),
    val tokensUsed: Int = 0,
    val responseTime: Long = 0,
    val confidence: Double? = null,
    val model: String = "",
    val conversationId: String? = null,
    val messageId: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Usage statistics for AI service
 */
@Serializable
data class UsageStats(
    val totalRequests: Long = 0,
    val totalTokens: Long = 0,
    val averageResponseTime: Double = 0.0,
    val totalCost: Double = 0.0,
    val requestsToday: Long = 0,
    val tokensToday: Long = 0
)