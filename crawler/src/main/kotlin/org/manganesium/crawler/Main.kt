package org.manganesium.crawler

import dataAccessObject.CrawlerDAO
import org.manganesium.crawler.services.CrawlerService

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    val name = "Kotlin"
    //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
    // to see how IntelliJ IDEA suggests fixing it.
    println("Hello, " + name + "!")

    for (i in 1..5) {
        //TIP Press <shortcut actionId="Debug"/> to start debugging your code. We have set one <icon src="AllIcons.Debugger.Db_set_breakpoint"/> breakpoint
        // for you, but you can always add more by pressing <shortcut actionId="ToggleLineBreakpoint"/>.
        println("i = $i")
    }


    // Initialize DAO with a path to a DB file (e.g. "crawler.db")
    val crawlerDao = CrawlerDAO("crawler.db")

    // Create the crawler service
    val crawlerService = CrawlerService(crawlerDao)

    // Define your starting URLs
    val startUrls = listOf(
        "https://comp4321-hkust.github.io/testpages/testpage.htm"
    )

    // Start simple BFS crawling up to 2 levels deep
    crawlerService.startCrawling(startUrls, maxDepth = 2)

    // Or demonstrate concurrency
    // crawlerService.startCrawlingConcurrently(startUrls, maxThreads = 4, maxDepth = 2)

    // Close the DAO to commit and release resources
    crawlerService.close()
}