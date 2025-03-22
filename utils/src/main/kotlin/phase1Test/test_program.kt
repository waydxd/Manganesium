package phase1Test

import models.Keyword
import org.mapdb.DBMaker
import org.mapdb.Serializer
import java.io.File
import java.io.PrintWriter
import java.security.Key

data class PageProperties(
    val title: String? = null,
    val lastModified: String? = null,
    val size: Int = 0,
    val url: String,
    val keywords: Keyword? = null,
)

fun main() {
    // Open crawler database
    val crawlerDb = DBMaker.fileDB("crawler.db").make()

    // Open indexer database
    val indexerDb = DBMaker.fileDB("indexer.db").make()

    // Get page properties from crawler.db
    val pagePropertiesMap = crawlerDb.hashMap("page_properties", Serializer.STRING, Serializer.JAVA)
        .createOrOpen()

    // Get parent-child links from crawler.db
    val parentChildLinksMap = crawlerDb.treeMap("parent_child_links", Serializer.STRING, Serializer.JAVA)
        .createOrOpen()

    // Get forward index (keywords) from indexer.db
    val forwardIndex = indexerDb.hashMap("forward_index", Serializer.STRING, Serializer.JAVA)
        .createOrOpen()

    // Debug information - print first entry to debug
    println("Number of pages stored: ${pagePropertiesMap.size}")
    println("Number of entries in forward index: ${forwardIndex.size}")

    if (pagePropertiesMap.isNotEmpty()) {
        val firstKey = pagePropertiesMap.keys.first()
        println("First page ID: $firstKey")
        println("First page properties: ${pagePropertiesMap[firstKey]}")
        println("First page forward index: ${forwardIndex[firstKey]}")
    }

    val outputFile = File("spider_result.txt")
    PrintWriter(outputFile).use { writer ->
        writer.println(pagePropertiesMap.size)

        pagePropertiesMap.forEach { (pageId, props) ->
            writer.println("--")

            val propsMap = props as LinkedHashMap<*, *>
            val pageProps = PageProperties(
                title = propsMap["title"] as String,
                lastModified = propsMap["lastModified"] as String,
                size = propsMap["size"] as Int,
                url = propsMap["url"] as String,
                keywords = propsMap["keywords"] as Keyword?
            )
            writer.println(pageProps.title)
            writer.println(pageProps.url)
            writer.println("${pageProps.lastModified}, ${pageProps.size}")

// Get keywords from forward index
            val keywords = forwardIndex[pageId] as? List<Keyword> ?: emptyList()

// Get the wordIDToWord mapping
            val wordIDToWordMap = indexerDb.hashMap("word_id_to_word", Serializer.STRING, Serializer.STRING)
                .createOrOpen()

// Format the keywords for output with actual words
            val keywordsLine = keywords.joinToString(separator = "; ") {
                val wordID = it.wordID
                val word = wordIDToWordMap[wordID] ?: wordID // Fallback to ID if word not found
                "$word ${it.frequency}"
            }
            writer.println(keywordsLine)

            val childLinks = parentChildLinksMap[pageId] as? List<String> ?: emptyList()

            childLinks.forEach { childId ->
                val childPropsRaw = pagePropertiesMap[childId]
                if (childPropsRaw != null) {
                    val childPropsMap = childPropsRaw as LinkedHashMap<*, *>
                    val childUrl = childPropsMap["url"] as String
                    writer.println(childUrl)
                }
            }

            writer.println("------------------------------")
        }
    }

    crawlerDb.close()
    indexerDb.close()
    println("Spider result written to spider result.txt")
}