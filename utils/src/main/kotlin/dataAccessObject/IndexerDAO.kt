package dataAccessObject

import io.github.oshai.kotlinlogging.KotlinLogging
import models.Keyword
import models.Post
import org.mapdb.DB
import org.mapdb.DBMaker
import java.io.File
import java.util.UUID

/**
 * Data Access Object (DAO) for managing indexer database operations.
 *
 * This class extends [IndexerDatabaseManager] and provides functions to store and retrieve
 * inverted indexes, stop words, word mappings, and page keywords.
 *
 * @param db the MapDB instance used for database operations.
 */
class IndexerDAO(db: DB) : IndexerDatabaseManager(db) {
    private val logger = KotlinLogging.logger {}

    /**
     * Secondary constructor for creating an [IndexerDAO] using a database file.
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
     * Secondary constructor for creating an [IndexerDAO] using a database file path.
     *
     * @param dbPath the path to the database file.
     */
    constructor(dbPath: String) : this(File(dbPath))

    /**
     * Stores a post in the inverted title index.
     *
     * @param wordId a unique identifier for the word.
     * @param post the [Post] object to be stored.
     */
    fun storeInvertedTitle(wordId: String, post: Post) {
        val pages = invertedTitle[wordId] as? MutableSet<Post> ?: mutableSetOf()
        pages.add(post)
        invertedTitle[wordId] = pages
    }

    /**
     * Stores a post in the inverted body index.
     *
     * @param wordId a unique identifier for the word.
     * @param post the [Post] object to be stored.
     */
    fun storeInvertedBody(wordId: String, post: Post) {
        val pages = invertedBody[wordId] as? MutableSet<Post> ?: mutableSetOf()
        pages.add(post)
        invertedBody[wordId] = pages
    }

    /**
     * Retrieves a list of posts for a given keyword from the inverted body index.
     *
     * @param wordId a unique identifier for the word.
     * @return a list of [Post] objects associated with the given wordId.
     */
    fun getPagesBodyForKeyword(wordId: String): List<Post> {
        logger.info { "Retrieving pages body for wordId: $wordId" }
        return (invertedBody[wordId] as? MutableSet<Post>)?.toList() ?: emptyList()
    }

    /**
     * Retrieves a list of posts for a given keyword from the inverted title index.
     *
     * @param wordId a unique identifier for the word.
     * @return a list of [Post] objects associated with the given wordId.
     */
    fun getPagesTitleForKeyword(wordId: String): List<Post> {
        logger.info { "Retrieving pages title for wordId: $wordId" }
        return (invertedTitle[wordId] as? MutableSet<Post>)?.toList() ?: emptyList()
    }

    /**
     * Stores a set of stop words into the database.
     *
     * @param stopWords the set of stop words to store.
     */
    fun storeStopWords(stopWords: Set<String>) {
        logger.info { "Storing stop words: $stopWords" }
        this.stopWords.addAll(stopWords)
    }

    /**
     * Retrieves the set of stored stop words.
     *
     * @return a set of stop words.
     */
    fun getStopWords(): Set<String> {
        logger.info { "Retrieving stop words" }
        return stopWords.toSet()
    }

    /**
     * Stores a mapping of a word to a unique word identifier.
     *
     * If the word already exists, the existing identifier is returned.
     *
     * @param word the word to map.
     * @return the existing or newly generated unique word identifier.
     */
    fun storeWordIdToWordMapping(word: String): String {
        val existingWordId = wordToWordID[word]
        if (existingWordId != null) {
            logger.info { "WordId already exists for word: $word, wordId: $existingWordId" }
            return existingWordId
        }
        val newWordId = UUID.randomUUID().toString()
        wordToWordID[word] = newWordId
        wordIDToWord[newWordId] = word
        logger.info { "Generated new wordId: $newWordId for word: $word" }
        return newWordId
    }

    /**
     * Retrieves the word identifier associated with a given word.
     *
     * @param keyword the word to lookup.
     * @return the word identifier if found, or null.
     */
    fun getWordIdForWord(keyword: String): String? {
        logger.info { "Retrieving word for wordId: $keyword" }
        return wordToWordID[keyword]
    }

    /**
     * Retrieves the word associated with a given word identifier.
     *
     * @param wordId the identifier of the word.
     * @return the word if found, or null.
     */
    fun getWordForWordId(wordId: String): String? {
        logger.info { "Retrieving word for wordId: $wordId" }
        return wordIDToWord[wordId]
    }

    /**
     * Stores the keywords associated with a page.
     *
     * @param pageId the unique page identifier.
     * @param keywords a list of [Keyword] objects associated with the page.
     */
    fun storePageKeywords(pageId: String, keywords: List<Keyword>) {
        forwardIndex[pageId] = keywords
    }

    /**
     * Retrieves the keywords associated with a page.
     *
     * @param pageId the unique page identifier.
     * @return a list of [Keyword] objects associated with the page.
     */
    fun getPageKeywords(pageId: String): List<Keyword> {
        logger.info { "Retrieving keywords for pageId: $pageId" }
        return forwardIndex[pageId] as? List<Keyword> ?: emptyList()
    }
}