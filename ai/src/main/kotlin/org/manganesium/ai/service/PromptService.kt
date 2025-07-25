package org.manganesium.ai.service

import org.manganesium.ai.model.Message
import org.manganesium.ai.model.MessageRole

/**
 * Service for managing prompt templates and context
 */
class PromptService {
    
    /**
     * Create a system prompt for the AI assistant
     */
    fun createSystemPrompt(): String {
        return """
            You are Manganesium, an intelligent search assistant that helps users find and understand information.
            
            Your primary capabilities:
            - Provide comprehensive, well-structured answers based on available sources
            - Cite sources appropriately using markdown links
            - Ask clarifying questions when needed
            - Maintain conversation context across multiple exchanges
            
            Guidelines:
            - Always be helpful, accurate, and concise
            - When referencing sources, use the format: [Source Title](URL)
            - If you're uncertain about information, clearly state your uncertainty
            - Focus on providing actionable insights when possible
            - Maintain a professional yet friendly tone
        """.trimIndent()
    }
    
    /**
     * Create a search query prompt based on user input
     */
    fun createSearchPrompt(userQuery: String, context: List<Message> = emptyList()): String {
        val contextString = if (context.isNotEmpty()) {
            val relevantContext = context.takeLast(5) // Use last 5 messages for context
                .filter { it.role != MessageRole.SYSTEM }
                .joinToString("\n") { "${it.role.name}: ${it.content}" }
            
            if (relevantContext.isNotBlank()) {
                "\n\nConversation Context:\n$relevantContext\n\n"
            } else ""
        } else ""
        
        return """
            ${contextString}User Query: $userQuery
            
            Please provide a comprehensive answer based on the available information. 
            If you reference any sources, make sure to cite them properly.
            
            Response:
        """.trimIndent()
    }
    
    /**
     * Create a summary prompt for long content
     */
    fun createSummaryPrompt(content: String, maxLength: Int = 200): String {
        return """
            Please provide a concise summary of the following content in approximately $maxLength words or less:
            
            Content: $content
            
            Summary:
        """.trimIndent()
    }
    
    /**
     * Create a prompt for extracting key information from sources
     */
    fun createExtractionPrompt(query: String, sources: List<String>): String {
        val sourcesText = sources.mapIndexed { index, source ->
            "Source ${index + 1}:\n$source\n"
        }.joinToString("\n")
        
        return """
            Based on the following sources, please answer this query: "$query"
            
            $sourcesText
            
            Please provide a comprehensive answer that synthesizes information from the sources above.
            Cite the sources by number (e.g., "According to Source 1...").
            
            Answer:
        """.trimIndent()
    }
    
    /**
     * Prepare conversation context for LLM
     */
    fun prepareConversationContext(
        messages: List<Message>, 
        maxMessages: Int = 10
    ): List<Message> {
        // Take the system message (if any) plus the last N user/assistant messages
        val systemMessages = messages.filter { it.role == MessageRole.SYSTEM }
        val conversationMessages = messages.filter { it.role != MessageRole.SYSTEM }
            .takeLast(maxMessages)
        
        return systemMessages + conversationMessages
    }
}