package org.manganesium.dataAccessObject

import org.mapdb.BTreeMap
import org.mapdb.DB
import org.mapdb.HTreeMap
import org.mapdb.Serializer

open class DatabaseManager(private val db: DB) {
    // Forward Index: pageId -> metadata (HTreeMap for fast random access)
    protected val forwardIndex: HTreeMap<String, Any> = db
        .hashMap("forward_index")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.JAVA)
        .createOrOpen()

    // Inverted Index: keyword -> list of pageIds (BTreeMap for ordered keyword searches)
    protected val invertedIndex: BTreeMap<String, Any> = db
        .treeMap("inverted_index")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.JAVA)
        .createOrOpen()

    // URL to Page ID Mapping: url -> pageId (HTreeMap for fast lookups)
    protected val urlToPageId: HTreeMap<String, String> = db
        .hashMap("url_to_page_id")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.STRING)
        .createOrOpen()

    // Parent/Child Links: parentPageId -> list of childPageIds (BTreeMap for ordered traversal)
    protected val parentChildLinks: BTreeMap<String, Any> = db
        .treeMap("parent_child_links")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.JAVA)
        .createOrOpen()

    // Stop Words: set of stop words (HTreeSet for fast membership checks)
    protected val stopWords = db
        .hashSet("stop_words")
        .serializer(Serializer.STRING)
        .createOrOpen()

    // Page properties (HTreeMap for fast random access)
    protected val pageProperties: HTreeMap<String, Any> = db
        .hashMap("page_properties")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.JAVA)
        .createOrOpen()

    // Commit changes and close the database
    fun close() {
        db.commit()
        db.close()
    }
}