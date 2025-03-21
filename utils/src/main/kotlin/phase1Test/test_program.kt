package phase1Test

import org.mapdb.DBMaker
import org.mapdb.Serializer
import java.io.File
import java.io.PrintWriter

// A sample PageProperties class representing the stored page information.
data class PageProperties(
    val title: String,
    val url: String,
    val lastModified: String,
    val size: String,
    val keywords: Map<String, Int>  // keyword -> frequency
)

fun main() {
    // Open the crawler.db
    val db = DBMaker.fileDB("crawler.db").make()

    // Open the maps
    val pagePropertiesMap = db.hashMap<String, Any>("page_properties", Serializer.STRING, Serializer.JAVA)
        .createOrOpen() as Map<String, Any>

    val parentChildLinksMap = db.treeMap<String, Any>("parent_child_links", Serializer.STRING, Serializer.JAVA)
        .createOrOpen() as Map<String, Any>

    // Convert the values from pagePropertiesMap to PageProperties objects.
    // It is assumed that these objects were stored as PageProperties.
    val pages = pagePropertiesMap.mapNotNull { (_, value) ->
        value as? PageProperties
    }

    // Create a lookup map from page id to PageProperties for child link resolution.
    // Here we assume that the keys in pagePropertiesMap correspond to page IDs.
    val pageIdToProperties = pagePropertiesMap.mapNotNull { (pageId, value) ->
        (value as? PageProperties)?.let { pageId to it }
    }.toMap()

    val outputFile = File("spider result.txt")
    PrintWriter(outputFile).use { writer ->
        // Write pages count on first line.
        writer.println(pages.size)
        // Iterate over each page and output in the specified format.
        pages.forEach { page ->
            // Start-of-page marker
            writer.println("-")
            // Page title
            writer.println(page.title)
            // URL
            writer.println(page.url)
            // Last modification date and size of page (comma-separated)
            writer.println("${page.lastModified}, ${page.size}")
            // List up to 10 keywords with frequencies, joined by semicolon and space.
            val keywordsLine = page.keywords.entries.take(10)
                .joinToString(separator = "; ") { "${it.key} ${it.value}" }
            writer.println(keywordsLine)
            // Get child links from parentChildLinksMap if any.
            // Assume that parentChildLinksMap values are collections (List or Set) of page IDs.
            val childIds = parentChildLinksMap[page.url]?.let {  // if stored key matches URL
                it as? Collection<*>
            } ?: emptyList<Any>()
            // Display up to 10 child links (using the URL value from the child page properties).
            childIds.take(10).forEach { childId ->
                val childPage = pageIdToProperties[childId.toString()]
                if (childPage != null) {
                    writer.println(childPage.url)
                }
            }
            // Separate pages using a line of hyphens (30 hyphens)
            writer.println("-".repeat(30))
        }
    }

    db.close()
}