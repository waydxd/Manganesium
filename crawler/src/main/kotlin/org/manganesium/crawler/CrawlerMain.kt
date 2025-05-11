package org.manganesium.crawler

import dataAccessObject.CrawlerDAO
import io.github.oshai.kotlinlogging.KotlinLogging
import org.manganesium.indexer.Indexer

private val logger = KotlinLogging.logger {}

/**
 * The main entry point for the crawler application.
 *
 * This function sets up and initializes the various components needed to perform
 * web crawling and indexing:
 *
 * 1. A [CrawlerDAO] is created to handle data access operations (using a file-based database).
 * 2. An [Indexer] is instantiated to index page content extracted during crawling.
 * 3. A [Crawler] is then created with the DAO, which internally creates a crawler service.
 * 4. A starting URL list is defined, which the [Crawler] uses as its initial set of pages.
 * 5. The crawler is started with a specified maximum depth and maximum page limit.
 * 6. After crawling, the crawler's shutdown routine is called to wait for indexing tasks.
 * 7. Finally, both the [CrawlerDAO] and [Indexer] are closed to commit any changes and
 *    release any held resources.
 *
 * Throughout the process, logging is performed using [KotlinLogging] to provide runtime
 * information about the application's progress.
 *
 * @sample
 * Running this function will print a summary of the total pages visited:
 *
 *     Crawling summary:
 *     Total pages visited: <number_of_pages>
 */
fun main() {
    logger.info { "[Main:main] Starting the crawler application" }

    // Initialize DAO with a path to a DB file
    val crawlerDao = CrawlerDAO("crawler.db")

    // Create the indexer
    val indexer = Indexer()

    // Create the crawler (which internally creates the service)
    val crawler = Crawler(crawlerDao)

    // Define your starting URLs
    val startUrls = listOf(
        "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm"
    )

    // Start crawling:
    //  - maxDepth = 3 (or any depth you need)
    //  - maxPages = 30 so it stops exactly after 30 pages
    logger.info { "[Main:main] Starting the crawling process" }
    crawler.startCrawling(startUrls, maxDepth = 10, maxPages = 300, indexer)

    // Wait for all indexing tasks to complete
    crawler.shutdown()

    // Close the DAO to commit and release resources
    crawler.close()
    indexer.close()

    logger.info { "[Main:main] Crawling and indexing process completed" }
    logger.info { "[Main:main] Total pages visited: ${crawler.visitedUrls.size}" }

    println("Crawling summary:")
    println("Total pages visited: ${crawler.visitedUrls.size}")
}