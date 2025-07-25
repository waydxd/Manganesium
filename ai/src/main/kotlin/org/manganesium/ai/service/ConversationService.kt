package org.manganesium.ai.service

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.manganesium.ai.model.Conversation
import org.manganesium.ai.model.Message
import org.mapdb.DB
import org.mapdb.HTreeMap
import org.mapdb.Serializer
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for managing conversations and messages using MapDB
 */
class ConversationService(private val db: DB) {
    
    private val logger = KotlinLogging.logger {}
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    // MapDB collections for AI data
    private val conversations: HTreeMap<String, String> = db
        .hashMap("ai_conversations")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.STRING)
        .createOrOpen()
    
    private val messages: HTreeMap<String, String> = db
        .hashMap("ai_messages")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.STRING)
        .createOrOpen()
    
    private val conversationMessages: HTreeMap<String, String> = db
        .hashMap("ai_conversation_messages")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.STRING)
        .createOrOpen()
    
    // In-memory cache for better performance
    private val conversationCache = ConcurrentHashMap<String, Conversation>()
    private val cacheExpirationTime = 60 * 60 * 1000L // 1 hour in milliseconds
    private val cacheTimestamps = ConcurrentHashMap<String, Long>()
    
    /**
     * Create a new conversation
     */
    fun createConversation(title: String = "New Conversation"): Conversation {
        val conversationId = generateId()
        val conversation = Conversation(
            id = conversationId,
            title = title
        )
        
        return saveConversation(conversation)
    }
    
    /**
     * Get conversation by ID
     */
    fun getConversation(conversationId: String): Conversation? {
        // Check cache first
        val cached = getCachedConversation(conversationId)
        if (cached != null) {
            return cached
        }
        
        return try {
            val conversationJson = conversations[conversationId] ?: return null
            val conversation = json.decodeFromString<Conversation>(conversationJson)
            
            // Load messages for this conversation
            val messageIds = getMessageIds(conversationId)
            val conversationMessages = messageIds.mapNotNull { messageId ->
                getMessageById(messageId)
            }.sortedBy { it.timestamp }
            
            val fullConversation = conversation.copy(messages = conversationMessages)
            
            // Cache the result
            cacheConversation(fullConversation)
            
            fullConversation
        } catch (e: Exception) {
            logger.error(e) { "Error loading conversation $conversationId" }
            null
        }
    }
    
    /**
     * Save or update a conversation
     */
    fun saveConversation(conversation: Conversation): Conversation {
        return try {
            val conversationJson = json.encodeToString(conversation)
            conversations[conversation.id] = conversationJson
            
            // Update cache
            cacheConversation(conversation)
            
            // Commit changes
            db.commit()
            
            conversation
        } catch (e: Exception) {
            logger.error(e) { "Error saving conversation ${conversation.id}" }
            conversation
        }
    }
    
    /**
     * Add a message to a conversation
     */
    fun addMessage(conversationId: String, message: Message): Message {
        return try {
            val messageWithConversationId = message.copy(conversationId = conversationId)
            val messageJson = json.encodeToString(messageWithConversationId)
            
            // Save message
            messages[message.id] = messageJson
            
            // Add message ID to conversation's message list
            addMessageToConversation(conversationId, message.id)
            
            // Update conversation's updatedAt timestamp
            val conversation = getConversation(conversationId)
            if (conversation != null) {
                val updatedConversation = conversation.copy(
                    updatedAt = System.currentTimeMillis()
                )
                saveConversation(updatedConversation)
            }
            
            // Clear cache for this conversation to force reload
            invalidateConversationCache(conversationId)
            
            // Commit changes
            db.commit()
            
            messageWithConversationId
        } catch (e: Exception) {
            logger.error(e) { "Error adding message to conversation $conversationId" }
            message
        }
    }
    
    /**
     * Get all conversations (limited to avoid memory issues)
     */
    fun getConversations(limit: Int = 50, offset: Int = 0): List<Conversation> {
        return try {
            conversations.keys
                .drop(offset)
                .take(limit)
                .mapNotNull { conversationId: String ->
                    getConversation(conversationId)
                }
                .sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            logger.error(e) { "Error loading conversations" }
            emptyList()
        }
    }
    
    /**
     * Delete a conversation and its messages
     */
    fun deleteConversation(conversationId: String): Boolean {
        return try {
            // Delete all messages in this conversation
            val messageIds = getMessageIds(conversationId)
            messageIds.forEach { messageId ->
                messages.remove(messageId)
            }
            
            // Delete conversation message list
            conversationMessages.remove(conversationId)
            
            // Delete conversation
            conversations.remove(conversationId)
            
            // Clear cache
            invalidateConversationCache(conversationId)
            
            // Commit changes
            db.commit()
            
            true
        } catch (e: Exception) {
            logger.error(e) { "Error deleting conversation $conversationId" }
            false
        }
    }
    
    private fun getMessageById(messageId: String): Message? {
        return try {
            val messageJson = messages[messageId] ?: return null
            json.decodeFromString<Message>(messageJson)
        } catch (e: Exception) {
            logger.error(e) { "Error loading message $messageId" }
            null
        }
    }
    
    private fun getMessageIds(conversationId: String): List<String> {
        return try {
            val messageIdsJson = conversationMessages[conversationId] ?: return emptyList()
            json.decodeFromString<List<String>>(messageIdsJson)
        } catch (e: Exception) {
            logger.error(e) { "Error loading message IDs for conversation $conversationId" }
            emptyList()
        }
    }
    
    private fun addMessageToConversation(conversationId: String, messageId: String) {
        val currentMessageIds = getMessageIds(conversationId).toMutableList()
        currentMessageIds.add(messageId)
        val messageIdsJson = json.encodeToString(currentMessageIds)
        conversationMessages[conversationId] = messageIdsJson
    }
    
    private fun getCachedConversation(conversationId: String): Conversation? {
        val cached = conversationCache[conversationId]
        val timestamp = cacheTimestamps[conversationId]
        
        return if (cached != null && timestamp != null && 
                   System.currentTimeMillis() - timestamp < cacheExpirationTime) {
            cached
        } else {
            // Cache expired or not found
            conversationCache.remove(conversationId)
            cacheTimestamps.remove(conversationId)
            null
        }
    }
    
    private fun cacheConversation(conversation: Conversation) {
        conversationCache[conversation.id] = conversation
        cacheTimestamps[conversation.id] = System.currentTimeMillis()
    }
    
    private fun invalidateConversationCache(conversationId: String) {
        conversationCache.remove(conversationId)
        cacheTimestamps.remove(conversationId)
    }
    
    private fun generateId(): String {
        return UUID.randomUUID().toString()
    }
    
    /**
     * Close the service and cleanup resources
     */
    fun close() {
        conversationCache.clear()
        cacheTimestamps.clear()
    }
}