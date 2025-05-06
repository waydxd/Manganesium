package api.route

import app.api.model.SearchRequest
import app.service.AppService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import mu.KotlinLogging

fun Application.configureSearchRoute() {
//    val logger = KotlinLogging.logger {}
//    val appService = AppService()
//
//    routing {
//        get("/search") {
//            try {
//                val request = SearchRequest(
//                    query = call.parameters["query"]?.trim() ?: return@get call.respond(HttpStatusCode.BadRequest),
//                    limit = call.parameters["limit"]?.toIntOrNull() ?: 10,
//                    offset = call.parameters["offset"]?.toIntOrNull() ?: 0
//                )
//                logger.debug { "Processing search: $request" }
//                val results = appService.search(request)
//                call.respond(results)
//            } catch (e: IllegalArgumentException) {
//                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
//            }
//        }
//    }

}