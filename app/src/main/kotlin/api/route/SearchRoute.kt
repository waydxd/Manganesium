package api.route

import app.api.model.SearchRequest
import app.service.AppService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging
import org.manganesium.ai.service.AIService
import org.mapdb.DBMaker
import java.util.concurrent.atomic.AtomicReference

// Singleton to hold AppService instance
object ServiceHolder {
    private val appServiceRef = AtomicReference<AppService?>(null)
    private val aiServiceRef = AtomicReference<AIService?>(null)
    private val logger = KotlinLogging.logger {}

    @Volatile
    private var isCrawlingComplete = false

    fun markCrawlingComplete() {
        isCrawlingComplete = true
        initializeServices()
    }

    fun getService(): AppService? {
        if (isCrawlingComplete && appServiceRef.get() == null) {
            initializeServices()
        }
        return appServiceRef.get()
    }

    fun getAIService(): AIService? {
        if (isCrawlingComplete && aiServiceRef.get() == null) {
            initializeServices()
        }
        return aiServiceRef.get()
    }

    private fun initializeServices() {
        if (appServiceRef.get() == null) {
            try {
                appServiceRef.compareAndSet(null, AppService())
                logger.info { "AppService initialized successfully" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to initialize AppService" }
            }
        }

        if (aiServiceRef.get() == null) {
            try {
                // Create AI database - reuse existing database or create separate one
                val aiDB = DBMaker.fileDB("ai.db")
                    .transactionEnable()
                    .closeOnJvmShutdown()
                    .make()

                val aiService = AIService(aiDB)
                aiServiceRef.compareAndSet(null, aiService)
                logger.info { "AIService initialized successfully" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to initialize AIService: ${e.message}" }
                // AI service is optional, so we continue without it
            }
        }
    }
}

fun Application.configureRouting() {
    val logger = KotlinLogging.logger {}

    routing {
        route("/api") {
            get("/search") {
                val service = ServiceHolder.getService()
                if (service == null) {
                    call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        mapOf("error" to "Search service is initializing. Please try again later.")
                    )
                    return@get
                }

                try {
                    val query = call.request.queryParameters["query"] ?: ""
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                    val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

                    logger.info { "Received search request: $query" }

                    val request = SearchRequest(query, limit, offset)
                    val results = service.search(request)
                    call.respond(HttpStatusCode.OK, results)
                } catch (e: Exception) {
                    logger.error(e) { "Error processing search request" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to (e.message ?: "Unknown error"))
                    )
                }
            }

            get("/health") {
                val serviceStatus = if (ServiceHolder.getService() != null) "READY" else "INITIALIZING"
                val aiStatus = if (ServiceHolder.getAIService() != null) "READY" else "UNAVAILABLE"
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "status" to "UP", 
                        "search" to serviceStatus,
                        "ai" to aiStatus
                    )
                )
            }

            // Add AI routes if AI service is available
            val aiService = ServiceHolder.getAIService()
            if (aiService != null) {
                configureAIRoutes(aiService)
            } else {
                // Provide fallback endpoints that indicate AI is not available
                route("/v2/ai") {
                    get("/health") {
                        call.respond(
                            HttpStatusCode.ServiceUnavailable,
                            mapOf("error" to "AI service is not configured or unavailable")
                        )
                    }
                    post("/ask") {
                        call.respond(
                            HttpStatusCode.ServiceUnavailable,
                            mapOf("error" to "AI service is not configured. Please set OPENAI_API_KEY environment variable.")
                        )
                    }
                }
            }
        }
    }
}