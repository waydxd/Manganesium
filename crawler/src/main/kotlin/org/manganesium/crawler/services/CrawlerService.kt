package org.manganesium.crawler.services

import java.io.File
import dataAccessObject.CrawlerDAO

class CrawlerService {
    private val dbFile = File("manganesium.db")
    val crawlerDao = CrawlerDAO(dbFile)

}