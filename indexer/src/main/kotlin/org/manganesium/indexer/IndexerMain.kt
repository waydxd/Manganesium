package org.manganesium.indexer

import models.Page

fun main() {
    val kwP = KeywordProcessor()
    val Ixer = Indexer()
//    val indexerDao = IndexerDAO("indexer.db")

    val testpage = Page("UUID", "https://www.google.com", "Google", "Google is a search engine", "2021-09-01", 100, listOf("https://www.google.com/about"))

    Ixer.indexPage(testpage)

}