package models

import java.io.Serializable

data class Post(
    val pageId: String,
    val frequency: Int,
    val positions: List<Int>
) : Serializable