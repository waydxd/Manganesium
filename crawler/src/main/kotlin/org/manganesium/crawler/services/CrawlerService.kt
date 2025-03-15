package org.manganesium.crawler.services

import dataAccessObject.CrawlerDAO
import models.Page
import org.jsoup.nodes.Document
import org.manganesium.crawler.Crawler
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors

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
        // Initialize the queue
        urlQueue.addAll(startUrls)

        var currentDepth = 0
        while (currentDepth < maxDepth && urlQueue.isNotEmpty()) {
            val levelSize = urlQueue.size
            // Process the current "layer" of URLs
            repeat(levelSize) {
                val url = urlQueue.poll() ?: return@repeat
                if (!visitedUrls.contains(url)) {
                    visitedUrls.add(url)
                    crawlSinglePage(url)
                }
            }
            currentDepth++
        }
    }

    /**
     * Crawl a single page (fetch, parse, store in DB, enqueue child links).
     *
     * @param url The page URL to crawl
     */
    private fun crawlSinglePage(url: String) {
        val document = crawler.fetchPage(url) ?: return
        val pageId = crawlerDAO.storeUrlToPageIdMapping(url)

        // Extract raw word list from the Document
        val rawKeywords = crawler.extractKeywords(document)
        // Convert it to a frequency map
        val keywordFrequencies = computeKeywordFrequencies(rawKeywords)

        // (Optional) store keywords in forward index
        crawlerDAO.storePageKeywords(pageId, rawKeywords)

        // Extract links
        val links = crawler.extractLinks(document)

        // Convert links to child page IDs
        val childPageIds = links.map { childUrl ->
            crawlerDAO.storeUrlToPageIdMapping(childUrl)
        }

        // Store the parent-child relationships
        crawlerDAO.storeParentChildLinks(pageId, childPageIds)

        // Build and store the Page model
        storePageProperties(pageId, document, keywordFrequencies, links, url)

        // Enqueue child links for further crawling
        links.forEach { if (!visitedUrls.contains(it)) urlQueue.offer(it) }
    }

    /**
     * Given a list of keywords, compute frequencies (word -> count).
     */
    private fun computeKeywordFrequencies(words: List<String>): Map<String, Int> {
        val freqMap = mutableMapOf<String, Int>()
        for (word in words) {
            freqMap[word] = freqMap.getOrDefault(word, 0) + 1
        }
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
        val title = doc.title().takeIf { it.isNotBlank() }
        val content = doc.body()?.text() ?: ""
        val lastModified = Instant.now().toString()
        val size = doc.outerHtml().length

        // Example: if you want to store unique numeric IDs, you can handle that in your DAO.
        // For this example, we’ll just put "0L" as the ID or derive from pageId as needed.
        // If there's an auto-increment mechanism, use that instead.
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

        crawlerDAO.storePageProperties(pageId, page)
    }

    /**
     * Demonstration of a concurrent approach (optional).
     * BFS with a thread pool, for those who want parallel crawling.
     */
    fun startCrawlingConcurrently(startUrls: List<String>, maxThreads: Int = 4, maxDepth: Int = 2) {
        val executor = Executors.newFixedThreadPool(maxThreads)
        urlQueue.addAll(startUrls)

        var currentDepth = 0
        while (currentDepth < maxDepth && urlQueue.isNotEmpty()) {
            val levelSize = urlQueue.size
            repeat(levelSize) {
                val url = urlQueue.poll() ?: return@repeat
                executor.execute {
                    if (!visitedUrls.contains(url)) {
                        visitedUrls.add(url)
                        crawlSinglePage(url)
                    }
                }
            }
            currentDepth++
        }

        // Shut down the executor once done
        executor.shutdown()
    }

    /**
     * Cleanly close the DAO once crawling is complete.
     */
    fun close() {
        crawlerDAO.close()
    }
}