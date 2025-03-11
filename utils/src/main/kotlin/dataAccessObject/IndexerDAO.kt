package dataAccessObject

import org.mapdb.DB

class IndexerDAO(db: DB) : DatabaseManager(db) {
    // Update inverted index
    fun storeInvertedIndex(keyword: String, pageId: String) {
        val pages = invertedIndex[keyword] as? MutableSet<String> ?: mutableSetOf()
        pages.add(pageId)
        invertedIndex[keyword] = pages
    }

    // Retrieve pages for a keyword
    fun getPagesForKeyword(keyword: String): List<String> {
        return invertedIndex[keyword] as? List<String> ?: emptyList()
    }

    // Store stop words
    fun storeStopWords(stopWords: Set<String>) {
        this.stopWords.addAll(stopWords)
    }

    // Retrieve stop words
    fun getStopWords(): Set<String> {
        return stopWords.toSet()
    }
}