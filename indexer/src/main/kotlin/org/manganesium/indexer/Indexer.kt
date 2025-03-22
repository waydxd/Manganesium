package org.manganesium.indexer

import models.Page
import models.Post
import dataAccessObject.IndexerDAO
import models.Keyword

class Indexer {
    private val kwP = KeywordProcessor()
    private val indexerDao = IndexerDAO("indexer.db")

    /**
     * performs indexing on Page object
     * input: page
     * @param p Page object
     */
    fun indexPage(p: Page) {
        val wordFreqT = kwP.stopAndStem(p.title)
        val wordFreqB = kwP.stopAndStem(p.content)
        val dummy: List<Int> = ArrayList()

        // Save inverted indexes with trimmed keywords
        for ((word, freq) in wordFreqT) {
            val trimmedWord = word.trim()
            if (trimmedWord.isNotEmpty()) {
                val wordID = indexerDao.storeWordIdToWordMapping(trimmedWord)
                indexerDao.storeInvertedTitle(wordID, Post(p.id, freq, dummy))
            }
        }
        for ((word, freq) in wordFreqB) {
            val trimmedWord = word.trim()
            if (trimmedWord.isNotEmpty()) {
                val wordID = indexerDao.storeWordIdToWordMapping(trimmedWord)
                indexerDao.storeInvertedBody(wordID, Post(p.id, freq, dummy))
            }
        }

        // Combine frequencies from title and content after trimming keywords.
        val combinedFreq = mutableMapOf<String, Int>()
        for ((word, freq) in wordFreqT) {
            val trimmedWord = word.trim()
            if (trimmedWord.isNotEmpty()) {
                combinedFreq[trimmedWord] = combinedFreq.getOrDefault(trimmedWord, 0) + freq
            }
        }
        for ((word, freq) in wordFreqB) {
            val trimmedWord = word.trim()
            if (trimmedWord.isNotEmpty()) {
                combinedFreq[trimmedWord] = combinedFreq.getOrDefault(trimmedWord, 0) + freq
            }
        }

        // Take top 10 keywords based on frequency and convert them to Keyword objects.
        val topKeywords = combinedFreq.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { entry ->
                Keyword(indexerDao.storeWordIdToWordMapping(entry.key), entry.value)
            }

        // Store only top 10 Keyword objects in the forward index.
        indexerDao.storePageKeywords(p.id, topKeywords)
    }
}