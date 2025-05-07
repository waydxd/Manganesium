package api.route

import app.api.model.SearchRequest
import app.service.AppService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicReference

// Singleton to hold AppService instance
object ServiceHolder {
    private val appServiceRef = AtomicReference<AppService?>(null)
    private val logger = KotlinLogging.logger {}

    @Volatile
    private var isCrawlingComplete = false

    fun markCrawlingComplete() {
        isCrawlingComplete = true
        initializeService()
    }

    fun getService(): AppService? {
        if (isCrawlingComplete && appServiceRef.get() == null) {
            initializeService()
        }
        return appServiceRef.get()
    }

    private fun initializeService() {
        if (appServiceRef.get() == null) {
            try {
                appServiceRef.compareAndSet(null, AppService())
                logger.info { "AppService initialized successfully" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to initialize AppService" }
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

                    val request = app.api.model.SearchRequest(query, limit, offset)
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
                call.respond(
                    HttpStatusCode.OK,
                    mapOf("status" to "UP", "search" to serviceStatus)
                )
            }
        }
    }
}