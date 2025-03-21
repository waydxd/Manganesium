package models

data class Post (
    val wordID: String,
    val frequency: Int,
    val position: List<Int>,
)