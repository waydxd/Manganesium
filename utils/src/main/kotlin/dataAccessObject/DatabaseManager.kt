package dataAccessObject

import org.mapdb.BTreeMap
import org.mapdb.DB
import org.mapdb.HTreeMap
import org.mapdb.Serializer

/**
 * Base class for managing database operations using MapDB.
 *
 * @property db the MapDB instance used for database operations.
 */
open class DatabaseManager(private val db: DB) {
    /**
     * Forward Index: pageId -> metadata (HTreeMap for fast random access).
     */
    protected val forwardIndex: HTreeMap<String, Any> = db
        .hashMap("forward_index")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.JAVA)
        .createOrOpen()

    /**
     * Inverted Index: keyword -> list of pageIds (BTreeMap for ordered keyword searches).
     */
    protected val invertedTitle: BTreeMap<String, Any> = db
        .treeMap("inverted_title")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.JAVA)
        .createOrOpen()

    protected val invertedBody: BTreeMap<String, Any> = db
        .treeMap("inverted_body")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.JAVA)
        .createOrOpen()

    /**
     * URL to Page ID Mapping: url -> pageId (HTreeMap for fast lookups).
     */
    protected val urlToPageId: HTreeMap<String, String> = db
        .hashMap("url_to_page_id")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.STRING)
        .createOrOpen()

    /**
     * Parent/Child Links: parentPageId -> list of childPageIds (BTreeMap for ordered traversal).
     */
    protected val parentChildLinks: BTreeMap<String, Any> = db
        .treeMap("parent_child_links")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.JAVA)
        .createOrOpen()

    /**
     * Set of stop words.
     */
    protected val stopWords = db
        .hashSet("stop_words")
        .serializer(Serializer.STRING)
        .createOrOpen()

    /**
     * Page properties.
     */
    protected val pageProperties: HTreeMap<String, Any> = db
        .hashMap("page_properties")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.JAVA)
        .createOrOpen()

    /**
     * Word to WordID mapping.
     */
    protected val wordToWordID: HTreeMap<String, String> = db
        .hashMap("word_to_word_id")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.STRING)
        .createOrOpen()

    /**
     * WordID to Word mapping.
     */
    protected val wordIDToWord: HTreeMap<String, String> = db
        .hashMap("word_id_to_word")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.STRING)
        .createOrOpen()

    /**
     * Commits changes and closes the database.
     */
    fun close() {
        db.commit()
        db.close()
    }
}