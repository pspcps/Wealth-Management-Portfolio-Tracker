package com.example.ai.agent

import com.example.ai.ChatMessage
import com.example.ai.ChatRole
import com.example.ai.ToolCall
import com.example.ai.WriteStatus
import com.example.ai.engine.LocalLlmEngine
import com.example.ai.tools.AddExpenseTool
import com.example.ai.tools.AddHoldingTool
import com.example.ai.tools.AddIncomeTool
import com.example.ai.tools.ToolResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FinanceAgent(
    private val llmEngine: LocalLlmEngine,
    private val toolRegistry: ToolRegistry,
    private val scope: CoroutineScope
) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                role = ChatRole.ASSISTANT,
                content = "👋 **Welcome to Wealth AI** — your private, on-device financial copilot.\n\n" +
                        "🔒 **Privacy Guarantee:** All your financial numbers stay 100% on your device with **₹0 cloud API cost**.\n\n" +
                        "You can ask me about your net worth, expenses, savings rate, portfolio holdings, FIRE progress, or run deterministic SIP & compound interest calculations."
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    fun sendMessage(userText: String, searchMode: com.example.ai.AiSearchMode = com.example.ai.AiSearchMode.AUTO) {
        if (userText.isBlank()) return

        val userMessage = ChatMessage(
            role = ChatRole.USER,
            content = userText
        )

        _messages.update { it + userMessage }
        _isThinking.value = true

        scope.launch(Dispatchers.Default) {
            try {
                val currentHistory = _messages.value
                val availableTools = toolRegistry.getTools()

                // Step 1: LLM Engine decides tool calls
                val plannedCalls = llmEngine.decideToolCalls(userText, currentHistory, availableTools, searchMode)

                // Step 2: Execute each tool deterministically
                val executedPairs = mutableListOf<Pair<ToolCall, ToolResult>>()
                for (call in plannedCalls) {
                    val result = toolRegistry.execute(call.toolName, call.arguments)
                    val completedCall = call.copy(result = result.summaryText)
                    executedPairs.add(Pair(completedCall, result))
                }

                // Step 3: LLM Engine synthesizes grounded factual response
                val response = llmEngine.synthesizeResponse(userText, executedPairs, currentHistory, searchMode)

                val assistantMessage = ChatMessage(
                    role = ChatRole.ASSISTANT,
                    content = response.content,
                    toolCalls = executedPairs.map { it.first },
                    sources = response.metadata.sources,
                    metadata = response.metadata
                )

                _messages.update { it + assistantMessage }
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    role = ChatRole.ASSISTANT,
                    content = "An error occurred while processing your request: ${e.message}"
                )
                _messages.update { it + errorMessage }
            } finally {
                _isThinking.value = false
            }
        }
    }

    fun confirmWriteOperation(messageId: String, toolCallId: String) {
        scope.launch(Dispatchers.IO) {
            val message = _messages.value.find { it.id == messageId } ?: return@launch
            val call = message.toolCalls.find { it.id == toolCallId } ?: return@launch

            if (call.writeStatus != WriteStatus.PENDING_CONFIRMATION) return@launch

            var success = false
            var executionMsg = ""

            try {
                when (call.toolName) {
                    "add_expense" -> {
                        val tool = toolRegistry.getTool("add_expense") as? AddExpenseTool
                        if (tool != null) {
                            tool.performCommit(call.arguments)
                            success = true
                            executionMsg = "Expense has been recorded into your local database."
                        }
                    }
                    "add_income" -> {
                        val tool = toolRegistry.getTool("add_income") as? AddIncomeTool
                        if (tool != null) {
                            tool.performCommit(call.arguments)
                            success = true
                            executionMsg = "Income stream has been saved into your local database."
                        }
                    }
                    "add_holding" -> {
                        val tool = toolRegistry.getTool("add_holding") as? AddHoldingTool
                        if (tool != null) {
                            tool.performCommit(call.arguments)
                            success = true
                            executionMsg = "Holding has been added to your portfolio."
                        }
                    }
                }
            } catch (e: Exception) {
                executionMsg = "Failed to commit write operation: ${e.message}"
            }

            _messages.update { list ->
                list.map { msg ->
                    if (msg.id == messageId) {
                        val updatedCalls = msg.toolCalls.map { c ->
                            if (c.id == toolCallId) {
                                c.copy(writeStatus = if (success) WriteStatus.EXECUTED else WriteStatus.FAILED)
                            } else c
                        }
                        msg.copy(
                            toolCalls = updatedCalls,
                            content = msg.content + "\n\n" + (if (success) "✅ **Confirmed & Executed:** $executionMsg" else "❌ **Failed:** $executionMsg")
                        )
                    } else msg
                }
            }
        }
    }

    fun cancelWriteOperation(messageId: String, toolCallId: String) {
        _messages.update { list ->
            list.map { msg ->
                if (msg.id == messageId) {
                    val updatedCalls = msg.toolCalls.map { c ->
                        if (c.id == toolCallId) {
                            c.copy(writeStatus = WriteStatus.CANCELLED)
                        } else c
                    }
                    msg.copy(
                        toolCalls = updatedCalls,
                        content = msg.content + "\n\n🚫 **Cancelled:** Write action was discarded by user."
                    )
                } else msg
            }
        }
    }

    fun clearChat() {
        _messages.value = listOf(
            ChatMessage(
                role = ChatRole.ASSISTANT,
                content = "👋 **Chat cleared.** Ready for your questions!"
            )
        )
    }
}
