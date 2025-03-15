package dataAccessObject

import org.manganesium.models.Page
import org.mapdb.DB
import org.mapdb.DBMaker
import java.io.File

class CrawlerDAO(db: DB) : DatabaseManager(db) {

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
            return existingPageId
        }

        // Generate a new unique page ID
        val newPageId = java.util.UUID.randomUUID().toString()

        // Store the URL to page ID mapping
        urlToPageId[url] = newPageId

        return newPageId
    }

    // Store page keywords
    fun storePageKeywords(pageId: String, keywords: List<String>) {
        forwardIndex[pageId] = keywords
    }

    // Retrieve page ID for a URL
    fun getPageIdForUrl(url: String): String? {
        return urlToPageId[url]
    }

    // Store parent/child links
    fun storeParentChildLinks(parentPageId: String, childPageIds: List<String>) {
        parentChildLinks[parentPageId] = childPageIds
    }

    // Retrieve child pages for a parent page
    fun getChildPages(parentPageId: String): List<String> {
        return parentChildLinks[parentPageId] as? List<String> ?: emptyList()
    }

    // Store page properties
    fun storePageProperties(pageId: String, page: Page) {
        val properties = mapOf(
            "title" to page.title.toString(),
            "lastModified" to page.lastModified,
            "size" to page.size,
            "links" to page.links
        )
        pageProperties[pageId] = properties
    }
}