package org.manganesium.models

data class Page(
    val id: Long,
    val url: String,
    val title: String? = null,
    val content: String = "",
    val lastModified: String? = null,
    val size: Int = 0,
    val keywords: Map<String, Int> = emptyMap(),
    val links: List<String> = emptyList()
)