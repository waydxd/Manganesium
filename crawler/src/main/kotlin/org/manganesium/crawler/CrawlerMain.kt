package org.manganesium.crawler

import dataAccessObject.CrawlerDAO
import io.github.oshai.kotlinlogging.KotlinLogging
import org.manganesium.indexer.Indexer

private val logger = KotlinLogging.logger {}

fun main() {
    logger.info { "[Main:main] Starting the crawler application" }

    // Initialize DAO with a path to a DB file
    val crawlerDao = CrawlerDAO("crawler.db")

    // File("indexer.db").delete()
    // Create the indexer
    val indexer = Indexer()

    // Create the crawler (which internally creates the service)
    val crawler = Crawler(crawlerDao)

    // Define your starting URLs
    val startUrls = listOf(
        "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm"
    )

    // Start crawling directly from crawler
    logger.info { "[Main:main] Starting the crawling process" }
    crawler.startCrawling(startUrls, maxDepth = 3, maxPages = 30, indexer)

    // Close the DAO to commit and release resources
    crawler.close()

    logger.info { "[Main:main] Crawling process completed" }
    logger.info { "[Main:main] Total pages visited: ${crawler.visitedUrls.size}" }

    println("Crawling summary:")
    println("Total pages visited: ${crawler.visitedUrls.size}")
}