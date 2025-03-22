package org.manganesium.indexer

import dataAccessObject.IndexerDAO
import models.Page

fun main() {
    val kwP = KeywordProcessor()
    val Ixer = Indexer()

    val testpage = Page("UUID", "https://www.google.com", "page title", "page title is a what content", "2021-09-01", 100, listOf("https://www.google.com/about"))

    Ixer.indexPage(testpage)

}