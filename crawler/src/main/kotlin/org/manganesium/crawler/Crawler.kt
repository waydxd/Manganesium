package org.manganesium.crawler

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentLinkedQueue
import org.manganesium.crawler.services.CrawlerService
import dataAccessObject.CrawlerDAO

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
     * with an optional maxDepth for BFS traversal.
     *
     * @param startUrls URLs to begin crawling from
     * @param maxDepth Number of levels to traverse
     */
    fun startCrawling(startUrls: List<String>, maxDepth: Int, maxPages: Int) {
        logger.info { "[Crawler:startCrawling] Starting crawling process with ${startUrls.size} start URLs and maxDepth=$maxDepth" }

        // Initialize the queue
        urlQueue.addAll(startUrls)

        var currentDepth = 0
        while (currentDepth < maxDepth && urlQueue.isNotEmpty() && visitedUrls.size < maxPages ) {
            logger.debug { "[Crawler:startCrawling] Processing depth level $currentDepth" }

            val levelSize = urlQueue.size
            repeat(levelSize) {
                val url = urlQueue.poll() ?: return@repeat
                if (!visitedUrls.contains(url)) {
                    visitedUrls.add(url)
                    logger.debug { "[Crawler:startCrawling] Crawling URL: $url" }
                    crawlerService.crawlSinglePage(url)
                }
            }
            currentDepth++
        }

        logger.info { "[Crawler:startCrawling] Crawling process completed. Visited ${visitedUrls.size} URLs." }
    }

    /**
     * Close the underlying data access object
     */
    fun close() {
        crawlerDAO.close()
    }

    /*
    /**
     * Extract keywords from the document by naive splitting on whitespace.
     * A real application might:
     * - Tokenize more robustly
     * - Handle punctuation and special characters
     * - Filter out stop words, etc.
     */
    fun extractKeywords(doc: Document): List<String> {
        logger.debug { "[Crawler:extractKeywords] Extracting keywords from document" }
        val textContent = doc.body()?.text() ?: ""
        val keywords = textContent
            .split("\\s+".toRegex())
            .map { it.lowercase() }
            .filter { it.isNotBlank() }
        logger.debug { "[Crawler:extractKeywords] Extracted ${keywords.size} keywords from the document" }
        return keywords
    }
    */
}