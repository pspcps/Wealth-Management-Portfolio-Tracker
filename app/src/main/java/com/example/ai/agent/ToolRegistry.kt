package com.example.ai.agent

import com.example.ai.mcp.McpPermissionManager
import com.example.ai.tools.FinanceTool
import com.example.ai.tools.ToolPermission
import com.example.ai.tools.ToolResult

class ToolRegistry(
    private val mcpPermissionManager: McpPermissionManager
) {
    private val toolsMap = mutableMapOf<String, FinanceTool>()

    fun register(tool: FinanceTool) {
        toolsMap[tool.name] = tool
    }

    fun getTools(): List<FinanceTool> = toolsMap.values.toList()

    fun getTool(name: String): FinanceTool? = toolsMap[name]

    suspend fun execute(toolName: String, arguments: Map<String, Any?>): ToolResult {
        val tool = toolsMap[toolName]
            ?: return ToolResult.error("Tool '$toolName' is not registered.")

        // Enforce sandbox permissions
        if (tool.permission == ToolPermission.WEB_SEARCH) {
            val allowed = mcpPermissionManager.isPermissionAllowed("web_market_research", ToolPermission.WEB_SEARCH)
            if (!allowed) {
                return ToolResult.unavailable("Web search tool permission is currently disabled in your privacy settings.")
            }
        }

        return try {
            tool.execute(arguments)
        } catch (e: Exception) {
            ToolResult.error("Exception during execution of $toolName: ${e.message}")
        }
    }
}
