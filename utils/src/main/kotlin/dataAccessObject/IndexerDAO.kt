package dataAccessObject

import io.github.oshai.kotlinlogging.KotlinLogging
import models.Keyword
import models.Post
import org.manganesium.dataAccessObject.DatabaseManager
import org.mapdb.DB
import org.mapdb.DBMaker
import java.io.File
import java.util.*

class IndexerDAO(db: DB) : DatabaseManager(db) {
    private val logger = KotlinLogging.logger {}

    constructor(dbFile: File) : this(
        DBMaker.fileDB(dbFile)
            .fileMmapEnable()
            .closeOnJvmShutdown()
            .make()
    )

    constructor(dbPath: String) : this(File(dbPath))

    fun storeInvertedTitle(wordId: String, post: Post) {
        val pages = invertedTitle[wordId] as? MutableSet<Post> ?: mutableSetOf()
        pages.add(post)
        invertedTitle[wordId] = pages
    }

    fun storeInvertedBody(wordId: String, post: Post) {
        val pages = invertedBody[wordId] as? MutableSet<Post> ?: mutableSetOf()
        pages.add(post)
        invertedBody[wordId] = pages
    }

    fun getPagesBodyForKeyword(wordId: String): List<Post> {
        logger.info { "Retrieving pages body for wordId: $wordId" }
        return (invertedBody[wordId] as? MutableSet<Post>)?.toList() ?: emptyList()
    }

    fun getPagesTitleForKeyword(wordId: String): List<Post> {
        logger.info { "Retrieving pages title for wordId: $wordId" }
        return (invertedTitle[wordId] as? MutableSet<Post>)?.toList() ?: emptyList()
    }

    fun storeStopWords(stopWords: Set<String>) {
        logger.info { "Storing stop words: $stopWords" }
        this.stopWords.addAll(stopWords)
    }

    fun getStopWords(): Set<String> {
        logger.info { "Retrieving stop words" }
        return stopWords.toSet()
    }

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

    fun getWordIdForWord(keyword: String): String? {
        logger.info { "Retrieving word for wordId: $keyword" }
        return wordToWordID[keyword]
    }

    fun getWordForWordId(wordId: String): String? {
        logger.info { "Retrieving word for wordId: $wordId" }
        return wordIDToWord[wordId]
    }

    // Store page keywords
    fun storePageKeywords(pageId: String, keywords: List<Keyword>) {
        forwardIndex[pageId] = keywords
    }

    // Debug function to check if stored pages are valid.
    private fun debugValidatePages(wordId: String) {
        logger.debug { "Validating pages for wordId: $wordId" }
        val titlePages = getPagesTitleForKeyword(wordId)
        val bodyPages = getPagesBodyForKeyword(wordId)

        titlePages.forEach { post ->
            if (post.pageID.isNotBlank() && post.frequency >= 0) {
                logger.debug { "Title post valid: $post" }
            } else {
                logger.warn { "Title post invalid: $post" }
            }
        }
        bodyPages.forEach { post ->
            if (post.pageID.isNotBlank() && post.frequency >= 0) {
                logger.debug { "Body post valid: $post" }
            } else {
                logger.warn { "Body post invalid: $post" }
            }
        }
    }}