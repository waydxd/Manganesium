# AI Module

This module implements the AI service layer for Manganesium v2, providing the foundation for Large Language Model (LLM) integration and conversational AI features.

## Overview

The AI module provides:
- LLM service interface with OpenAI GPT-4 implementation
- Conversation management with persistent storage
- Prompt engineering and context handling
- API endpoints for AI interactions
- Usage tracking and health monitoring

## Architecture

```
ai/
├── src/main/kotlin/org/manganesium/ai/
│   ├── service/          # Core AI services
│   │   ├── LLMService.kt        # LLM service interface
│   │   ├── OpenAIService.kt     # OpenAI implementation  
│   │   ├── PromptService.kt     # Prompt templates and context
│   │   ├── ConversationService.kt # Conversation management
│   │   └── AIService.kt         # Main AI orchestration service
│   ├── model/            # Data models
│   │   └── AIModels.kt          # Conversation, Message, AIResponse, etc.
│   ├── config/           # Configuration classes
│   │   └── AIConfig.kt          # AI service configuration
│   └── client/           # External API clients
│       └── OpenAIClient.kt      # OpenAI HTTP client
└── src/test/kotlin/      # Unit tests
```

## Configuration

The AI service can be configured using environment variables:

```bash
# Required
OPENAI_API_KEY=your_openai_api_key_here

# Optional
OPENAI_BASE_URL=https://api.openai.com/v1
OPENAI_DEFAULT_MODEL=gpt-4
AI_ENABLE_CACHING=true
AI_CACHE_EXPIRATION_MINUTES=60
AI_MAX_CONVERSATION_HISTORY=20
```

## API Endpoints

### AI Ask
```
POST /api/v2/ai/ask
{
  "question": "What is machine learning?",
  "conversationId": "optional-conversation-id", 
  "model": "gpt-4",
  "temperature": 0.7,
  "maxTokens": 1000
}
```

### Conversations
```
# Create conversation
POST /api/v2/conversations
{
  "title": "My Conversation"
}

# Get conversations
GET /api/v2/conversations?limit=50&offset=0

# Get specific conversation
GET /api/v2/conversations/{id}

# Add message to conversation
POST /api/v2/conversations/{id}/messages
{
  "content": "Follow-up question...",
  "model": "gpt-4"
}

# Delete conversation
DELETE /api/v2/conversations/{id}
```

### Health Check
```
GET /api/v2/ai/health
```

## Usage

### Basic AI Query
```kotlin
val aiService = AIService(db, AIConfig.fromEnvironment())

val result = aiService.askQuestion("What is Kotlin?")
result.onSuccess { response ->
    println("Answer: ${response.content}")
    println("Tokens used: ${response.tokensUsed}")
}
```

### Conversation Management
```kotlin
// Create a new conversation
val conversation = aiService.createConversation("Learning Kotlin")

// Add messages to conversation
val response = aiService.addMessageToConversation(
    conversation.id,
    "Tell me about coroutines"
)
```

## Features

### LLM Integration
- **OpenAI GPT-4** integration with full API support
- **Configurable models** (GPT-4, GPT-3.5-turbo)
- **Token usage tracking** and cost estimation
- **Error handling** with retry logic and graceful degradation

### Conversation Management
- **Persistent storage** using MapDB
- **Conversation history** with message threading
- **Context management** for multi-turn conversations
- **Caching** for improved performance

### Prompt Engineering
- **System prompts** for consistent AI behavior
- **Context injection** for conversation continuity
- **Template system** for different query types
- **Source attribution** for factual responses

### API Integration
- **RESTful endpoints** following existing patterns
- **JSON serialization** with proper error handling
- **Health monitoring** and service status
- **Backward compatibility** with existing v1 search

## Testing

```bash
# Run AI module tests
gradle :ai:test

# Test specific functionality
gradle :ai:test --tests "AIServiceTest"
```

## Development Notes

### Adding New LLM Providers
To add support for additional LLM providers:

1. Implement the `LLMService` interface
2. Create a new client in the `client` package
3. Add configuration options in `AIConfig`
4. Update the `AIService` to support provider selection

### Database Schema
The AI module extends the existing MapDB structure with:
- `ai_conversations` - Conversation metadata
- `ai_messages` - Individual messages
- `ai_conversation_messages` - Message-to-conversation mappings

### Error Handling
- **API failures** return Result types with detailed error information
- **Missing configuration** gracefully degrades service availability
- **Rate limiting** and cost management prevent runaway usage
- **Logging** provides comprehensive debugging information

## Future Enhancements

- RAG (Retrieval Augmented Generation) pipeline integration
- Real-time web search capabilities  
- Streaming response support
- Vector embeddings for semantic search
- Multiple conversation contexts
- Plugin system for custom AI behaviors

This module provides the foundation for transforming Manganesium into a Perplexity AI-style conversational search engine while maintaining full backward compatibility with existing functionality.