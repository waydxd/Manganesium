package org.manganesium.crawler.services

import dataAccessObject.CrawlerDAO
import models.Page
import org.jsoup.nodes.Document
import org.manganesium.crawler.Crawler
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.logging.Logger

// Initialize the logger
private val logger = Logger.getLogger(CrawlerService::class.java.name)

class CrawlerService(val crawlerDAO: CrawlerDAO) {
    private val crawler = Crawler()
     val visitedUrls = mutableSetOf<String>()
    private val urlQueue = ConcurrentLinkedQueue<String>()

    /**
     * Starts the crawling process from the given start URLs,
     * with an optional maxDepth for BFS traversal.
     *
     * @param startUrls URLs to begin crawling from
     * @param maxDepth Number of levels to traverse
     */
    fun startCrawling(startUrls: List<String>, maxDepth: Int) {
        logger.info("Starting crawling process with ${startUrls.size} start URLs and maxDepth=$maxDepth")

        // Initialize the queue
        urlQueue.addAll(startUrls)

        var currentDepth = 0
        while (currentDepth < maxDepth && urlQueue.isNotEmpty()) {
            logger.fine("Processing depth level $currentDepth")

            val levelSize = urlQueue.size
            repeat(levelSize) {
                val url = urlQueue.poll() ?: return@repeat
                if (!visitedUrls.contains(url)) {
                    visitedUrls.add(url)
                    logger.fine("Crawling URL: $url")
                    crawlSinglePage(url)
                }
            }
            currentDepth++
        }

        logger.info("Crawling process completed. Visited ${visitedUrls.size} URLs.")
    }

    /**
     * Crawl a single page (fetch, parse, store in DB, enqueue child links).
     *
     * @param url The page URL to crawl
     */
    private fun crawlSinglePage(url: String) {
        logger.fine("Fetching document from URL: $url")
        val document = crawler.fetchPage(url) ?: run {
            logger.severe("Failed to fetch document from URL: $url")
            return
        }

        logger.fine("Storing URL to page ID mapping for URL: $url")
        val pageId = crawlerDAO.storeUrlToPageIdMapping(url)
        logger.fine("Stored URL to page ID mapping. Page ID: $pageId")

        // Extract raw word list from the Document
        logger.fine("Extracting keywords from document")
        val rawKeywords = crawler.extractKeywords(document)
        logger.fine("Extracted ${rawKeywords.size} raw keywords from the document")

        // Convert it to a frequency map
        logger.fine("Computing keyword frequencies")
        val keywordFrequencies = computeKeywordFrequencies(rawKeywords)
        logger.fine("Computed keyword frequencies for ${keywordFrequencies.size} unique keywords")

        // (Optional) store keywords in forward index
        logger.fine("Storing page keywords in forward index")
        crawlerDAO.storePageKeywords(pageId, rawKeywords)
        logger.fine("Stored page keywords in forward index")

        // Extract links
        logger.fine("Extracting links from document")
        val links = crawler.extractLinks(document)
        logger.fine("Extracted ${links.size} links from the document")

        // Convert links to child page IDs
        logger.fine("Storing child URLs to page ID mappings")
        val childPageIds = links.map { childUrl ->
            crawlerDAO.storeUrlToPageIdMapping(childUrl)
        }
        logger.fine("Stored ${childPageIds.size} child URLs to page ID mappings")

        // Store the parent-child relationships
        logger.fine("Storing parent-child relationships")
        crawlerDAO.storeParentChildLinks(pageId, childPageIds)
        logger.fine("Stored parent-child relationships")

        // Build and store the Page model
        logger.fine("Storing page properties")
        storePageProperties(pageId, document, keywordFrequencies, links, url)
        logger.fine("Stored page properties")

        // Enqueue child links for further crawling
        logger.fine("Enqueuing child URLs for further crawling")
        links.forEach { if (!visitedUrls.contains(it)) urlQueue.offer(it) }
        logger.fine("Enqueued ${links.size} child URLs for further crawling")
    }

    /**
     * Given a list of keywords, compute frequencies (word -> count).
     */
    private fun computeKeywordFrequencies(words: List<String>): Map<String, Int> {
        logger.fine("Computing keyword frequencies for ${words.size} words")
        val freqMap = mutableMapOf<String, Int>()
        for (word in words) {
            freqMap[word] = freqMap.getOrDefault(word, 0) + 1
        }
        logger.fine("Computed frequencies for ${freqMap.size} unique keywords")
        return freqMap
    }

    /**
     * Create a Page object aligned with your data model, then store it with the DAO.
     */
    private fun storePageProperties(
        pageId: String,
        doc: Document,
        keywordFrequencies: Map<String, Int>,
        links: List<String>,
        url: String
    ) {
        logger.fine("Creating Page object for URL: $url")
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

        logger.fine { "Page object: $page" } // Print the Page object
        logger.fine{ "Storing Page object in DAO" }
        crawlerDAO.storePageProperties(pageId, page)
        logger.fine { "Stored Page object in DAO" }
    }

    /**
     * Demonstration of a concurrent approach (optional).
     * BFS with a thread pool, for those who want parallel crawling.
     */
    fun startCrawlingConcurrently(startUrls: List<String>, maxThreads: Int = 4, maxDepth: Int = 2) {
        logger.info("Starting concurrent crawling process with ${startUrls.size} start URLs, maxThreads=$maxThreads, and maxDepth=$maxDepth")

        val executor = Executors.newFixedThreadPool(maxThreads)
        urlQueue.addAll(startUrls)

        var currentDepth = 0
        while (currentDepth < maxDepth && urlQueue.isNotEmpty()) {
            logger.fine("Processing depth level $currentDepth")

            val levelSize = urlQueue.size
            repeat(levelSize) {
                val url = urlQueue.poll() ?: return@repeat
                executor.execute {
                    if (!visitedUrls.contains(url)) {
                        visitedUrls.add(url)
                        logger.fine("Crawling URL: $url")
                        crawlSinglePage(url)
                    }
                }
            }
            currentDepth++
        }

        // Shut down the executor once done
        executor.shutdown()
        logger.info("Concurrent crawling process completed. Visited ${visitedUrls.size} URLs.")
    }

    /**
     * Cleanly close the DAO once crawling is complete.
     */
    fun close() {
        crawlerDAO.close()
    }
}