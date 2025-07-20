package org.manganesium.ai.service

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.manganesium.ai.config.AIConfig
import org.manganesium.ai.config.LLMConfig
import org.mapdb.DBMaker
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AIServiceTest {

    @Test
    fun `test AI service initialization`() {
        // Create in-memory database for testing
        val testDB = DBMaker.memoryDB().make()
        
        // Create AI service with empty config (should handle gracefully)
        val aiService = AIService(testDB, AIConfig())
        
        // Test that service can be created
        assertNotNull(aiService)
        
        // Test conversation creation (should work without API key)
        val conversation = aiService.createConversation("Test Conversation")
        assertNotNull(conversation)
        assertTrue(conversation.title == "Test Conversation")
        
        // Test getting conversations
        val conversations = aiService.getConversations()
        assertTrue(conversations.isNotEmpty())
        assertTrue(conversations.first().id == conversation.id)
        
        // Clean up
        aiService.close()
        testDB.close()
    }
    
    @Test
    fun `test conversation service basic operations`() {
        val testDB = DBMaker.memoryDB().make()
        val conversationService = ConversationService(testDB)
        
        // Test creating conversation
        val conversation = conversationService.createConversation("Test")
        assertNotNull(conversation)
        
        // Test retrieving conversation
        val retrieved = conversationService.getConversation(conversation.id)
        assertNotNull(retrieved)
        assertTrue(retrieved.title == "Test")
        
        // Clean up
        conversationService.close()
        testDB.close()
    }
    
    @Test
    fun `test AI service health check without API key`() = runBlocking {
        val testDB = DBMaker.memoryDB().make()
        val aiService = AIService(testDB, AIConfig())
        
        // Should return false when no API key is configured
        val isHealthy = aiService.isHealthy()
        assertTrue(!isHealthy) // Should be false without API key
        
        // Service info should indicate not configured
        val serviceInfo = aiService.getServiceInfo()
        assertTrue(serviceInfo["configured"] == false)
        
        aiService.close()
        testDB.close()
    }
}