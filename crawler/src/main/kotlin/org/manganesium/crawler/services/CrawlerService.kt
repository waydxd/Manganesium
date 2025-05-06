package org.manganesium.crawler.services

import dataAccessObject.CrawlerDAO
import models.Page
import org.jsoup.nodes.Document
import org.manganesium.crawler.Crawler
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.Jsoup
import java.time.Instant
import org.manganesium.indexer.Indexer
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * Service class responsible for crawling individual pages, parsing content,
 * storing page properties in the database, and indexing page content.
 *
 * This class collaborates with the [Crawler] (which supplies methods for fetching
 * and link extraction), the [CrawlerDAO] (for persistent storage), and the [Indexer]
 * (to perform search indexing on the crawled pages). Additionally, it makes use of a thread
 * pool executor to offload indexing tasks, allowing for asynchronous processing.
 *
 * @property crawlerDAO The data access object for persistence operations.
 * @property crawler The crawler instance used to fetch pages and extract links.
 */
class CrawlerService(val crawlerDAO: CrawlerDAO, val crawler: Crawler) {
    // Thread pool for indexing tasks
    private val executor: ExecutorService = Executors.newFixedThreadPool(5) // Adjust pool size as needed

    /**
     * Crawls a single page: fetches the page, extracts links, stores mappings and relationships,
     * creates a [Page] object from the document, and submits an asynchronous indexing task.
     *
     * Steps performed:
     * 1. Fetch the page from the given URL.
     * 2. If fetching fails (i.e. returns null), log the failure and return immediately.
     * 3. Store the URL-to-page ID mapping in the DAO.
     * 4. Extract all child links from the document.
     * 5. Store child link mappings and the parent-child relationships.
     * 6. Create a [Page] object using extracted properties (title, content, last modified, size, etc.),
     *    and store it in the DAO.
     * 7. Submit an asynchronous task to index the page using the provided [Indexer].
     * 8. Enqueue all extracted child URLs for further crawling, if they haven't already been visited.
     *
     * @param url The URL of the page to be crawled.
     * @param indexer The indexer instance used to index the page content.
     */
    fun crawlSinglePage(url: String, indexer: Indexer) {
        logger.debug { "[CrawlerService:crawlSinglePage] Fetching document from URL: $url" }
        val document = crawler.fetchPage(url) ?: run {
            logger.error { "[CrawlerService:crawlSinglePage] Failed to fetch document from URL: $url" }
            return
        }
        val response = Jsoup.connect(url).execute()
        val lastModifiedHeader = response.header("Last-Modified")

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
        val page = storePageProperties(pageId, document, links, url, lastModifiedHeader)
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
     * Creates a [Page] object from the provided document and properties, then stores it into the DAO.
     *
     * This method extracts the title, content, last modified timestamp, and size of the page,
     * and uses these properties along with the page's URL and link list to create a [Page] instance.
     * The size is determined by the length of the HTML content (using `doc.outerHtml().length`).
     *
     * @param pageId The unique identifier assigned to the page.
     * @param doc The Jsoup [Document] representing the fetched HTML page.
     * @param links A list of URLs extracted from the document.
     * @param url The original URL of the page.
     * @return The created [Page] object which is also stored via the DAO.
     */
    private fun storePageProperties(
        pageId: String,
        doc: Document,
        links: List<String>,
        url: String,
        lastModifiedHeader: String?
    ): Page {
        logger.debug { "[CrawlerService:storePageProperties] Creating Page object for URL: $url" }
        val title = doc.title().takeIf { it.isNotBlank() } ?: ""
        val content = doc.body()?.text() ?: ""
        val size = doc.outerHtml().length

        val outputFormatter = DateTimeFormatter
            .ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH)
            .withZone(ZoneId.of("Asia/Hong_Kong"))
        val lastModified = try {
            if (lastModifiedHeader.isNullOrBlank()) {
                logger.debug { "[CrawlerService:storePageProperties] No Last-Modified header for URL: $url, using current time in HKT" }
                ZonedDateTime.now(ZoneId.of("Asia/Hong_Kong")).format(outputFormatter)
            } else {
                val inputFormatter = DateTimeFormatter
                    .ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
                    .withZone(ZoneId.of("GMT"))
                val date = ZonedDateTime.parse(lastModifiedHeader, inputFormatter)
                date.withZoneSameInstant(ZoneId.of("Asia/Hong_Kong")).format(outputFormatter)
            }
        } catch (e: DateTimeParseException) {
            logger.warn { "[CrawlerService:storePageProperties] Invalid Last-Modified header '$lastModifiedHeader' for URL: $url, using current time in HKT" }
            ZonedDateTime.now(ZoneId.of("Asia/Hong_Kong")).format(outputFormatter)
        }

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
     * Shuts down the executor service and waits for all indexing tasks to complete.
     *
     * This method attempts to gracefully shut down the thread pool that is used to execute
     * indexing tasks. It stops accepting new tasks and waits up to 60 seconds for existing tasks
     * to terminate. If the tasks do not finish within the timeout, it forces a shutdown.
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