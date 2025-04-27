package dataAccessObject

import models.Page
import org.mapdb.DB
import org.mapdb.HTreeMap
import org.mapdb.BTreeMap
import org.mapdb.Serializer

open class CrawlerDatabaseManager(val db: DB) {
    /**
     * Page properties.
     */
    val pageProperties: HTreeMap<String, Any> = db
        .hashMap("page_properties")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.JAVA)
        .createOrOpen()

    /**
     * Parent/Child Links: parentPageId -> list of childPageIds (BTreeMap for ordered traversal).
     */
    val parentChildLinks: BTreeMap<String, Any> = db
        .treeMap("parent_child_links")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.JAVA)
        .createOrOpen()

    /**
     * URL to Page ID Mapping: url -> pageId (HTreeMap for fast lookups).
     */
    val urlToPageId: HTreeMap<String, String> = db
        .hashMap("url_to_page_id")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.STRING)
        .createOrOpen()

    fun close() {
        db.commit()
        db.close()
    }
}