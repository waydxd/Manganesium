package org.manganesium.dataAccessObject

import org.mapdb.DB
import java.util.*

class IndexerDAO(db: DB) : DatabaseManager(db) {
    // Update inverted index
    fun storeInvertedTitle(keyword: String, pageId: String) {
        val pages = invertedTitle[keyword] as? MutableSet<String> ?: mutableSetOf()
        pages.add(pageId)
        invertedTitle[keyword] = pages
    }
    fun storeInvertedBody(keyword: String, pageId: String) {
        val pages = invertedBody[keyword] as? MutableSet<String> ?: mutableSetOf()
        pages.add(pageId)
        invertedBody[keyword] = pages
    }

    // Retrieve pages body for a keyword
    fun getPagesBodyForKeyword(keyword: String): List<String> {
        return invertedBody[keyword] as? List<String> ?: emptyList()
    }

    // Retrieve pages title for a keyword
    fun getPagesTitleForKeyword(keyword: String): List<String> {
        return invertedTitle[keyword] as? List<String> ?: emptyList()
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