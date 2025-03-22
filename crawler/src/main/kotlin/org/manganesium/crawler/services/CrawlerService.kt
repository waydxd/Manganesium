package org.manganesium.crawler.services

import dataAccessObject.CrawlerDAO
import models.Page
import org.jsoup.nodes.Document
import org.manganesium.crawler.Crawler
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import org.manganesium.indexer.Indexer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

class CrawlerService(val crawlerDAO: CrawlerDAO, val crawler: Crawler) {
    // Thread pool for indexing tasks
    private val executor: ExecutorService = Executors.newFixedThreadPool(5) // Adjust pool size as needed

    /**
     * Crawl a single page (fetch, parse, store in DB, enqueue child links).
     *
     * @param url The page URL to crawl
     * @param indexer The indexer to use for indexing the page
     */
    fun crawlSinglePage(url: String, indexer: Indexer) {
        logger.debug { "[CrawlerService:crawlSinglePage] Fetching document from URL: $url" }
        val document = crawler.fetchPage(url) ?: run {
            logger.error { "[CrawlerService:crawlSinglePage] Failed to fetch document from URL: $url" }
            return
        }

        // Store URL -> pageID
        logger.debug { "[CrawlerService:crawlSinglePage] Storing URL to page ID mapping for URL: $url" }
        val pageId = crawlerDAO.storeUrlToPageIdMapping(url)
        logger.debug { "[CrawlerService:crawlSinglePage] Stored URL to page ID mapping. Page ID: $pageId" }

        // Extract links
        logger.debug { "[CrawlerService:crawlSinglePage] Extracting links from document" }
        val links = crawler.extractLinks(document)
        logger.debug { "[CrawlerService:crawlSinglePage] Extracted ${links.size} links from the document" }

        // Convert links to child page IDs
        logger.debug { "[CrawlerService:crawlSinglePage] Storing child URLs to page ID mappings" }
        val childPageIds = links.map { childUrl ->
            crawlerDAO.storeUrlToPageIdMapping(childUrl)
        }
        logger.debug { "[CrawlerService:crawlSinglePage] Stored ${childPageIds.size} child URLs to page ID mappings" }

        // Store the parent-child relationships
        logger.debug { "[CrawlerService:crawlSinglePage] Storing parent-child relationships" }
        crawlerDAO.storeParentChildLinks(pageId, childPageIds)
        logger.debug { "[CrawlerService:crawlSinglePage] Stored parent-child relationships" }

        // Build and store the Page model
        logger.debug { "[CrawlerService:crawlSinglePage] Storing page properties" }
        val page = storePageProperties(pageId, document, links, url)
        logger.debug { "[CrawlerService:crawlSinglePage] Stored page properties" }

        // Submit indexing task to the thread pool
        executor.submit {
            logger.debug { "[CrawlerService:crawlSinglePage - Thread] Indexing page content on a separate thread." }
            try {
                indexer.indexPage(page)
                logger.debug { "[CrawlerService:crawlSinglePage - Thread] Finished indexing page content." }
            } catch (e: Exception) {
                logger.error { "[CrawlerService:crawlSinglePage - Thread] Failed to index page: $url, error: ${e.message}" }
            }
            indexer.indexerClose()
        }

        // Enqueue child links for further crawling
        logger.debug { "[CrawlerService:crawlSinglePage] Enqueuing child URLs for further crawling" }
        links.forEach {
            if (!crawler.visitedUrls.contains(it)) {
                crawler.urlQueue.offer(it)
            }
        }
        logger.debug { "[CrawlerService:crawlSinglePage] Enqueued ${links.size} child URLs for further crawling" }
    }

    /**
     * Create a Page object aligned with your data model, then store it with the DAO.
     * Returns the created Page object for potential indexing.
     */
    private fun storePageProperties(
        pageId: String,
        doc: Document,
        links: List<String>,
        url: String
    ): Page {
        logger.debug { "[CrawlerService:storePageProperties] Creating Page object for URL: $url" }
        val title = doc.title().takeIf { it.isNotBlank() } ?: ""
        val content = doc.body()?.text() ?: ""
        val lastModified = Instant.now().toString()
        val size = doc.outerHtml().length

        val page = Page(
            id = pageId,
            url = url,
            title = title,
            content = content,
            lastModified = lastModified,
            size = size,
            links = links
        )

        logger.debug { "[CrawlerService:storePageProperties] Storing Page object in DAO" }
        crawlerDAO.storePageProperties(pageId, page)
        logger.debug { "[CrawlerService:storePageProperties] Stored Page object in DAO" }

        return page
    }

    /**
     * Shutdown the executor and wait for all indexing tasks to complete.
     */
    fun shutdown() {
        executor.shutdown() // Stop accepting new tasks
        try {
            // Wait up to 60 seconds for all tasks to finish
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                logger.warn { "Some indexing tasks did not finish within timeout" }
                executor.shutdownNow() // Force shutdown if timeout occurs
            }
        } catch (e: InterruptedException) {
            logger.error { "Shutdown interrupted: ${e.message}" }
            executor.shutdownNow()
        }
    }
}