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

class Crawler(private val crawlerDAO: CrawlerDAO) {
    val visitedUrls = mutableSetOf<String>()
    val urlQueue = ConcurrentLinkedQueue<String>()

    // Create our own reference to CrawlerService with the DAO passed in
    private val crawlerService = CrawlerService(crawlerDAO, this)

    /**
     * Fetch the HTML content of the given URL using Jsoup.
     *
     * @param url The URL to fetch.
     * @return Jsoup Document containing the page's HTML, or null if fetch fails.
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
     * Extract links (absolute URLs) from the provided Document.
     *
     * @param doc The fetched Jsoup Document.
     * @return A list of absolute URLs found in the document.
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
     * Starts the crawling process from the given start URLs,
     * with an optional maxDepth for BFS traversal, stopping exactly at maxPages.
     *
     * @param startUrls URLs to begin crawling from
     * @param maxDepth Number of levels to traverse
     * @param maxPages Maximum number of pages to crawl
     * @param indexer The indexer instance to use for indexing pages
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
     * Shutdown the crawler service (to wait for indexing threads)
     */
    fun shutdown() {
        crawlerService.shutdown()
    }

    /**
     * Close the underlying data access object
     */
    fun close() {
        crawlerDAO.close()
    }
}