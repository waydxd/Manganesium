package service

import SynchronizedLRUCache
import dataAccessObject.CrawlerDAO
import dataAccessObject.IndexerDAO
import models.Keyword
import models.Post
import kotlin.math.log10
import models.ScoredResult

class SearchService(
    private val crawlerDao: CrawlerDAO,
    private val indexerDao: IndexerDAO,
    private val wordIdCacheSize: Int = 1000,
    private val postingsCacheSize: Int = 500,
    private val resultsCacheSize: Int = 100,
    private val maxTfCacheSize: Int = 300
) {
    private val wTitle = 10.0
    private val wBody = 1.0
    private val numberDocs = crawlerDao.getAllPageIds().size
    private val alpha = 0.7
    private val dampingFactor = 0.85
    private val maxIterations = 10

    private val wordIdCache = SynchronizedLRUCache<String, String?>(wordIdCacheSize)
    private val titlePostingsCache = SynchronizedLRUCache<String, List<Post>>(postingsCacheSize)
    private val bodyPostingsCache = SynchronizedLRUCache<String, List<Post>>(postingsCacheSize)
    private val resultsCache = SynchronizedLRUCache<String, List<ScoredResult>>(resultsCacheSize)
    private val maxTfTitleCache = SynchronizedLRUCache<String, Int>(maxTfCacheSize)
    private val maxTfBodyCache = SynchronizedLRUCache<String, Int>(maxTfCacheSize)
    private val pageRankCache = SynchronizedLRUCache<String, Double>(maxTfCacheSize)
    private val documentLengthCache = SynchronizedLRUCache<String, Int>(maxTfCacheSize)

    /**
     * Searches the index for documents matching the query and returns ranked document IDs with scores.
     * @param query The user query string containing terms and phrases
     * @return List of scored results in descending order of relevance
     */
    fun search(query: String): List<ScoredResult> {
        resultsCache.get(query)?.let { return it }

        val termScores = calculateTermScores(query)
        val pageRanks = calculatePageRanks()

        val finalScores = mutableListOf<ScoredResult>()

        for ((pageId, termScore) in termScores) {
            val pageRank = pageRanks[pageId] ?: 0.0
            val finalScore = alpha * termScore + (1 - alpha) * pageRank
            finalScores.add(ScoredResult(pageId, finalScore))
        }

        // Normalize final scores
        val maxScore = finalScores.maxOfOrNull { it.score } ?: 0.0
        val normalizedScores = if (maxScore > 0) {
            finalScores.map { ScoredResult(it.pageId, it.score / maxScore) }
        } else {
            finalScores
        }

        val rankedResults = normalizedScores.sortedByDescending { it.score }.take(50)
        resultsCache.put(query, rankedResults)
        return rankedResults
    }

    /**
     * Calculate term scores for a query
     */
    private fun calculateTermScores(query: String): Map<String, Double> {
        val components = parseQuery(query)
        val scores = mutableMapOf<String, Double>()

        for (component in components) {
            if (component.startsWith("\"") && component.endsWith("\"")) {
                val phrase = component.trim('"').split("\\s+".toRegex())
                if (phrase.size > 1) processPhrase(phrase, scores)
                else processTerm(phrase[0], scores)
            } else {
                processTerm(component, scores)
            }
        }

        return scores
    }


    /**
     * Calculate PageRank scores for all pages based on incoming links.
     * This is a simplified implementation that normalizes by the maximum number of links.
     */
    private fun calculatePageRanks(): Map<String, Double> {
        // Try getting from cache
        if (pageRankCache.size() > 0) {
            val allPageIds = crawlerDao.getAllPageIds()
            val cachedRanks = allPageIds.mapNotNull { pageId ->
                pageRankCache.get(pageId)?.let { pageId to it }
            }
            if (cachedRanks.size == allPageIds.size) {
                return cachedRanks.toMap()
            }
        }

        val allPages = crawlerDao.getAllPageIds().toSet()
        val incomingLinksMap = mutableMapOf<String, MutableSet<String>>()
        val outgoingLinksMap = mutableMapOf<String, MutableSet<String>>()

        allPages.forEach { pageId ->
            incomingLinksMap[pageId] = crawlerDao.getParentPages(pageId).toMutableSet()
            outgoingLinksMap[pageId] = crawlerDao.getChildPages(pageId).toMutableSet()
        }

        var pageRanks = allPages.associateWith { 1.0 / allPages.size }.toMutableMap()
        val newPageRanks = mutableMapOf<String, Double>()

        for (i in 0 until maxIterations) {
            for (page in allPages) {
                var rank = (1 - dampingFactor) / allPages.size

                for (linkingPage in incomingLinksMap[page] ?: emptySet()) {
                    val numOutgoingLinks = outgoingLinksMap[linkingPage]?.size ?: 0
                    rank += dampingFactor * (pageRanks[linkingPage] ?: 0.0) / (numOutgoingLinks.toDouble().takeIf { it > 0 } ?: 1.0)
                }
                newPageRanks[page] = rank
            }
            // Check for convergence
            val delta = newPageRanks.map { (page, newRank) ->
                kotlin.math.abs(newRank - (pageRanks[page] ?: 0.0))
            }.sum()

            if (delta < 1e-5) {
                break
            }
            pageRanks = newPageRanks.toMutableMap()
        }

        // Normalize and cache results
        val sumRanks = pageRanks.values.sum()
        val normalizedRanks = pageRanks.mapValues { (_, rank) -> rank / sumRanks }

        normalizedRanks.forEach { (pageId, score) ->
            pageRankCache.put(pageId, score)
        }

        return normalizedRanks
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
            while (i < chars.size && chars[i].isWhitespace()) i++
            if (i >= chars.size) break

            if (chars[i] == '"') {
                val start = i
                i++
                while (i < chars.size && chars[i] != '"') i++
                if (i < chars.size) {
                    components.add(query.substring(start, i + 1))
                    i++
                }
            } else {
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
        val wordId = wordIdCache.getOrPut(term) { indexerDao.getWordIdForWord(term) } ?: return

        val titlePostings = titlePostingsCache.getOrPut(wordId) { indexerDao.getPagesTitleForKeyword(wordId) }
        val bodyPostings = bodyPostingsCache.getOrPut(wordId) { indexerDao.getPagesBodyForKeyword(wordId) }

        val allDocs = (titlePostings.map { it.pageID } + bodyPostings.map { it.pageID }).toSet()
        val df = allDocs.size
        if (df == 0) return
        val idf = log10(numberDocs.toDouble() / df)

        for (post in titlePostings + bodyPostings) {
            val pageId = post.pageID
            val tfTitle = titlePostings.find { it.pageID == pageId }?.frequency ?: 0
            val tfBody = bodyPostings.find { it.pageID == pageId }?.frequency ?: 0
            val maxTfTitle = getMaxTfTitle(pageId)
            val maxTfBody = getMaxTfBody(pageId)
            // Incorporate document length
            val docLength = getDocumentLength(pageId)
            val weightTitle = if (maxTfTitle > 0) (tfTitle.toDouble() / maxTfTitle) * idf / docLength else 0.0
            val weightBody = if (maxTfBody > 0) (tfBody.toDouble() / maxTfBody) * idf / docLength else 0.0
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
        if (wordIds.size != phrase.size) return

        val candidateDocs = findCandidateDocs(wordIds)
        val phraseDocs = mutableSetOf<String>()
        val tfPhraseTitle = mutableMapOf<String, Int>()
        val tfPhraseBody = mutableMapOf<String, Int>()

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

        val dfPhrase = phraseDocs.size
        if (dfPhrase == 0) return
        val idfPhrase = log10(numberDocs.toDouble() / dfPhrase)

        for (doc in phraseDocs) {
            val tfTitle = tfPhraseTitle.getOrDefault(doc, 0)
            val tfBody = tfPhraseBody.getOrDefault(doc, 0)
            val maxTfTitle = getMaxTfTitle(doc)
            val maxTfBody = getMaxTfBody(doc)
            // Incorporate document length
            val docLength = getDocumentLength(doc)
            val weightTitle = if (maxTfTitle > 0) (tfTitle.toDouble() / maxTfTitle) * idfPhrase / docLength else 0.0
            val weightBody = if (maxTfBody > 0) (tfBody.toDouble() / maxTfBody) * idfPhrase / docLength else 0.0
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
        val indices = IntArray(k) { 0 }
        var count = 0

        // Sort position lists for each term
        val sortedPosLists = posLists.map { it.sorted() }

        while (true) {
            // Get current positions
            val currentPositions = (0 until k).map { i ->
                sortedPosLists[i].getOrNull(indices[i]) ?: return count // If any list is exhausted, return
            }

            // Check if all positions are valid (not null)
            if (currentPositions.any { it == null }) {
                break // If any list is exhausted, exit loop
            }

            // Check if the positions are consecutive
            if ((1 until k).all { currentPositions[it] == currentPositions[it - 1]!! + 1 }) {
                count++ // Increment count if consecutive

                // Advance all indices
                for (i in 0 until k) {
                    indices[i]++
                }
            } else {
                // Find the minimum position and advance its index
                val minPosition = currentPositions.filterNotNull().minOrNull() ?: break
                val minIndex = currentPositions.indexOf(minPosition)
                indices[minIndex]++
            }

            // Check if any index has reached the end of its list
            if ((0 until k).any { indices[it] >= sortedPosLists[it].size }) {
                break // Exit loop if any list is exhausted
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
            val bodyKeywords = keywords.filter { keyword ->
                indexerDao.getPagesBodyForKeyword(keyword.wordID).any { it.pageID == pageId }
            }
            bodyKeywords.maxOfOrNull { it.frequency } ?: 1
        }
    }

    /**
     * Computes the document length for normalization.
     */
    private fun getDocumentLength(pageId: String): Int {
        return documentLengthCache.getOrPut(pageId) {
            val keywords = indexerDao.forwardIndex[pageId] as? List<Keyword> ?: return@getOrPut 1
            keywords.sumOf { it.frequency }
        }
    }
}