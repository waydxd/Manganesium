package org.manganesium.crawler

import dataAccessObject.CrawlerDAO
import org.manganesium.crawler.services.CrawlerService
import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    /*
    val name = "Kotlin"
    //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
    // to see how IntelliJ IDEA suggests fixing it.
    println("Hello, " + name + "!")

    for (i in 1..5) {
        //TIP Press <shortcut actionId="Debug"/> to start debugging your code. We have set one <icon src="AllIcons.Debugger.Db_set_breakpoint"/> breakpoint
        // for you, but you can always add more by pressing <shortcut actionId="ToggleLineBreakpoint"/>.
        println("i = $i")
    }
    */


    // Configure logger to show FINE level messages
    val rootLogger = Logger.getLogger("")

    // Clear existing handlers
    for (handler in rootLogger.handlers) {
        rootLogger.removeHandler(handler)
    }

    // Add console handler with FINE level
    val consoleHandler = ConsoleHandler()
    consoleHandler.level = Level.FINE
    rootLogger.addHandler(consoleHandler)
    rootLogger.level = Level.FINE

    // Also set the logger level for your specific packages
    Logger.getLogger("org.manganesium").level = Level.FINE
    Logger.getLogger("dataAccessObject").level = Level.FINE

    // Initialize DAO with a path to a DB file
    val crawlerDao = CrawlerDAO("crawler.db")

    // Create the crawler service
    val crawlerService = CrawlerService(crawlerDao)

    // Define your starting URLs
    val startUrls = listOf(
        "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm"
    )

    // Start crawling
    crawlerService.startCrawling(startUrls, maxDepth = 2)

    // Close the DAO to commit and release resources
    crawlerService.close()

    println("Crawling summary:")
    println("Total pages visited: ${crawlerService.visitedUrls.size}")
}