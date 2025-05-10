package dataAccessObject

import models.Page
import org.mapdb.DB
import org.mapdb.DBMaker
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

/**
 * Data Access Object (DAO) for managing crawler database operations.
 *
 * @constructor Creates a CrawlerDAO from the provided database.
 * @param db the MapDB instance used for database operations.
 */
class CrawlerDAO(db: DB) : CrawlerDatabaseManager(db) {
    private val logger = KotlinLogging.logger {}

    /**
     * Secondary constructor to create a CrawlerDAO using a file.
     *
     * @param dbFile the database file.
     */
    constructor(dbFile: File) : this(
        DBMaker.fileDB(dbFile)
            .fileMmapEnable()
            .closeOnJvmShutdown()
            .make()
    )

    /**
     * Secondary constructor to create a CrawlerDAO using a file path.
     *
     * @param dbPath the path to the database file.
     */
    constructor(dbPath: String) : this(File(dbPath))

    /**
     * Stores a mapping from URL to a unique page ID.
     *
     * @param url the URL to store.
     * @return the existing or newly generated unique page ID.
     */
    fun storeUrlToPageIdMapping(url: String): String {
        val existingPageId = urlToPageId[url]
        if (existingPageId != null) {
            logger.debug { "[CrawlerDAO:storeUrlToPageIdMapping] URL already exists: $url -> $existingPageId" }
            return existingPageId
        }
        val newPageId = java.util.UUID.randomUUID().toString()
        urlToPageId[url] = newPageId
        return newPageId
    }

    /**
     * Retrieves the unique page ID for a given URL.
     *
     * @param url the URL whose page ID is to be retrieved.
     * @return the page ID if found, otherwise null.
     */
    fun getPageIdForUrl(url: String): String? {
        val pageId = urlToPageId[url]
        logger.debug { "[CrawlerDAO:getPageIdForUrl] Retrieved page ID for URL: $url -> ${pageId ?: "not found"}" }
        return pageId
    }

    /**
     * Stores parent-child page relationships.
     *
     * @param parentPageId the parent page ID.
     * @param childPageIds list of child page IDs.
     */
    fun storeParentChildLinks(parentPageId: String, childPageIds: List<String>) {
        parentChildLinks[parentPageId] = childPageIds
    }

    /**
     * Retrieves child page IDs for a given parent page.
     *
     * @param parentPageId the parent page ID.
     * @return a list of child page IDs.
     */
    fun getChildPages(parentPageId: String): List<String> {
        val childPages = parentChildLinks[parentPageId] as? List<String> ?: emptyList()
        logger.debug { "[CrawlerDAO:getChildPages] Retrieved ${childPages.size} child pages for parent page ID: $parentPageId" }
        return childPages
    }

    /**
     * Stores properties of a page in the database.
     *
     * @param pageId the unique page identifier.
     * @param page the Page object containing page properties.
     */
    fun storePageProperties(pageId: String, page: Page) {
        val properties = mapOf(
            "title" to page.title.toString(),
            "lastModified" to page.lastModified,
            "size" to page.size,
            "url" to page.url,
            "content" to page.content,
        )
        pageProperties[pageId] = properties
    }

    /**
     * Retrieves properties of a page by its unique identifier.
     *
     * @param pageId the unique page identifier.
     * @return a map of page properties if found, otherwise null.
     */
    fun getPageProperties(pageId: String): Map<String, Any>? {
        val properties = pageProperties[pageId] as? Map<String, Any>
        logger.debug { "[CrawlerDAO:getPageProperties] Retrieved properties for page ID: $pageId" }
        return properties
        }

    /**
     * Gets all page IDs from the database.
     */
    fun getAllPageIds(): List<String> {
        return pageProperties.keys.toList()
    }

    /**
     * Gets parent pages that link to the given page.
     */
    fun getParentPages(childPageId: String): List<String> {
        return parentChildLinks
            .filter { (_, children) ->
                (children as? List<*>)?.contains(childPageId) == true
            }
            .keys
            .toList()
    }
}