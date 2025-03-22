package org.manganesium.indexer

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
            // Use resource loading instead of hardcoded path
            val inputStream = javaClass.getResourceAsStream("org/manganesium/indexer/stopwords.txt")
                ?: KeywordProcessor::class.java.getResourceAsStream("/org/manganesium/indexer/stopwords.txt")

            if (inputStream != null) {
                inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { stopwords.add(it.trim()) }
                }
                LOGGER.info("Loaded ${stopwords.size} stopwords")
            } else {
                // Fallback to file path
                val file = File("indexer/src/main/kotlin/org/manganesium/indexer/stopwords.txt")
                if (file.exists()) {
                    file.bufferedReader().useLines { lines ->
                        lines.forEach { stopwords.add(it.trim()) }
                    }
                    LOGGER.info("Loaded ${stopwords.size} stopwords from file")
                } else {
                    LOGGER.warning("Stopwords file not found")
                }
            }
        } catch (e: Exception) {
            LOGGER.warning("ERROR loading stopwords: ${e.message}")
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
        val stems = HashMap<String, Int>()
        if (keywordBody == null) {
            LOGGER.info("Keyword body is null, returning empty map.")
            return stems
        }
        LOGGER.info("Processing keyword body: $keywordBody")
        val tokenizer = StringTokenizer(keywordBody, " \t\n\r\u000c")
        while (tokenizer.hasMoreTokens()) {
            val rawToken = tokenizer.nextToken()
            LOGGER.fine("Raw token: $rawToken")
            val token = rawToken.replace(Regex("[^\\p{L}\\p{Nd}]"), "").lowercase(Locale.getDefault())
            LOGGER.fine("Cleaned token: $token")
            if (token.isEmpty()) {
                LOGGER.fine("Token is empty after cleaning, skipping.")
                continue
            }
            if (stopwords.contains(token)) {
                LOGGER.fine("Token '$token' is a stopword, skipping.")
                continue
            }
            val stem = stemmer.stripAffixes(token)
            LOGGER.fine("Token '$token' stemmed to '$stem'.")
            stems[stem] = stems.getOrDefault(stem, 0) + 1
        }
        LOGGER.info("Total processed tokens after stopword removal and stemming: ${stems.values.sum()}")
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