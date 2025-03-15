package org.manganesium.models

data class Page(
    val id: Long,
    val url: String,
    val title: String?,
    val content: String,
    val lastModified: String?,
    val size: Int,
    val keywords: Map<String, Int>,
    val links: List<String>
)