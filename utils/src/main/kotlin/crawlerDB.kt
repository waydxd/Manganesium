import io.github.oshai.kotlinlogging.KotlinLogging
import org.mapdb.DBMaker
import org.mapdb.Serializer

fun main() {
    // Open the database file
    val db = DBMaker.fileDB("crawler.db").make()
    val logger = KotlinLogging.logger {}

    // Access the map you want to inspect
    val urlToPageIdMap = db.hashMap("url_to_page_id", Serializer.STRING, Serializer.STRING).createOrOpen()
    val pagePropertiesMap = db.hashMap("page_properties", Serializer.STRING, Serializer.JAVA).createOrOpen()
    val parentChildLinksMap = db.treeMap("parent_child_links", Serializer.STRING, Serializer.JAVA).createOrOpen()
    // Print the contents of the urlToPageId map
    var count = 0
    logger.info { "Contents of urlToPageId map:" }
    for ((key, value) in urlToPageIdMap) {
        if (count++ > 10) {
            break
        }
        logger.info {"URL: $key -> Page ID: $value"}
    }
    count = 0
    // Print the contents of the pageProperties map
    logger.info { "\nContents of pageProperties map:" }
    for ((key, value) in pagePropertiesMap) {
        if (count++ > 10) {
            break
        }
        logger.info {"URL: $key -> Page Properties: $value"}
    }
    count = 0
    // Close the database
    logger.info {"\nContents of parent-child link map:"}
    for ((key, value) in parentChildLinksMap ) {
        if (count++ > 10) {
            break
        }
        logger.info {"PageID: $key -> PageID(s): $value"}
    }
    db.close()
}