package app.api.model

/**
 * SearchResponse.kt
 *
 * This file defines the SearchResponse data class, which is used to represent a search response in the application.
 *
 * The SearchResponse class contains the following properties:
 * - pageID: A string representing the ID of the page.
 * - title: A string representing the title of the page.
 * - url: A string representing the URL of the page.
 * - lastModified: A string representing the last modified date of the page.
 * - snippet: A string representing a snippet of content from the page.
 */
data class SearchResponse(
    val pageID: String,
    val title: String,
    val url: String,
    val lastModified: String,
    val snippet: String,
)
