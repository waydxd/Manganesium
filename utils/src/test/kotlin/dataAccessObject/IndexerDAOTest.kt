// Kotlin
package dataAccessObject

import models.Post
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mapdb.DB
import org.mapdb.DBMaker
import org.mapdb.Serializer

class IndexerDAOTest {
 private lateinit var db: DB
 private lateinit var indexerDAO: IndexerDAO

 @BeforeEach
 fun setUp() {
  db = DBMaker.memoryDB().make()
  db.treeMap("inverted_title", Serializer.STRING, Serializer.JAVA).create()
  db.treeMap("inverted_body", Serializer.STRING, Serializer.JAVA).create()
  db.hashSet("stop_words", Serializer.STRING).create()
  db.hashMap("word_to_word_id", Serializer.STRING, Serializer.STRING).create()
  // Pre-create the reverse mapping.
//  db.hashMap("wordIdToWord", Serializer.STRING, Serializer.STRING).create()
  indexerDAO = IndexerDAO(db)
 }

 @AfterEach
 fun tearDown() {
  db.close()
 }

 @Test
 fun testStoreInvertedTitle() {
  val wordId = "testWord"
  val post = Post("1", 5, emptyList())
  indexerDAO.storeInvertedTitle(wordId, post)
  val invertedTitle = db.treeMap("inverted_title", Serializer.STRING, Serializer.JAVA).open()
  val postsSet = invertedTitle[wordId] as? MutableSet<Post>
  assertNotNull(postsSet)
  assertTrue(postsSet!!.contains(post))
 }

 @Test
 fun testStoreInvertedBody() {
  val wordId = "testBody"
  val post = Post("1", 10, emptyList())
  indexerDAO.storeInvertedBody(wordId, post)
  val invertedBody = db.treeMap("inverted_body", Serializer.STRING, Serializer.JAVA).open()
  val postsSet = invertedBody[wordId] as? MutableSet<Post>
  assertNotNull(postsSet)
  assertTrue(postsSet!!.contains(post))
 }

 @Test
 fun testGetPagesBodyForKeyword() {
  val wordId = "bodyTest"
  val post1 = Post("page1", 1, emptyList())
  val post2 = Post("page2", 1, emptyList())
  val mapping = db.treeMap("inverted_body", Serializer.STRING, Serializer.JAVA).open() as MutableMap<String, Any>
  mapping[wordId] = mutableSetOf(post1, post2)
  val pages = indexerDAO.getPagesBodyForKeyword(wordId)
  assertEquals(listOf(post1, post2), pages)
 }

 @Test
 fun testGetPagesTitleForKeyword() {
  val wordId = "titleTest"
  val postA = Post("pageA", 1, emptyList())
  val postB = Post("pageB", 1, emptyList())
  val mapping = db.treeMap("inverted_title", Serializer.STRING, Serializer.JAVA).open() as MutableMap<String, Any>
  mapping[wordId] = mutableSetOf(postA, postB)
  val pages = indexerDAO.getPagesTitleForKeyword(wordId)
  assertEquals(listOf(postA, postB), pages)
 }

 @Test
 fun testStoreStopWords() {
  val stops = setOf("a", "the", "an")
  indexerDAO.storeStopWords(stops)
  val storedStops = indexerDAO.getStopWords()
  assertTrue(stops.all { it in storedStops })
 }

 @Test
 fun testStoreWordIdToWordMapping() {
  val word = "test"
  val id1 = indexerDAO.storeWordIdToWordMapping(word)
  val id2 = indexerDAO.storeWordIdToWordMapping(word)
  assertEquals(id1, id2)
  val mapping = db.hashMap("word_to_word_id", Serializer.STRING, Serializer.STRING).open()
  assertEquals(id1, mapping[word])
 }

 @Test
 fun testGetWordIdForWord() {
  val word = "anotherTest"
  val wordId = indexerDAO.storeWordIdToWordMapping(word)
  val retrieved = indexerDAO.getWordIdForWord(word)
  assertEquals(wordId, retrieved)
 }
}