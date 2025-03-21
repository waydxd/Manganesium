package org.manganesium.indexer

import org.manganesium.indexer.Porter
import java.io.File
import java.util.*
import java.util.logging.Logger;

class KeywordProcessor {
    private val stemmer = Porter()
    private val stopwords = HashSet<String>()   // store stopwords

    init {
        loadStopword()
    }

    private fun loadStopword() {
        try {
            val file = File("./indexer/src/main/kotlin/org/manganesium/indexer/stopwords.txt")
            val reader: Scanner = Scanner(file)
            while (reader.hasNextLine()) {
                val data: String = reader.nextLine()
                stopwords.add(data)
            }
        } catch (e: Exception) {
            println(e)
            LOGGER.warning("[ ERROR while loading stopwords ] ")
        }
    }

    /**
     * 1
     * performs stopword removal and stemming using Porter algorithm
     * performs wordID <=> word assignment
     * store the result of stem in a freq map
     * store the wordID map in DB
     * @param keywordBody String, represent the raw body test string
     */
    fun stopAndStem(keywordBody: String?): HashMap<String, Int> {
        val stems = HashMap<String, Int>()  // freq map
        val tokenizer = StringTokenizer(keywordBody)
        var stem: String    // stemmed word

        while (tokenizer.hasMoreTokens()) {
            val word = tokenizer.nextToken()
            if (stopwords.contains(word)) { 
                continue
            }
            stem = stemmer.stripAffixes(word)
            stems[stem] = stems.getOrDefault(stem, 0) + 1
        }
        return stems
    }

    companion object {
        private var keywordProcessorInstance: KeywordProcessor? = null
        private val LOGGER: Logger = Logger.getLogger(KeywordProcessor::class.java.name)
        val instance: KeywordProcessor?
            get() {
                if (keywordProcessorInstance == null) {
                    keywordProcessorInstance = KeywordProcessor()
                }
                return keywordProcessorInstance
            }
    }
}