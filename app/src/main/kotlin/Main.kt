package app

import org.manganesium.crawler.main as runCrawler
import phase1Test.main as runTestProgram

fun main() {
    try {
        println("Starting Crawler...")
        runCrawler()
        println("Crawler completed.")
    } catch (e: Exception) {
        println("Error running crawler: ${e.message}")
        e.printStackTrace()
    }

    try {
        println("Starting Test Program...")
        runTestProgram()
        println("Test Program completed.")
    } catch (e: Exception) {
        println("Error running test program: ${e.message}")
        e.printStackTrace()
    }
}