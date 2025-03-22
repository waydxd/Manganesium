package org.manganesium.crawler

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentLinkedQueue
import org.manganesium.crawler.services.CrawlerService
import dataAccessObject.CrawlerDAO
import org.manganesium.indexer.Indexer

private val logger = KotlinLogging.logger {}

/**
 * The [Crawler] class is responsible for performing web crawling operations.
 *
 * It provides methods to fetch web pages, extract links from the pages, initiate a crawling
 * process using a breadth-first search (BFS) algorithm, and manage the lifecycle of related resources.
 *
 * @property crawlerDAO the data access object used for storing and retrieving crawler-related data.
 */
class Crawler(private val crawlerDAO: CrawlerDAO) {

    /**
     * Set of URLs that have already been visited during the crawling process.
     */
    val visitedUrls = mutableSetOf<String>()

    /**
     * A thread-safe queue to store URLs pending for crawling.
     */
    val urlQueue = ConcurrentLinkedQueue<String>()

    // Create our own reference to CrawlerService with the DAO passed in
    private val crawlerService = CrawlerService(crawlerDAO, this)

    /**
     * Fetches the HTML content of the given URL using Jsoup.
     *
     * This method attempts to connect to the specified URL using Jsoup and retrieve its HTML content.
     * If the connection fails (for instance, due to an [IOException]), the method logs the error and returns null.
     *
     * @param url the URL to be fetched.
     * @return a [Document] containing the page's HTML if the fetch is successful, or null if it fails.
     */
    fun fetchPage(url: String): Document? {
        return try {
            logger.debug { "[Crawler:fetchPage] Fetching page from URL: $url" }
            Jsoup.connect(url).get()
        } catch (e: IOException) {
            logger.error { "[Crawler:fetchPage] Failed to fetch $url: ${e.message}" }
            null
        }
    }

    /**
     * Extracts all absolute URLs (links) from the given HTML [Document].
     *
     * This method searches for anchor elements with an href attribute and converts each to an absolute URL.
     * Only non-empty URLs are added to the returned list.
     *
     * @param doc the Jsoup [Document] from which to extract links.
     * @return a [List] of Strings representing the absolute URLs found in the document.
     */
    fun extractLinks(doc: Document): List<String> {
        logger.debug { "[Crawler:extractLinks] Extracting links from document" }
        val links = mutableListOf<String>()
        val anchorElements = doc.select("a[href]")
        for (element in anchorElements) {
            val absoluteUrl = element.attr("abs:href")
            if (absoluteUrl.isNotEmpty()) {
                links.add(absoluteUrl)
            }
        }
        logger.debug { "[Crawler:extractLinks] Extracted ${links.size} links from the document" }
        return links
    }

    /**
     * Starts the crawling process from the given starting URLs using breadth-first search (BFS).
     *
     * The crawling process traverses the web up to a specified number of levels ([maxDepth]) or until a maximum
     * number of pages ([maxPages]) have been visited. For each page, the crawler fetches the page, extracts its links,
     * and then enqueues the extracted URLs for further crawling. The [Indexer] is used to index page content.
     *
     * @param startUrls the list of initial URLs to begin crawling from.
     * @param maxDepth the maximum depth to traverse during the crawl.
     * @param maxPages the maximum number of pages to visit during the crawl.
     * @param indexer the indexer instance used to index pages as they are crawled.
     */
    fun startCrawling(startUrls: List<String>, maxDepth: Int, maxPages: Int, indexer: Indexer) {
        logger.info { "[Crawler:startCrawling] Starting crawling process with ${startUrls.size} start URLs and maxDepth=$maxDepth" }

        // Initialize the queue
        urlQueue.addAll(startUrls)

        var currentDepth = 0
        while (currentDepth < maxDepth && urlQueue.isNotEmpty() && visitedUrls.size < maxPages) {
            logger.debug { "[Crawler:startCrawling] Processing depth level $currentDepth" }

            val levelSize = urlQueue.size
            for (i in 0 until levelSize) {
                // Check if we've reached the page limit before processing the next URL
                if (visitedUrls.size >= maxPages) {
                    logger.debug { "[Crawler:startCrawling] Reached maxPages ($maxPages), stopping crawl" }
                    break
                }

                val url = urlQueue.poll() ?: continue
                if (!visitedUrls.contains(url)) {
                    visitedUrls.add(url)
                    logger.debug { "[Crawler:startCrawling] Crawling URL: $url" }
                    crawlerService.crawlSinglePage(url, indexer)
                }
            }
            currentDepth++
            // Check again after the level to break the outer loop if needed
            if (visitedUrls.size >= maxPages) {
                logger.debug { "[Crawler:startCrawling] Max pages reached at depth $currentDepth" }
                break
            }
        }

        logger.info { "[Crawler:startCrawling] Crawling process completed. Visited ${visitedUrls.size} URLs." }
    }

    /**
     * Shuts down the crawler service.
     *
     * This method delegates to the [CrawlerService] shutdown method, allowing indexing tasks running
     * on separate threads to finish cleanly.
     */
    fun shutdown() {
        crawlerService.shutdown()
    }

    /**
     * Closes the underlying data access object.
     *
     * This method should be called once crawling is complete to commit changes and release any resources held
     * by the [CrawlerDAO].
     */
    fun close() {
        crawlerDAO.close()
    }
}