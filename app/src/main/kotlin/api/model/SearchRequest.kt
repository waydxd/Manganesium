package app.api.model

/**
 * SearchRequest.kt
 *
 * This file defines the SearchRequest data class, which is used to represent a search request in the application.
 *
 * The SearchRequest class contains the following properties:
 * - query: A string representing the search query.
 * - limit: An integer representing the maximum number of results to return.
 * - offset: An integer representing the offset for pagination (used for skipping a number of results before starting to return results).
 */
data class SearchRequest(
    val query: String,
    val limit: Int,
    val offset: Int,
)
