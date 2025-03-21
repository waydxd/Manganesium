package models

data class Post (
    val pageID: String,
    val frequency: Int,
    val position: List<Int>,
)