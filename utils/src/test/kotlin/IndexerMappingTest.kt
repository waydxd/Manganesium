package indexer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertFalse
import org.mapdb.DBMaker
import org.mapdb.Serializer
import java.io.File
import java.util.*

class IndexerMappingTest {
    @Test
    fun testStopwordsAreSavedInWordIdMapping() {
        val db = DBMaker.fileDB("/Users/wayd/IdeaProjects/COMP4321-Group25/indexer.db").make()
        try {
            val wordToWordIdMap = db.hashMap("word_to_word_id", Serializer.STRING, Serializer.STRING).createOrOpen()

            assertFalse(wordToWordIdMap.isEmpty())
            val stopwords = HashSet<String>()
            val file = File("/Users/wayd/IdeaProjects/COMP4321-Group25/indexer/src/main/kotlin/org/manganesium/indexer/stopwords.txt")
            val reader = Scanner(file)
            while (reader.hasNextLine()) {
                val data = reader.nextLine()
                stopwords.add(data)
            }
            stopwords.forEach { word ->
                println("Checking for stopword: $word")
                println("Contain?" + wordToWordIdMap[word])
                assertFalse(
                    wordToWordIdMap.containsKey(word),
                    "Stopword \\`$word\\` is present in the word_to_word_id mapping"
                )
            }
        } finally {
            db.close()
        }
    }
}