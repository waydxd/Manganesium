// File: utils/src/test/kotlin/dataAccessObject/CrawlerDAOTest.kt
import dataAccessObject.CrawlerDAO
import models.Page
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mapdb.*

class CrawlerDAOTest {
 private lateinit var db: DB
 private lateinit var urlToPageId: HTreeMap<String, String>
 private lateinit var forwardIndex: HTreeMap<String, Any>
 private lateinit var parentChildLinks: BTreeMap<String, Any>
 private lateinit var pageProperties: HTreeMap<String, Any>
 private lateinit var crawlerDAO: CrawlerDAO

 @BeforeEach
 fun setUp() {
  // Create an in-memory DB to avoid file locking and empty file corruption issues.
  db = DBMaker.memoryDB().make()

  // Initialize (or create) the maps used in the DAO.
  urlToPageId = db.hashMap("url_to_page_id", Serializer.STRING, Serializer.STRING).create()
  forwardIndex = db.hashMap("forward_index", Serializer.STRING, Serializer.JAVA).create()
  // Change to treeMap to match the expected type in the DAO.
  parentChildLinks = db.treeMap("parent_child_links", Serializer.STRING, Serializer.JAVA).create()
  pageProperties = db.hashMap("page_properties", Serializer.STRING, Serializer.JAVA).create()

  // Instantiate DAO by providing the DB instance.
  crawlerDAO = CrawlerDAO(db)
 }

 @AfterEach
 fun tearDown() {
  db.close()
 }

 @Test
 fun testStoreUrlToPageIdMapping() {
  val url = "http://example.com"
  val result = crawlerDAO.storeUrlToPageIdMapping(url)
  assertNotNull(result)
  // Calling the method again should return the previously stored pageId.
  val sameResult = crawlerDAO.storeUrlToPageIdMapping(url)
  assertEquals(result, sameResult)
 }

 @Test
 fun testStorePageKeywords() {
  val pageId = "page1"
  val keywords = listOf("keyword1", "keyword2")
  crawlerDAO.storePageKeywords(pageId, keywords)
  val storedKeywords = forwardIndex[pageId] as? List<String>
  assertEquals(keywords, storedKeywords)
 }

 @Test
 fun testGetPageIdForUrl() {
  val url = "http://example.com"
  val pageId = crawlerDAO.storeUrlToPageIdMapping(url)
  val fetchedPageId = crawlerDAO.getPageIdForUrl(url)
  assertEquals(pageId, fetchedPageId)
 }

 @Test
 fun testStoreParentChildLinks() {
  val parentPageId = "parent1"
  val childPageIds = listOf("child1", "child2")
  crawlerDAO.storeParentChildLinks(parentPageId, childPageIds)
  val storedLinks = parentChildLinks[parentPageId] as? List<String>
  assertEquals(childPageIds, storedLinks)
 }

 @Test
 fun testGetChildPages() {
  val parentPageId = "parent1"
  val childPageIds = listOf("child1", "child2")
  parentChildLinks[parentPageId] = childPageIds
  val result = crawlerDAO.getChildPages(parentPageId)
  assertEquals(childPageIds, result)
 }

 @Test
 fun testStorePageProperties() {
  val pageId = "page1"
  val page = Page("Title", "2023-10-01", "12345", "example.com", "timestamp", 1, listOf("link1", "link2"))
  crawlerDAO.storePageProperties(pageId, page)
  val storedProperties = pageProperties[pageId] as? Map<String, Any>
  assertNotNull(storedProperties)
  assertEquals(page.title, storedProperties?.get("title"))
 }
}