package app.service

import app.api.model.SearchRequest
import app.api.model.SearchResponse
import dataAccessObject.CrawlerDAO
import dataAccessObject.IndexerDAO
import mu.KotlinLogging
import service.SearchService
import java.text.SimpleDateFormat
import java.util.Date
import org.manganesium.indexer.KeywordProcessor

class AppService {
    private val logger = KotlinLogging.logger {}
    private val crawlerDAO = CrawlerDAO("crawler.db")
    private val indexerDAO = IndexerDAO("indexer.db")
    private val searchService = SearchService(crawlerDAO, indexerDAO);

     fun search(request: SearchRequest): List<SearchResponse> {
        logger.debug { "Searching for: ${request.query}" }

        // Process the query through stopword removal and stemming
        val keywordProcessor = KeywordProcessor.instance
        val processedQuery = keywordProcessor?.stopAndStem(request.query)
            ?.keys?.joinToString(" ")
            ?: request.query

        logger.debug { "Processed query: $processedQuery" }
        val pageIds = searchService.search(processedQuery) // Should we implement pagination in search?
        return pageIds
            .drop(request.offset)
            .take(request.limit)
            .mapNotNull { pageId ->
                try {
                    val page = crawlerDAO.getPageProperties(pageId) ?: return@mapNotNull null
                    val content = page["content"].toString() ?: ""

                    SearchResponse(
                        pageID = pageId,
                        title = page["title"].toString(),
                        url = page["url"].toString(),
                        lastModified = formatDate(page["lastModified"].toString()),
                        snippet = generateSnippet(content, request.query)
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error creating SearchResponse for pageId: $pageId" }
                    null
                }
            }
    }

    private fun generateSnippet(content: String, query: String): String {
        val words = query.split(" ").map { it.lowercase() }
        val contentLower = content.lowercase()
        var start = 0
        words.forEach { if (it in contentLower) start = maxOf(0, contentLower.indexOf(it) - 50) }
        val end = minOf(start + 150, content.length)
        var snippet = if (start > 0) "..." + content.substring(start, end) else content.substring(0, end)
        if (end < content.length) snippet += "..."
        return words.fold(snippet) { text, word -> text.replace(word, "<b>$word</b>", true) }
    }

    private fun formatDate(dateString: String): String {
        return try {
            // Handle the ISO-8601 format like "2025-04-27T16:40:11.071381Z"
            if (dateString.contains('T') && dateString.contains('Z')) {
                // Already in desired format, just clean it up
                val parsedDate = dateString.substringBefore('.')
                return parsedDate.replace('T', ' ')
            }

            // Try to parse as long timestamp if not in ISO format
            val timestamp = dateString.toLongOrNull()
            if (timestamp != null) {
                val date = Date(timestamp)
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date)
            } else {
                dateString // Return original if can't parse
            }
        } catch (e: Exception) {
            "Unknown date"
        }
    }

}