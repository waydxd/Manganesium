package api.route

import app.api.model.SearchRequest
import app.service.AppService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import mu.KotlinLogging

fun Application.configureRouting() {
    val logger = KotlinLogging.logger {}
    routing {
        route("/api") {
            get("/search") {
                try {
                    val query = call.request.queryParameters["query"] ?: ""
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 1
                    val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 10

                    logger.info { "Received search request: $query" }

                    val request = SearchRequest(query, limit, offset)
                    val app = AppService()
                    val results = app.search(request)
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
                call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
            }
        }
    }
}