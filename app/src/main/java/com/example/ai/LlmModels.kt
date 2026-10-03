package com.example.ai

enum class ChatRole {
    USER,
    ASSISTANT,
    TOOL,
    SYSTEM
}

enum class WriteStatus {
    NONE,
    PENDING_CONFIRMATION,
    CONFIRMED,
    CANCELLED,
    EXECUTED,
    FAILED
}

enum class SourceCategory {
    LOCAL_ROOM_DATA,
    DETERMINISTIC_CALCULATOR,
    WEB_RESEARCH,
    MCP_SERVER
}

enum class AiSearchMode(
    val title: String,
    val icon: String,
    val shortLabel: String,
    val description: String
) {
    AUTO("Smart Hybrid", "⚡", "Auto", "Intelligently routes across on-device vault, math engines, and external research"),
    WEB_SEARCH("Live Web & Markets", "🌐", "Web", "Search real-time market trends, news, historical CAGR benchmarks, and tax policies"),
    LOCAL_VAULT("Local Vault", "🔒", "Vault", "Query your private Net Worth, holdings, expenses, and savings offline"),
    CALCULATORS("Math Calculators", "🧮", "Math", "Deterministic SIP, Step-Up SIP, FIRE, XIRR, and compounding models")
}

data class Source(
    val title: String,
    val url: String? = null,
    val snippet: String = "",
    val category: SourceCategory = SourceCategory.LOCAL_ROOM_DATA
)

data class ToolCall(
    val id: String = java.util.UUID.randomUUID().toString().take(8),
    val toolName: String,
    val arguments: Map<String, Any?> = emptyMap(),
    val result: String? = null,
    val isWriteOperation: Boolean = false,
    val writeStatus: WriteStatus = if (isWriteOperation) WriteStatus.PENDING_CONFIRMATION else WriteStatus.NONE,
    val writePayloadPreview: Map<String, String> = emptyMap()
)

data class AiResponseMetadata(
    val usedLocalData: Boolean = false,
    val usedCalculators: Boolean = false,
    val usedWebSearch: Boolean = false,
    val usedExternalMcp: Boolean = false,
    val toolsUsed: List<String> = emptyList(),
    val sources: List<Source> = emptyList()
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: ChatRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolCalls: List<ToolCall> = emptyList(),
    val sources: List<Source> = emptyList(),
    val metadata: AiResponseMetadata = AiResponseMetadata()
)

data class ModelInfo(
    val id: String,
    val name: String,
    val modelFamily: String,
    val parameterCount: String,
    val quantization: String,
    val memoryRequirementMb: Int,
    val contextLength: Int,
    val downloadUrl: String,
    val localFilePath: String,
    val isInstalled: Boolean,
    val license: String,
    val description: String
)

data class LlmResponse(
    val content: String,
    val toolCalls: List<ToolCall> = emptyList(),
    val isToolCallRequested: Boolean = toolCalls.isNotEmpty(),
    val metadata: AiResponseMetadata = AiResponseMetadata()
)
