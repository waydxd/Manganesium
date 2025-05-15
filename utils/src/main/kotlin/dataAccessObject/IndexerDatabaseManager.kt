package dataAccessObject

import org.mapdb.BTreeMap
import org.mapdb.DB
import org.mapdb.HTreeMap
import org.mapdb.Serializer

open class IndexerDatabaseManager(val db: DB) {
    /**
     * Forward Index: pageId -> metadata (HTreeMap for fast random access).
     */
    val forwardIndex: HTreeMap<String, Any> = db
        .hashMap("forward_index")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.JAVA)
        .createOrOpen()

    /**
     * Inverted Index: keyword -> list of pageIds (BTreeMap for ordered keyword searches).
     */
    val invertedTitle: BTreeMap<String, Any> = db
        .treeMap("inverted_title")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.JAVA)
        .createOrOpen()

    val invertedBody: BTreeMap<String, Any> = db
        .treeMap("inverted_body")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.JAVA)
        .createOrOpen()


    /**
     * Word to WordID mapping.
     */
    val wordToWordID: HTreeMap<String, String> = db
        .hashMap("word_to_word_id")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.STRING)
        .createOrOpen()

    /**
     * WordID to Word mapping.
     */
    val wordIDToWord: HTreeMap<String, String> = db
        .hashMap("word_id_to_word")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.STRING)
        .createOrOpen()

    val stopWords = db
        .hashSet("stop_words")
        .serializer(Serializer.STRING)
        .createOrOpen()

    /**
     * Commits changes and closes the database.
     */
    fun close() {
        if (!db.isClosed()) {
            db.commit()
            db.close()
        }
    }
}