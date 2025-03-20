package org.manganesium.crawler.services

import dataAccessObject.CrawlerDAO
import models.Page
import org.jsoup.nodes.Document
import org.manganesium.crawler.Crawler
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class CrawlerService (val crawlerDAO: CrawlerDAO){
    //private val dbFile = File("manganesium.db")
    //val crawlerDao = CrawlerDAO(dbFile)


    private val crawler = Crawler()

    // Set of visited URLs to avoid cycles or repeated crawls
    private val visitedUrls = mutableSetOf<String>()

    // Basic queue for BFS-like crawling
    private val urlQueue = ConcurrentLinkedQueue<String>()

    /**
     * Starts the crawling process from the given start URLs,
     * with an optional maxDepth for BFS traversal.
     *
     * @param startUrls URLs to begin crawling from
     * @param maxDepth Number of levels to traverse
     */
    fun startCrawling(startUrls: List<String>, maxDepth: Int = 2) {
        logger.info { "Starting crawling process with ${startUrls.size} start URLs and maxDepth=$maxDepth" }

        // Initialize the queue
        urlQueue.addAll(startUrls)

        var currentDepth = 0
        while (currentDepth < maxDepth && urlQueue.isNotEmpty()) {
            logger.debug { "Processing depth level $currentDepth" }

            val levelSize = urlQueue.size
            // Process the current "layer" of URLs
            repeat(levelSize) {
                val url = urlQueue.poll() ?: return@repeat
                if (!visitedUrls.contains(url)) {
                    visitedUrls.add(url)
                    logger.debug { "Crawling URL: $url" }
                    crawlSinglePage(url)
                }
            }
            currentDepth++
        }

        logger.info { "Crawling process completed. Visited ${visitedUrls.size} URLs." }
    }

    /**
     * Crawl a single page (fetch, parse, store in DB, enqueue child links).
     *
     * @param url The page URL to crawl
     */
    private fun crawlSinglePage(url: String) {
        logger.debug { "Fetching document from URL: $url" }
        val document = crawler.fetchPage(url) ?: run {
            logger.error { "Failed to fetch document from URL: $url" }
            return
        }

        logger.debug { "Storing URL to page ID mapping for URL: $url" }
        val pageId = crawlerDAO.storeUrlToPageIdMapping(url)
        logger.debug { "Stored URL to page ID mapping. Page ID: $pageId" }

        // Extract raw word list from the Document
        logger.debug { "Extracting keywords from document" }
        val rawKeywords = crawler.extractKeywords(document)
        logger.debug { "Extracted ${rawKeywords.size} raw keywords from the document" }

        // Convert it to a frequency map
        logger.debug { "Computing keyword frequencies" }
        val keywordFrequencies = computeKeywordFrequencies(rawKeywords)
        logger.debug { "Computed keyword frequencies for ${keywordFrequencies.size} unique keywords" }

        // (Optional) store keywords in forward index
        logger.debug { "Storing page keywords in forward index" }
        crawlerDAO.storePageKeywords(pageId, rawKeywords)
        logger.debug { "Stored page keywords in forward index" }

        // Extract links
        logger.debug { "Extracting links from document" }
        val links = crawler.extractLinks(document)
        logger.debug { "Extracted ${links.size} links from the document" }

        // Convert links to child page IDs
        logger.debug { "Storing child URLs to page ID mappings" }
        val childPageIds = links.map { childUrl ->
            crawlerDAO.storeUrlToPageIdMapping(childUrl)
        }
        logger.debug { "Stored ${childPageIds.size} child URLs to page ID mappings" }

        // Store the parent-child relationships
        logger.debug { "Storing parent-child relationships" }
        crawlerDAO.storeParentChildLinks(pageId, childPageIds)
        logger.debug { "Stored parent-child relationships" }

        // Build and store the Page model
        logger.debug { "Storing page properties" }
        storePageProperties(pageId, document, keywordFrequencies, links, url)
        logger.debug { "Stored page properties" }

        // Enqueue child links for further crawling
        logger.debug { "Enqueuing child URLs for further crawling" }
        links.forEach { if (!visitedUrls.contains(it)) urlQueue.offer(it) }
        logger.debug { "Enqueued ${links.size} child URLs for further crawling" }
    }

    /**
     * Given a list of keywords, compute frequencies (word -> count).
     */
    private fun computeKeywordFrequencies(words: List<String>): Map<String, Int> {
        logger.debug { "Computing keyword frequencies for ${words.size} words" }
        val freqMap = mutableMapOf<String, Int>()
        for (word in words) {
            freqMap[word] = freqMap.getOrDefault(word, 0) + 1
        }
        logger.debug { "Computed frequencies for ${freqMap.size} unique keywords" }
        return freqMap
    }

    /**
     * Create a Page object aligned with your data model, then store it with the DAO.
     *
     * data class Page(
     *   val id: Long,
     *   val url: String,
     *   val title: String? = null,
     *   val content: String = "",
     *   val lastModified: String? = null,
     *   val size: Int = 0,
     *   val keywords: Map<String, Int> = emptyMap(),
     *   val links: List<String> = emptyList()
     * )
     */
    private fun storePageProperties(
        pageId: String,
        doc: Document,
        keywordFrequencies: Map<String, Int>,
        links: List<String>,
        url: String
    ) {
        logger.debug { "Creating Page object for URL: $url" }
        val title = doc.title().takeIf { it.isNotBlank() }
        val content = doc.body()?.text() ?: ""
        val lastModified = Instant.now().toString()
        val size = doc.outerHtml().length

        val page = Page(
            id = 0L,
            url = url,
            title = title,
            content = content,
            lastModified = lastModified,
            size = size,
            keywords = keywordFrequencies,
            links = links
        )

        logger.debug { "Storing Page object in DAO" }
        crawlerDAO.storePageProperties(pageId, page)
        logger.debug { "Stored Page object in DAO" }
    }

    /**
     * Demonstration of a concurrent approach (optional).
     * BFS with a thread pool, for those who want parallel crawling.
     */
    fun startCrawlingConcurrently(startUrls: List<String>, maxThreads: Int = 4, maxDepth: Int = 2) {
        logger.info { "Starting concurrent crawling process with ${startUrls.size} start URLs, maxThreads=$maxThreads, and maxDepth=$maxDepth" }

        val executor = Executors.newFixedThreadPool(maxThreads)
        urlQueue.addAll(startUrls)

        var currentDepth = 0
        while (currentDepth < maxDepth && urlQueue.isNotEmpty()) {
            logger.debug { "Processing depth level $currentDepth" }

            val levelSize = urlQueue.size
            repeat(levelSize) {
                val url = urlQueue.poll() ?: return@repeat
                executor.execute {
                    if (!visitedUrls.contains(url)) {
                        visitedUrls.add(url)
                        logger.debug { "Crawling URL: $url" }
                        crawlSinglePage(url)
                    }
                }
            }
            currentDepth++
        }

        // Shut down the executor once done
        executor.shutdown()
        logger.info { "Concurrent crawling process completed. Visited ${visitedUrls.size} URLs." }
    }

    /**
     * Cleanly close the DAO once crawling is complete.
     */
    fun close() {
        crawlerDAO.close()
    }
}