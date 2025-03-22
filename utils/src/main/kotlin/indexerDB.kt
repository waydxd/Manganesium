import io.github.oshai.kotlinlogging.KotlinLogging
import org.mapdb.DBMaker
import org.mapdb.Serializer

fun main() {
    // Open the database file
    val db = DBMaker.fileDB("indexer.db").make()
    val logger = KotlinLogging.logger {}

    // Access the maps you want to inspect
    val invertedTitleMap = db.treeMap("inverted_title", Serializer.STRING, Serializer.JAVA).createOrOpen()
    val invertedBodyMap = db.treeMap("inverted_body", Serializer.STRING, Serializer.JAVA).createOrOpen()
    val wordToWordIDMap = db.hashMap("word_to_word_id", Serializer.STRING, Serializer.STRING).createOrOpen()
    val forwardIndex = db.hashMap("forward_index", Serializer.STRING, Serializer.JAVA).createOrOpen()
    val wordIDToWordMap = db.hashMap("word_id_to_word", Serializer.STRING, Serializer.STRING).createOrOpen()

    // Print the contents of the invertedTitle map
    var count = 0
    logger.info { "Contents of invertedTitle map:" }
    for ((key, value) in invertedTitleMap) {
        if (count++ > 10) {
            break
        }
        logger.info { "Word ID: $key -> Posts: $value" }
    }
    count = 0

    // Print the contents of the invertedBody map
    logger.info { "\nContents of invertedBody map:" }
    for ((key, value) in invertedBodyMap) {
        if (count++ > 10) {
            break
        }
        logger.info { "Word ID: $key -> Posts: $value" }
    }
    count = 0

    // Print the contents of the wordToWordID map
    logger.info { "\nContents of wordToWordID map:" }
    for ((key, value) in wordToWordIDMap) {
        if (count++ > 10) {
            break
        }
        logger.info { "Word: $key -> Word ID: $value" }
    }
    count = 0

    // Print the contents of the wordToWordID map
    logger.info { "\nContents of wordIDToWord map:" }
    for ((key, value) in wordIDToWordMap) {
        if (count++ > 10) {
            break
        }
        logger.info { "Word ID: $key -> Word: $value" }
    }
    count = 0

    logger.info { "\nContents of forward index map:" }
    for ((key, value ) in forwardIndex) {
        if (count++ > 10) {
            break
        }
        logger.info { "Page ID: $key -> keywords: $value" }
    }

    // Close the database
    db.close()
}