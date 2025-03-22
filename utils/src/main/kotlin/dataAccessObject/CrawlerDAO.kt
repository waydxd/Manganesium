package dataAccessObject

import models.Page
import org.manganesium.dataAccessObject.DatabaseManager
import org.mapdb.DB
import org.mapdb.DBMaker
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

class CrawlerDAO(db: DB) : DatabaseManager(db) {
    private val logger = KotlinLogging.logger {}

    constructor(dbFile: File) : this(
        DBMaker.fileDB(dbFile)
            .fileMmapEnable()
            .closeOnJvmShutdown()
            .make()
    )

    constructor(dbPath: String) : this(File(dbPath))

    // Store URL to page ID mapping and return the page ID
    fun storeUrlToPageIdMapping(url: String): String {
        // Check if URL already exists in the mapping
        val existingPageId = urlToPageId[url]
        if (existingPageId != null) {
            logger.debug { "[CrawlerDAO:storeUrlToPageIdMapping] URL already exists in mapping: $url -> $existingPageId" }
            return existingPageId
        }

        // Generate a new unique page ID
        val newPageId = java.util.UUID.randomUUID().toString()

        // Store the URL to page ID mapping
        urlToPageId[url] = newPageId

        return newPageId
    }

    // Retrieve page ID for a URL
    fun getPageIdForUrl(url: String): String? {
        val pageId = urlToPageId[url]
        logger.debug { "[CrawlerDAO:getPageIdForUrl] Retrieved page ID for URL: $url -> ${pageId ?: "not found"}" }
        return pageId
    }

    // Store parent/child links
    fun storeParentChildLinks(parentPageId: String, childPageIds: List<String>) {
        parentChildLinks[parentPageId] = childPageIds
    }

    // Retrieve child pages for a parent page
    fun getChildPages(parentPageId: String): List<String> {
        val childPages = parentChildLinks[parentPageId] as? List<String> ?: emptyList()
        logger.debug { "[CrawlerDAO:getChildPages] Retrieved ${childPages.size} child pages for parent page ID: $parentPageId" }
        return childPages
    }

    // Store page properties
    fun storePageProperties(pageId: String, page: Page) {
        val properties = mapOf(
            "title" to page.title.toString(),
            "lastModified" to page.lastModified,
            "size" to page.size,
            "url" to page.url
        )
        pageProperties[pageId] = properties
    }
}