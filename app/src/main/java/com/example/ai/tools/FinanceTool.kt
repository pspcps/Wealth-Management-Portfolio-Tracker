package com.example.ai.tools

import com.example.ai.Source

enum class ToolPermission {
    READ_FINANCIAL_DATA,
    CALCULATE,
    WEB_SEARCH,
    WRITE_FINANCIAL_DATA
}

data class ToolResult(
    val success: Boolean,
    val data: Map<String, Any?> = emptyMap(),
    val summaryText: String = "",
    val error: String? = null,
    val source: Source? = null
) {
    companion object {
        fun success(
            data: Map<String, Any?> = emptyMap(),
            summaryText: String = "",
            summary: String = summaryText,
            source: Source? = null
        ): ToolResult {
            val text = if (summaryText.isNotEmpty()) summaryText else summary
            return ToolResult(
                success = true,
                data = data,
                summaryText = text,
                source = source
            )
        }

        fun unavailable(reason: String): ToolResult {
            return ToolResult(
                success = false,
                data = emptyMap(),
                summaryText = reason,
                error = reason
            )
        }

        fun error(message: String): ToolResult {
            return ToolResult(
                success = false,
                data = emptyMap(),
                summaryText = "Failed to execute: $message",
                error = message
            )
        }
    }
}

interface FinanceTool {
    val name: String
    val description: String
    val permission: ToolPermission
    val isWriteOperation: Boolean get() = false
    val parameterSchema: Map<String, String> get() = emptyMap()

    suspend fun execute(arguments: Map<String, Any?>): ToolResult
}
