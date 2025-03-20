package org.manganesium.crawler

import dataAccessObject.CrawlerDAO
import org.manganesium.crawler.services.CrawlerService
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main() {

    logger.info { "[Main:main] Starting the crawler application" }

    // Initialize DAO with a path to a DB file
    val crawlerDao = CrawlerDAO("crawler.db")

    // Create the crawler service
    val crawlerService = CrawlerService(crawlerDao)

    // Define your starting URLs
    val startUrls = listOf(
        "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm"
    )

    // Start crawling
    logger.info { "[Main:main] Starting the crawling process" }
    crawlerService.startCrawling(startUrls, maxDepth = 2)

    // Close the DAO to commit and release resources
    crawlerService.close()

    logger.info { "[Main:main] Crawling process completed" }
    logger.info { "[Main:main] Total pages visited: ${crawlerService.visitedUrls.size}" }

    println("Crawling summary:")
    println("Total pages visited: ${crawlerService.visitedUrls.size}")
}