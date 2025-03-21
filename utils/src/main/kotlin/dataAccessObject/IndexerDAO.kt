package org.manganesium.dataAccessObject

import models.Post
import org.mapdb.DB
import java.util.*

class IndexerDAO(db: DB) : DatabaseManager(db) {

    /**
     * Store the wordID to keyword mapping
     * @param keyword, wordId
     */
    fun storeInvertedTitle(keyword: Post, wordId: String) {
        val pages = invertedTitle[wordId] as? MutableSet<Post> ?: mutableSetOf()
        pages.add(keyword)
        invertedTitle[wordId] = pages
    }

    /**
     * Store the wordID to keyword mapping
     * @param keyword, wordId
     */
    fun storeInvertedBody(keyword: Post, wordId: String) {
        val pages = invertedBody[wordId] as? MutableSet<Post> ?: mutableSetOf()
        pages.add(keyword)
        invertedBody[wordId] = pages
    }

    /**
     * get the pages body for a keyword
     * @param wordId
     */
    fun getPagesBodyForKeyword(wordId: String): List<String> {
        return invertedBody[wordId] as? List<String> ?: emptyList()
    }

    /**
     * get the pages title for a keyword
     * @param wordId
     */
    fun getPagesTitleForKeyword(wordId: String): List<String> {
        return invertedTitle[wordId] as? List<String> ?: emptyList()
    }

    // Store stop words
    fun storeStopWords(stopWords: Set<String>) {
        this.stopWords.addAll(stopWords)
    }

    // Retrieve stop words
    fun getStopWords(): Set<String> {
        return stopWords.toSet()
    }

    /**
     * Store the word to wordID mapping
     * @param word
     */
    fun storeWordIdToWordMapping(word: String): String {
        val existingWordId = wordToWordID[word]
        if (existingWordId != null) {
            return existingWordId
        }
        val newWordId = UUID.randomUUID().toString()
        wordToWordID[word] = newWordId

        return newWordId
    }

    /**
     * Get the wordID using keyword
     */
    fun getWordForWordId(keyword: String): String? {
        return wordToWordID[keyword]
    }
}