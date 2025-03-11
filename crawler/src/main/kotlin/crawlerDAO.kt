package org.manganesium.crawler

import org.manganesium.DAO.BaseDAO

class crawlerDAO : BaseDAO<String> {
    override fun create(item: String) {
        println("Creating item: $item")
    }
    override fun read(id: String): String? {
        println("Reading item with id: $id")
        return null
    }
    override fun update(item: String) {
        println("Updating item: $item")
    }
    override fun delete(id: String) {
        println("Deleting item with id: $id")
    }
}