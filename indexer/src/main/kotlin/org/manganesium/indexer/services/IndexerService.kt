package org.manganesium.indexer.services

import dataAccessObject.CrawlerDAO
import io.github.oshai.kotlinlogging.KotlinLogging
import org.manganesium.dataAccessObject.IndexerDAO
import org.manganesium.indexer.KeywordProcessor
import org.manganesium.indexer.Porter
import java.io.File
import java.util.*
import java.util.logging.Logger;

private val logger = KotlinLogging.logger {}

class IndexerService(val indDAO: IndexerDAO)  {

}