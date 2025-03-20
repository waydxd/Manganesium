import org.mapdb.DBMaker
import org.mapdb.Serializer

fun main() {
    // Open the database file
    val db = DBMaker.fileDB("crawler.db").make()

    // Access the map you want to inspect
    val urlToPageIdMap = db.hashMap("urlToPageId", Serializer.STRING, Serializer.STRING).createOrOpen()
    val pagePropertiesMap = db.hashMap("pageProperties", Serializer.STRING, Serializer.JAVA).createOrOpen()

    // Print the contents of the urlToPageId map
    println("Contents of urlToPageId map:")
    for ((key, value) in urlToPageIdMap) {
        println("URL: $key -> Page ID: $value")
    }

    // Print the contents of the pageProperties map
    println("\nContents of pageProperties map:")
    for ((key, value) in pagePropertiesMap) {
        println("Page ID: $key -> Page Properties: $value")
    }

    // Close the database
    db.close()
}