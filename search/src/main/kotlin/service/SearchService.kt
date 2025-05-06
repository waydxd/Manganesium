package service

import SynchronizedLRUCache
import dataAccessObject.CrawlerDAO
import dataAccessObject.IndexerDAO
import models.Keyword
import models.Post
import kotlin.math.log10


class SearchService(
    private val crawlerDao: CrawlerDAO,
    private val indexerDao: IndexerDAO,
    private val wordIdCacheSize: Int = 1000,
    private val postingsCacheSize: Int = 500,
    private val resultsCacheSize: Int = 100,
    private val maxTfCacheSize: Int = 300 // Cache for all 300 pages
) {
    // Weights to favor title matches
    private val wTitle = 10.0
    private val wBody = 1.0
    // Total number of documents (assumed 300 as per project spec)
    private val numberDocs = 300

    // Thread-safe caches
    private val wordIdCache = SynchronizedLRUCache<String, String?>(wordIdCacheSize)
    private val titlePostingsCache = SynchronizedLRUCache<String, List<Post>>(postingsCacheSize)
    private val bodyPostingsCache = SynchronizedLRUCache<String, List<Post>>(postingsCacheSize)
    private val resultsCache = SynchronizedLRUCache<String, List<String>>(resultsCacheSize)
    private val maxTfTitleCache = SynchronizedLRUCache<String, Int>(maxTfCacheSize)
    private val maxTfBodyCache = SynchronizedLRUCache<String, Int>(maxTfCacheSize)

    /**
     * Searches the index for documents matching the query and returns the top 50 ranked document IDs.
     * @param query The user query string containing terms and phrases (e.g., "hong kong" universities)
     * @return List of up to 50--page IDs in descending order of relevance
     */
    fun search(query: String): List<String> {
        // Check results cache first
        resultsCache.get(query)?.let { return it }

        val components = parseQuery(query)
        val scores = mutableMapOf<String, Double>()

        // Process each query component (term or phrase)
        for (component in components) {
            if (component.startsWith("\"") && component.endsWith("\"")) {
                // Handle phrase query
                val phrase = component.trim('"').split("\\s+".toRegex())
                if (phrase.size > 1) processPhrase(phrase, scores)
                else processTerm(phrase[0], scores) // Single-word phrase treated as term
            } else {
                // Handle individual term
                processTerm(component, scores)
            }
        }

        // Rank documents by score and select top 50
        val rankedResults = scores.entries
            .sortedByDescending { it.value }
            .take(50)
            .map { it.key }

        // Store in results cache
        resultsCache.put(query, rankedResults)
        return rankedResults
    }

    /**
     * Parses the query into terms and phrases.
     * Terms are separated by spaces; phrases are enclosed in double quotes.
     */
    private fun parseQuery(query: String): List<String> {
        val components = mutableListOf<String>()
        var i = 0
        val chars = query.trim().toCharArray()

        while (i < chars.size) {
            // Skip whitespace
            while (i < chars.size && chars[i].isWhitespace()) i++
            if (i >= chars.size) break

            if (chars[i] == '"') {
                // Start of a phrase
                val start = i
                i++ // Skip opening quote
                while (i < chars.size && chars[i] != '"') i++
                if (i < chars.size) {
                    components.add(query.substring(start, i + 1)) // Include quotes
                    i++ // Skip closing quote
                }
            } else {
                // Start of a term
                val start = i
                while (i < chars.size && !chars[i].isWhitespace()) i++
                components.add(query.substring(start, i))
            }
        }
        return components
    }

    /**
     * Processes an individual term and updates document scores.
     */
    private fun processTerm(term: String, scores: MutableMap<String, Double>) {
        // Check word ID cache
        val wordId = wordIdCache.getOrPut(term) { indexerDao.getWordIdForWord(term) } ?: return
        // Check postings caches
        val titlePostings = titlePostingsCache.getOrPut(wordId) { indexerDao.getPagesTitleForKeyword(wordId) }
        val bodyPostings = bodyPostingsCache.getOrPut(wordId) { indexerDao.getPagesBodyForKeyword(wordId) }

        // Compute document frequency (df) and inverse document frequency (idf)
        val allDocs = (titlePostings.map { it.pageID } + bodyPostings.map { it.pageID }).toSet()
        val df = allDocs.size
        if (df == 0) return
        val idf = log10(numberDocs.toDouble() / df)

        // Score each document containing the term
        val postings = titlePostings + bodyPostings
        for (post in postings) {
            val pageId = post.pageID
            val tfTitle = titlePostings.find { it.pageID == pageId }?.frequency ?: 0
            val tfBody = bodyPostings.find { it.pageID == pageId }?.frequency ?: 0
            val maxTfTitle = getMaxTfTitle(pageId)
            val maxTfBody = getMaxTfBody(pageId)

            val weightTitle = if (maxTfTitle > 0) (tfTitle.toDouble() / maxTfTitle) * idf else 0.0
            val weightBody = if (maxTfBody > 0) (tfBody.toDouble() / maxTfBody) * idf else 0.0
            val docWeight = wTitle * weightTitle + wBody * weightBody

            scores[pageId] = scores.getOrDefault(pageId, 0.0) + docWeight
        }
    }

    /**
     * Processes a phrase and updates document scores.
     */
    private fun processPhrase(phrase: List<String>, scores: MutableMap<String, Double>) {
        val wordIds = phrase.mapNotNull { word ->
            wordIdCache.getOrPut(word) { indexerDao.getWordIdForWord(word) }
        }
        if (wordIds.size != phrase.size) return // Skip if any term is not indexed

        // Find candidate documents containing all terms
        val candidateDocs = findCandidateDocs(wordIds)
        val phraseDocs = mutableSetOf<String>()
        val tfPhraseTitle = mutableMapOf<String, Int>()
        val tfPhraseBody = mutableMapOf<String, Int>()

        // Compute phrase frequencies for each candidate document
        for (doc in candidateDocs) {
            val titlePositions = wordIds.map { getPositions(it, doc, "title") }
            val bodyPositions = wordIds.map { getPositions(it, doc, "body") }
            val tfTitle = computePhraseFrequency(titlePositions)
            val tfBody = computePhraseFrequency(bodyPositions)

            if (tfTitle > 0) {
                tfPhraseTitle[doc] = tfTitle
                phraseDocs.add(doc)
            }
            if (tfBody > 0) {
                tfPhraseBody[doc] = tfBody
                phraseDocs.add(doc)
            }
        }

        // Compute idf and score documents
        val dfPhrase = phraseDocs.size
        if (dfPhrase == 0) return
        val idfPhrase = log10(numberDocs.toDouble() / dfPhrase)

        for (doc in phraseDocs) {
            val tfTitle = tfPhraseTitle.getOrDefault(doc, 0)
            val tfBody = tfPhraseBody.getOrDefault(doc, 0)
            val maxTfTitle = getMaxTfTitle(doc)
            val maxTfBody = getMaxTfBody(doc)

            val weightTitle = if (maxTfTitle > 0) (tfTitle.toDouble() / maxTfTitle) * idfPhrase else 0.0
            val weightBody = if (maxTfBody > 0) (tfBody.toDouble() / maxTfBody) * idfPhrase else 0.0
            val docWeight = wTitle * weightTitle + wBody * weightBody

            scores[doc] = scores.getOrDefault(doc, 0.0) + docWeight
        }
    }

    /**
     * Finds candidate documents containing all terms in the phrase (in any order).
     */
    private fun findCandidateDocs(wordIds: List<String>): Set<String> {
        val docSets = wordIds.map { wid ->
            val titleDocs = titlePostingsCache.getOrPut(wid) { indexerDao.getPagesTitleForKeyword(wid) }.map { it.pageID }
            val bodyDocs = bodyPostingsCache.getOrPut(wid) { indexerDao.getPagesBodyForKeyword(wid) }.map { it.pageID }
            (titleDocs + bodyDocs).toSet()
        }
        return docSets.reduce { acc, set -> acc.intersect(set) }
    }

    /**
     * Retrieves position list for a term in a document for a specific field.
     */
    private fun getPositions(wordId: String, pageId: String, field: String): List<Int> {
        val postings = if (field == "title") {
            titlePostingsCache.getOrPut(wordId) { indexerDao.getPagesTitleForKeyword(wordId) }
        } else {
            bodyPostingsCache.getOrPut(wordId) { indexerDao.getPagesBodyForKeyword(wordId) }
        }
        val post = postings.find { it.pageID == pageId }
        return post?.position ?: emptyList()
    }

    /**
     * Computes the number of times a phrase appears given position lists.
     */
    private fun computePhraseFrequency(posLists: List<List<Int>>): Int {
        if (posLists.any { it.isEmpty() } || posLists.isEmpty()) return 0
        val k = posLists.size
        val sortedLists = posLists.map { it.sorted() }
        var count = 0
        val indices = IntArray(k) { 0 }

        while (indices[0] < sortedLists[0].size) {
            val positions = (0 until k).map { i ->
                sortedLists[i][indices[i]]
            }
            if ((1 until k).all { positions[it] == positions[it - 1] + 1 }) {
                count++
                // Advance all indices
                for (i in 0 until k) {
                    indices[i]++
                    if (indices[i] >= sortedLists[i].size && i < k - 1) return count
                }
            } else {
                // Advance the index of the smallest position
                val minPos = positions.minOrNull()!!
                val minIdx = positions.indexOfFirst { it == minPos }
                indices[minIdx]++
                if (indices[minIdx] >= sortedLists[minIdx].size) break
            }
        }
        return count
    }

    /**
     * Computes max_tf for the title of a page using the forward index.
     */
    private fun getMaxTfTitle(pageId: String): Int {
        return maxTfTitleCache.getOrPut(pageId) {
            val keywords = indexerDao.forwardIndex[pageId] as? List<Keyword> ?: return@getOrPut 1
            // Filter keywords that appear in the title (check invertedTitle)
            val titleKeywords = keywords.filter { keyword ->
                indexerDao.getPagesTitleForKeyword(keyword.wordID).any { it.pageID == pageId }
            }
            titleKeywords.maxOfOrNull { it.frequency } ?: 1
        }
    }

    /**
     * Computes max_tf for the body of a page using the forward index.
     */
    private fun getMaxTfBody(pageId: String): Int {
        return maxTfBodyCache.getOrPut(pageId) {
            val keywords = indexerDao.forwardIndex[pageId] as? List<Keyword> ?: return@getOrPut 1
            // Filter keywords that appear in the body (check invertedBody)
            val bodyKeywords = keywords.filter { keyword ->
                indexerDao.getPagesBodyForKeyword(keyword.wordID).any { it.pageID == pageId }
            }
            bodyKeywords.maxOfOrNull { it.frequency } ?: 1
        }
    }
}