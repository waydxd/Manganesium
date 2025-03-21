package org.manganesium.dataAccessObject

import models.Post
import org.mapdb.DB
import java.util.*

class IndexerDAO(db: DB) : DatabaseManager(db) {

    // Update inverted index
    fun storeInvertedTitle(keyword: Post, wordId: String) {
        val pages = invertedTitle[wordId] as? MutableSet<Post> ?: mutableSetOf()
        pages.add(keyword)
        invertedTitle[wordId] = pages
    }
    fun storeInvertedBody(keyword: Post, wordId: String) {
        val pages = invertedBody[wordId] as? MutableSet<Post> ?: mutableSetOf()
        pages.add(keyword)
        invertedBody[wordId] = pages
    }

    // Retrieve pages body for a
    fun getPagesBodyForKeyword(wordId: String): List<String> {
        return invertedBody[wordId] as? List<String> ?: emptyList()
    }

    // Retrieve pages title for a keyword
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

    // WordID to Word mapping and return the word ID
    fun storeWordIdToWordMapping(word: String): String {
        val existingWordId = wordToWordID[word]
        if (existingWordId != null) {
            return existingWordId
        }
        val newWordId = UUID.randomUUID().toString()
        wordToWordID[word] = newWordId

        return newWordId
    }

    // Retrieve word for a word ID
    fun getWordForWordId(wordId: String): String? {
        return wordToWordID[wordId]
    }
}