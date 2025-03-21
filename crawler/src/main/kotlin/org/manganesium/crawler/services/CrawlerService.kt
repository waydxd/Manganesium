package org.manganesium.crawler.services

import dataAccessObject.CrawlerDAO
import models.Page
import org.jsoup.nodes.Document
import org.manganesium.crawler.Crawler
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant

private val logger = KotlinLogging.logger {}

class CrawlerService(val crawlerDAO: CrawlerDAO, val crawler: Crawler) {
    /**
     * Crawl a single page (fetch, parse, store in DB, enqueue child links).
     *
     * @param url The page URL to crawl
     */
    fun crawlSinglePage(url: String) {
        logger.debug { "[CrawlerService:crawlSinglePage] Fetching document from URL: $url" }
        val document = crawler.fetchPage(url) ?: run {
            logger.error { "[CrawlerService:crawlSinglePage] Failed to fetch document from URL: $url" }
            return
        }

        logger.debug { "[CrawlerService:crawlSinglePage] Storing URL to page ID mapping for URL: $url" }
        val pageId = crawlerDAO.storeUrlToPageIdMapping(url)
        logger.debug { "[CrawlerService:crawlSinglePage] Stored URL to page ID mapping. Page ID: $pageId" }

        /*
        // Extract raw word list from the Document
        logger.debug { "[CrawlerService:crawlSinglePage] Extracting keywords from document" }
        val rawKeywords = crawler.extractKeywords(document)
        logger.debug { "[CrawlerService:crawlSinglePage] Extracted ${rawKeywords.size} raw keywords from the document" }

        // Convert it to a frequency map
        logger.debug { "[CrawlerService:crawlSinglePage] Computing keyword frequencies" }
        val keywordFrequencies = computeKeywordFrequencies(rawKeywords)
        logger.debug { "[CrawlerService:crawlSinglePage] Computed keyword frequencies for ${keywordFrequencies.size} unique keywords" }

        // (Optional) store keywords in forward index
        logger.debug { "[CrawlerService:crawlSinglePage] Storing page keywords in forward index" }
        crawlerDAO.storePageKeywords(pageId, rawKeywords)
        logger.debug { "[CrawlerService:crawlSinglePage] Stored page keywords in forward index" }
        */

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
        storePageProperties(pageId, document, links, url)
        logger.debug { "[CrawlerService:crawlSinglePage] Stored page properties" }

        // Enqueue child links for further crawling
        logger.debug { "[CrawlerService:crawlSinglePage] Enqueuing child URLs for further crawling" }
        links.forEach { if (!crawler.visitedUrls.contains(it)) crawler.urlQueue.offer(it) }
        logger.debug { "[CrawlerService:crawlSinglePage] Enqueued ${links.size} child URLs for further crawling" }
    }

    /*
    /**
     * Given a list of keywords, compute frequencies (word -> count).
     */
    private fun computeKeywordFrequencies(words: List<String>): Map<String, Int> {
        logger.debug { "[CrawlerService:computeKeywordFrequencies] Computing keyword frequencies for ${words.size} words" }
        val freqMap = mutableMapOf<String, Int>()
        for (word in words) {
            freqMap[word] = freqMap.getOrDefault(word, 0) + 1
        }
        logger.debug { "[CrawlerService:computeKeywordFrequencies] Computed frequencies for ${freqMap.size} unique keywords" }
        return freqMap
    }
    */

    /**
     * Create a Page object aligned with your data model, then store it with the DAO.
     */
    private fun storePageProperties(
        pageId: String,
        doc: Document,
        //keywordFrequencies: Map<String, Int>,
        links: List<String>,
        url: String
    ) {
        logger.debug { "[CrawlerService:storePageProperties] Creating Page object for URL: $url" }
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
            //keywords = keywordFrequencies,
            links = links
        )

        logger.debug { "[CrawlerService:storePageProperties] Storing Page object in DAO" }
        crawlerDAO.storePageProperties(pageId, page)
        logger.debug { "[CrawlerService:storePageProperties] Stored Page object in DAO" }
    }
}