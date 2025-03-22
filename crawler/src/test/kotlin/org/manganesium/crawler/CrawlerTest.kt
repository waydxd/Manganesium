package org.manganesium.crawler

import dataAccessObject.CrawlerDAO
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.manganesium.indexer.Indexer
import org.mockito.kotlin.*
import java.io.IOException

// For static mocking of Jsoup
import org.mockito.Mockito.mockStatic
import org.mockito.MockedStatic

// Alias for clarity.
import org.mockito.kotlin.spy as kotlinSpy

open class CrawlerTest {

    private lateinit var crawlerDAO: CrawlerDAO
    private lateinit var crawler: Crawler
    private lateinit var indexer: Indexer

    @BeforeEach
    fun setUp() {
        crawlerDAO = mock()
        // Now that Crawler is open, we can spy on it.
        crawler = kotlinSpy(Crawler(crawlerDAO))
        indexer = mock()
    }

    @Test
    fun `test extractLinks returns expected links`() {
        val html = """
            <html>
              <body>
                <a href="https://example.com/page1">Page 1</a>
                <a href="https://example.com/page2">Page 2</a>
              </body>
            </html>
        """.trimIndent()
        // Parse with base URI so "abs:href" resolves.
        val doc: Document = Jsoup.parse(html, "https://example.com")
        val links = crawler.extractLinks(doc)

        assertEquals(2, links.size, "Should extract two links")
        assertTrue(links.contains("https://example.com/page1"))
        assertTrue(links.contains("https://example.com/page2"))
    }

    @Test
    fun `test close calls dao close`() {
        crawler.close()
        verify(crawlerDAO).close()
    }

    @Test
    fun `test startCrawling processes pages successfully`() {
        val dummyHtml = """
            <html>
              <head><title>Test Page</title></head>
              <body>
                <p>Some sample text.</p>
              </body>
            </html>
        """.trimIndent()
        val dummyDoc: Document = Jsoup.parse(dummyHtml, "http://example.com")

        // For a successful crawl, stub fetchPage so that any URL returns dummyDoc.
        doReturn(dummyDoc).`when`(crawler).fetchPage(any())
        // Stub DAO mapping method to return a dummy page ID.
        whenever(crawlerDAO.storeUrlToPageIdMapping(any())).thenReturn("1")

        val startUrls = listOf("http://example.com")
        crawler.startCrawling(startUrls, maxDepth = 1, maxPages = 1, indexer)

        // Verify that the starting URL was processed.
        assertTrue(crawler.visitedUrls.contains("http://example.com"))
        verify(crawlerDAO).storeUrlToPageIdMapping("http://example.com")
    }

    /*
    @Test
    fun `test startCrawling with fetch failure does not process page when fetchPage returns null`() {
        val failUrl = "http://fail.com"
        // Given that TestCrawler returns null for "http://fail.com",
        // calling startCrawling should not trigger any DAO or indexer interactions.
        crawler.startCrawling(listOf(failUrl), maxDepth = 1, maxPages = 1, indexer)

        // Verify that since fetching fails, DAO and indexer methods are not called.
        verify(crawlerDAO, never()).storeUrlToPageIdMapping(org.mockito.kotlin.any())
        verify(indexer, never()).indexPage(org.mockito.kotlin.any())
        // The URL should still be recorded in the visited set.
        assertTrue(crawler.visitedUrls.contains(failUrl))
    }
    */

    @Test
    fun `test fetchPage handles IOException gracefully`() {
        val url = "http://ioerror.com"
        // Use static mocking for Jsoup so that calling get() throws an IOException.
        mockStatic(Jsoup::class.java).use { mockedJsoup: MockedStatic<Jsoup> ->
            val connection = mock<org.jsoup.Connection>()
            whenever(Jsoup.connect(url)).thenReturn(connection)
            whenever(connection.get()).thenThrow(IOException("Test IO Error"))

            val result: Document? = crawler.fetchPage(url)
            assertNull(result, "fetchPage should return null on IOException")
        }
    }
}
