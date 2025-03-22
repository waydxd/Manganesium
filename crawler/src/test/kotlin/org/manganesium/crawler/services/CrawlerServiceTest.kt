package org.manganesium.crawler.services

import dataAccessObject.CrawlerDAO
import models.Page
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.manganesium.crawler.Crawler
import org.manganesium.indexer.Indexer
import org.mockito.kotlin.*
import java.time.Instant

// Alias Mockito Kotlin spy to avoid ambiguity
import org.mockito.kotlin.spy as kotlinSpy

class CrawlerServiceTest {

    private lateinit var crawlerDAO: CrawlerDAO
    private lateinit var crawler: Crawler
    private lateinit var crawlerService: CrawlerService
    private lateinit var indexer: Indexer

    @BeforeEach
    fun setUp() {
        crawlerDAO = mock()
        // Create a spy of the Crawler to intercept fetchPage calls.
        crawler = kotlinSpy(Crawler(crawlerDAO))
        crawlerService = CrawlerService(crawlerDAO, crawler)
        indexer = mock()
    }

    @Test
    fun `test crawlSinglePage succeeds with valid document`() {
        val parentUrl = "http://parent.com"
        val childUrl = "http://child.com"
        val html = """
            <html>
              <head><title>Parent Title</title></head>
              <body>
                <a href="$childUrl">Child Link</a>
                <p>Some content here</p>
              </body>
            </html>
        """.trimIndent()

        // Create a dummy document with one hyperlink.
        val document: Document = Jsoup.parse(html, parentUrl)

        // Stub Crawler.fetchPage so that it returns the document for the parent URL.
        doReturn(document).`when`(crawler).fetchPage(parentUrl)

        // Stub DAO calls: parent returns page ID "1" and child returns "2".
        whenever(crawlerDAO.storeUrlToPageIdMapping(parentUrl)).thenReturn("1")
        whenever(crawlerDAO.storeUrlToPageIdMapping(childUrl)).thenReturn("2")

        // Call crawlSinglePage.
        crawlerService.crawlSinglePage(parentUrl, indexer)

        // Verify that the parent and child URL mappings were stored.
        verify(crawlerDAO).storeUrlToPageIdMapping(parentUrl)
        verify(crawlerDAO).storeUrlToPageIdMapping(childUrl)

        // Verify that the parent-child relationship is stored properly.
        verify(crawlerDAO).storeParentChildLinks("1", listOf("2"))

        // Capture the Page object that was indexed.
        val pageCaptor = argumentCaptor<Page>()
        verify(indexer).indexPage(pageCaptor.capture())
        val capturedPage = pageCaptor.firstValue

        assertEquals("1", capturedPage.id)
        assertEquals(parentUrl, capturedPage.url)
        assertEquals("Parent Title", capturedPage.title)
        assertEquals(listOf(childUrl), capturedPage.links)

        // Confirm the child's URL is enqueued for further crawling.
        assertTrue(crawler.urlQueue.contains(childUrl))
    }

    @Test
    fun `test crawlSinglePage handles fetch failure gracefully`() {
        val failUrl = "http://fail.com"

        // Simulate fetch failure by having fetchPage return null.
        doReturn(null).`when`(crawler).fetchPage(failUrl)

        // Invoke crawlSinglePage.
        crawlerService.crawlSinglePage(failUrl, indexer)

        // Verify that in case of failure, the DAO's store method is not called.
        verify(crawlerDAO, never()).storeUrlToPageIdMapping(any())
        // Also, indexer.indexPage should never be invoked.
        verify(indexer, never()).indexPage(any())
    }
}
