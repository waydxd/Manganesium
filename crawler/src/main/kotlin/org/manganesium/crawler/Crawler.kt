package org.manganesium.crawler

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.util.logging.Logger

class Crawler {
    // Initialize the logger
    private val logger = Logger.getLogger(Crawler::class.java.name)

    /**
     * Fetch the HTML content of the given URL using Jsoup.
     *
     * @param url The URL to fetch.
     * @return Jsoup Document containing the page's HTML, or null if fetch fails.
     */
    fun fetchPage(url: String): Document? {
        return try {
            logger.fine("Fetching page from URL: $url")
            Jsoup.connect(url).get()
        } catch (e: IOException) {
            logger.severe("Failed to fetch $url: ${e.message}")
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
        logger.fine("Extracting links from document")
        val links = mutableListOf<String>()
        val anchorElements = doc.select("a[href]")
        for (element in anchorElements) {
            val absoluteUrl = element.attr("abs:href")
            if (absoluteUrl.isNotEmpty()) {
                links.add(absoluteUrl)
            }
        }
        logger.fine("Extracted ${links.size} links from the document")
        return links
    }

    /**
     * Extract keywords from the document by naive splitting on whitespace.
     * A real application might:
     * - Tokenize more robustly
     * - Handle punctuation and special characters
     * - Filter out stop words, etc.
     */
    fun extractKeywords(doc: Document): List<String> {
        logger.fine("Extracting keywords from document")
        val textContent = doc.body()?.text() ?: ""
        val keywords = textContent
            .split("\\s+".toRegex())
            .map { it.lowercase() }
            .filter { it.isNotBlank() }
        logger.fine("Extracted ${keywords.size} keywords from the document")
        return keywords
    }
}