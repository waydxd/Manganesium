package dataAccessObject

import org.mapdb.DB
import org.mapdb.Serializer

open class DatabaseManager(private val db: DB) {
    // Forward Index: pageId -> metadata
    protected val forwardIndex = db.hashMap("forward_index", Serializer.STRING, Serializer.JAVA).createOrOpen()

    // Inverted Index: keyword -> list of pageIds
    protected val invertedIndex = db.hashMap("inverted_index", Serializer.STRING, Serializer.JAVA).createOrOpen()

    // URL to Page ID Mapping: url -> pageId
    protected val urlToPageId = db.hashMap("url_to_page_id", Serializer.STRING, Serializer.STRING).createOrOpen()

    // Parent/Child Links: parentPageId -> list of childPageIds
    protected val parentChildLinks = db.hashMap("parent_child_links", Serializer.STRING, Serializer.JAVA).createOrOpen()

    // Stop Words: set of stop words
    protected val stopWords = db.hashSet("stop_words", Serializer.STRING).createOrOpen()

    // Commit changes and close the database
    fun close() {
        db.commit()
        db.close()
    }
}