package org.manganesium.ai.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import org.manganesium.ai.config.AIConfig

/**
 * HTTP client for OpenAI API
 */
class OpenAIClient(private val config: AIConfig) {
    
    private val logger = KotlinLogging.logger {}
    
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson()
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
    }
    
    /**
     * Send a chat completion request to OpenAI
     */
    suspend fun createChatCompletion(request: ChatCompletionRequest): Result<ChatCompletionResponse> {
        return try {
            val response = httpClient.post("${config.openAIBaseUrl}/chat/completions") {
                header("Authorization", "Bearer ${config.openAIApiKey}")
                header("Content-Type", "application/json")
                setBody(request)
            }
            
            if (response.status.isSuccess()) {
                val responseBody = response.body<ChatCompletionResponse>()
                Result.success(responseBody)
            } else {
                val errorBody = response.body<String>()
                logger.error { "OpenAI API error: ${response.status} - $errorBody" }
                Result.failure(Exception("OpenAI API error: ${response.status} - $errorBody"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error calling OpenAI API" }
            Result.failure(e)
        }
    }
    
    /**
     * Create embeddings using OpenAI API
     */
    suspend fun createEmbeddings(request: EmbeddingRequest): Result<EmbeddingResponse> {
        return try {
            val response = httpClient.post("${config.openAIBaseUrl}/embeddings") {
                header("Authorization", "Bearer ${config.openAIApiKey}")
                header("Content-Type", "application/json")
                setBody(request)
            }
            
            if (response.status.isSuccess()) {
                val responseBody = response.body<EmbeddingResponse>()
                Result.success(responseBody)
            } else {
                val errorBody = response.body<String>()
                logger.error { "OpenAI API error: ${response.status} - $errorBody" }
                Result.failure(Exception("OpenAI API error: ${response.status} - $errorBody"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error calling OpenAI embeddings API" }
            Result.failure(e)
        }
    }
    
    /**
     * Test the connection to OpenAI API
     */
    suspend fun testConnection(): Boolean {
        return try {
            val testRequest = ChatCompletionRequest(
                model = "gpt-3.5-turbo",
                messages = listOf(
                    ChatMessage(role = "user", content = "Hello")
                ),
                maxTokens = 5
            )
            
            val result = createChatCompletion(testRequest)
            result.isSuccess
        } catch (e: Exception) {
            logger.error(e) { "Connection test failed" }
            false
        }
    }
    
    fun close() {
        httpClient.close()
    }
}

// Data classes for OpenAI API

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val maxTokens: Int? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val presencePenalty: Double? = null,
    val frequencyPenalty: Double? = null
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<ChatChoice>,
    val usage: Usage
)

@Serializable
data class ChatChoice(
    val index: Int,
    val message: ChatMessage,
    val finishReason: String? = null
)

@Serializable
data class Usage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

@Serializable
data class EmbeddingRequest(
    val model: String = "text-embedding-ada-002",
    val input: List<String>
)

@Serializable
data class EmbeddingResponse(
    val `object`: String,
    val data: List<EmbeddingData>,
    val model: String,
    val usage: Usage
)

@Serializable
data class EmbeddingData(
    val `object`: String,
    val embedding: List<Double>,
    val index: Int
)