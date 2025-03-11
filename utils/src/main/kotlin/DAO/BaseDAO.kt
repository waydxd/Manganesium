package org.manganesium.DAO
/**
 * BaseDAO interface
 */
interface BaseDAO<T> {
    fun create(item: T)
    fun read(id: String): T?
    fun update(item: T)
    fun delete(id: String)
}