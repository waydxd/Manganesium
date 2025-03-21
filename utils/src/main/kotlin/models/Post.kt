package models

import java.io.Serializable

data class Post (
    val pageID: String,
    val frequency: Int,
    val position: List<Int>,
) : Serializable