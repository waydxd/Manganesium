package org.manganesium.crawler.services

import dataAccessObject.CrawlerDAO
import java.io.File

class CrawlerService {
    private val dbFile = File("manganesium.db")
    val crawlerDao = CrawlerDAO(dbFile)

}