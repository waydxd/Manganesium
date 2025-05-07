package app

import api.route.ServiceHolder
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import api.route.configureRouting
import mu.KotlinLogging

class ApiServer {
    private val logger = KotlinLogging.logger {}
    private val port = System.getenv("PORT")?.toIntOrNull() ?: 8080

    fun start() {
        logger.info { "Starting server on port $port" }

        embeddedServer(Netty, port = port) {
            install(ContentNegotiation) {
                jackson {
                    registerKotlinModule()
                    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                }
            }

            configureRouting()
        }.start(wait = true)
    }
}

fun main() {
    val logger = KotlinLogging.logger {}

    try {
        logger.info { "Starting Crawler in background thread..." }
        // Create and start a thread for running the crawler
        val crawlerThread = Thread {
            try {
                org.manganesium.crawler.main()
                logger.info { "Crawler completed." }
                // Mark crawler as complete to initialize the AppService
                ServiceHolder.markCrawlingComplete()
            } catch (e: Exception) {
                logger.error(e) { "Error running crawler" }
            }
        }

        // Set as daemon thread so it won't prevent application shutdown
        crawlerThread.isDaemon = true
        crawlerThread.start()

    } catch (e: Exception) {
        logger.error(e) { "Error setting up crawler thread" }
    }

    logger.info { "Starting API server..." }

    try {
        val server = ApiServer()
        server.start()
    } catch (e: Exception) {
        logger.error(e) { "Error starting API server" }
    }


}