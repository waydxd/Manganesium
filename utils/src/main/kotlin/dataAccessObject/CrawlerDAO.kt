package dataAccessObject

import org.mapdb.DB

class CrawlerDAO(db: DB) : DatabaseManager(db) {
    // Store page metadata
    fun storePageMetadata(pageId: String, metadata: Map<String, Any>) {
        forwardIndex[pageId] = metadata
    }

    // Store URL to page ID mapping
    fun storeUrlToPageIdMapping(url: String, pageId: String) {
        urlToPageId[url] = pageId
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
}