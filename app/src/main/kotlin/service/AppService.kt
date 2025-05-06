package app.service

import app.api.model.SearchRequest
import app.api.model.SearchResponse
import dataAccessObject.DatabaseManager
import mu.KotlinLogging
import java.text.SimpleDateFormat
import java.util.Date

class AppService {
//    private val logger = KotlinLogging.logger {}
//    private val searchService = SearchService()
//    private val db = DatabaseManager()

//    fun search(request: SearchRequest): List<SearchResponse> {
//        logger.debug { "Searching for: ${request.query}" }
//        val pageIds = searchService.query(request.query)
//        return pageIds
//            .drop(request.offset)
//            .take(request.limit)
//            .mapNotNull { pageId ->
//                db.pages[pageId]?.let { page ->
//                    SearchResponse(
//                        pageId = page.pageId,
//                        title = page.title,
//                        url = page.url,
//                        snippet = generateSnippet(page.content, request.query),
//                        lastModified = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(Date(page.lastModified)),
//                        keywords = db.pageIdToTopKeywords[pageId]?.map { it.stem }
//                    )
//                }
//            }
//    }

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
}