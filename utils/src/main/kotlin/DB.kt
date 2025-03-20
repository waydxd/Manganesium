import io.github.oshai.kotlinlogging.KotlinLogging
import org.mapdb.DBMaker
import org.mapdb.Serializer
import kotlin.math.log

fun main() {
    // Open the database file
    val db = DBMaker.fileDB("crawler.db").make()
    val logger = KotlinLogging.logger {}

    // Access the map you want to inspect
    val urlToPageIdMap = db.hashMap("url_to_page_id", Serializer.STRING, Serializer.STRING).createOrOpen()
    val pagePropertiesMap = db.hashMap("page_properties", Serializer.STRING, Serializer.JAVA).createOrOpen()

    // Print the contents of the urlToPageId map
    println("Contents of urlToPageId map:")
    for ((key, value) in urlToPageIdMap) {
        logger.info {"URL: $key -> Page ID: $value"}
    }

    // Print the contents of the pageProperties map
    println("\nContents of pageProperties map:")
    for ((key, value) in pagePropertiesMap) {
        logger.info {"URL: $key -> Page Properties: $value"}
    }

    // Close the database
    db.close()
}