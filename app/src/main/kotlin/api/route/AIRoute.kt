package api.route

import app.api.model.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging
import org.manganesium.ai.config.LLMConfig
import org.manganesium.ai.model.MessageRole
import org.manganesium.ai.service.AIService

/**
 * Configure AI-related routes
 */
fun Route.configureAIRoutes(aiService: AIService) {
    val logger = KotlinLogging.logger {}

    route("/v2/ai") {
        
        // Main AI ask endpoint
        post("/ask") {
            try {
                val request = call.receive<AIAskRequest>()
                logger.info { "AI ask request: ${request.question.take(50)}..." }

                // Build LLM config from request parameters
                val llmConfig = LLMConfig(
                    model = request.model ?: "gpt-4",
                    temperature = request.temperature ?: 0.7,
                    maxTokens = request.maxTokens ?: 1000
                )

                val result = aiService.askQuestion(
                    question = request.question,
                    conversationId = request.conversationId,
                    llmConfig = llmConfig
                )

                result.fold(
                    onSuccess = { aiResponse ->
                        val response = AIAskResponse(
                            answer = aiResponse.content,
                            sources = aiResponse.sources,
                            conversationId = aiResponse.conversationId,
                            messageId = aiResponse.messageId,
                            tokensUsed = aiResponse.tokensUsed,
                            responseTime = aiResponse.responseTime,
                            model = aiResponse.model,
                            metadata = aiResponse.metadata
                        )
                        call.respond(HttpStatusCode.OK, response)
                    },
                    onFailure = { exception ->
                        logger.error(exception) { "Error processing AI ask request" }
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to (exception.message ?: "Unknown error"))
                        )
                    }
                )

            } catch (e: Exception) {
                logger.error(e) { "Error in AI ask endpoint" }
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid request format")
                )
            }
        }

        // AI health endpoint
        get("/health") {
            try {
                val isHealthy = aiService.isHealthy()
                val serviceInfo = aiService.getServiceInfo()
                val usageStats = aiService.getUsageStats()

                val response = AIHealthResponse(
                    status = if (isHealthy) "UP" else "DOWN",
                    configured = serviceInfo["configured"] as Boolean,
                    provider = serviceInfo["provider"] as String,
                    model = serviceInfo["model"] as String,
                    usageStats = AIUsageStats(
                        totalRequests = usageStats.totalRequests,
                        totalTokens = usageStats.totalTokens,
                        averageResponseTime = usageStats.averageResponseTime,
                        requestsToday = usageStats.requestsToday,
                        tokensToday = usageStats.tokensToday
                    )
                )

                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                logger.error(e) { "Error in AI health endpoint" }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Health check failed")
                )
            }
        }
    }

    route("/v2/conversations") {
        
        // Create new conversation
        post {
            try {
                val request = call.receive<CreateConversationRequest>()
                logger.info { "Creating conversation: ${request.title}" }

                val conversation = aiService.createConversation(request.title)
                val response = ConversationResponse(
                    id = conversation.id,
                    title = conversation.title,
                    createdAt = conversation.createdAt,
                    updatedAt = conversation.updatedAt,
                    messageCount = conversation.messages.size
                )

                call.respond(HttpStatusCode.Created, response)
            } catch (e: Exception) {
                logger.error(e) { "Error creating conversation" }
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid request format")
                )
            }
        }

        // Get conversations list
        get {
            try {
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

                val conversations = aiService.getConversations(limit, offset)
                val response = conversations.map { conversation ->
                    ConversationResponse(
                        id = conversation.id,
                        title = conversation.title,
                        createdAt = conversation.createdAt,
                        updatedAt = conversation.updatedAt,
                        messageCount = conversation.messages.size
                    )
                }

                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                logger.error(e) { "Error getting conversations" }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to load conversations")
                )
            }
        }

        // Get specific conversation with messages
        get("/{id}") {
            try {
                val conversationId = call.parameters["id"] ?: ""
                val conversation = aiService.getConversation(conversationId)

                if (conversation == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Conversation not found")
                    )
                    return@get
                }

                val response = ConversationWithMessagesResponse(
                    id = conversation.id,
                    title = conversation.title,
                    createdAt = conversation.createdAt,
                    updatedAt = conversation.updatedAt,
                    messages = conversation.messages.map { message ->
                        MessageResponse(
                            id = message.id,
                            role = message.role.name.lowercase(),
                            content = message.content,
                            timestamp = message.timestamp,
                            sources = message.sources
                        )
                    }
                )

                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                logger.error(e) { "Error getting conversation" }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to load conversation")
                )
            }
        }

        // Add message to conversation
        post("/{id}/messages") {
            try {
                val conversationId = call.parameters["id"] ?: ""
                val request = call.receive<AddMessageRequest>()

                // Check if conversation exists
                val conversation = aiService.getConversation(conversationId)
                if (conversation == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Conversation not found")
                    )
                    return@post
                }

                // Build LLM config
                val llmConfig = LLMConfig(
                    model = request.model ?: "gpt-4",
                    temperature = request.temperature ?: 0.7,
                    maxTokens = request.maxTokens ?: 1000
                )

                val result = aiService.addMessageToConversation(
                    conversationId = conversationId,
                    content = request.content,
                    llmConfig = llmConfig
                )

                result.fold(
                    onSuccess = { aiResponse ->
                        val response = AIAskResponse(
                            answer = aiResponse.content,
                            sources = aiResponse.sources,
                            conversationId = aiResponse.conversationId,
                            messageId = aiResponse.messageId,
                            tokensUsed = aiResponse.tokensUsed,
                            responseTime = aiResponse.responseTime,
                            model = aiResponse.model,
                            metadata = aiResponse.metadata
                        )
                        call.respond(HttpStatusCode.OK, response)
                    },
                    onFailure = { exception ->
                        logger.error(exception) { "Error adding message to conversation" }
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to (exception.message ?: "Unknown error"))
                        )
                    }
                )

            } catch (e: Exception) {
                logger.error(e) { "Error in add message endpoint" }
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid request format")
                )
            }
        }

        // Delete conversation
        delete("/{id}") {
            try {
                val conversationId = call.parameters["id"] ?: ""
                val success = aiService.deleteConversation(conversationId)

                if (success) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Conversation not found or could not be deleted")
                    )
                }
            } catch (e: Exception) {
                logger.error(e) { "Error deleting conversation" }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to delete conversation")
                )
            }
        }
    }
}