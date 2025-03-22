package models

import java.io.Serializable

data class Keyword(
    val wordID: String,
    val frequency: Int
) : Serializable