package org.manganesium.indexer;

import models.Page
import models.Post
import dataAccessObject.IndexerDAO

public class Indexer {
    private val kwP = KeywordProcessor()
    private val indexerDao = IndexerDAO("indexer.db")

    /**
     * performs indexing on Page object
     * input: page
     * @param p Page object
     */
    fun indexPage(p: Page) {
        val word_freq_T = kwP.stopAndStem(p.title)
        val word_freq_B = kwP.stopAndStem(p.content)
        val dummy: List<Int> = ArrayList<Int>()

        for (i in word_freq_T) {
            val wordID = indexerDao.storeWordIdToWordMapping(i.key)
            indexerDao.storeInvertedTitle(wordID, Post(p.id.toString(), i.value, dummy))
        }
        for (i in word_freq_B) {
            val wordID = indexerDao.storeWordIdToWordMapping(i.key)
            indexerDao.storeInvertedTitle(wordID, Post(p.id.toString(), i.value, dummy))
        }
    }
}
