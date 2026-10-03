package com.example.ai.mcp

import com.example.ai.tools.FinanceTool
import com.example.ai.tools.ToolPermission
import com.example.ai.tools.ToolResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

enum class McpConnectionType {
    LOCAL_SANDBOX,
    BUILTIN_WEB,
    EXTERNAL_SERVER
}

data class McpServerConfig(
    val id: String,
    val name: String,
    val type: McpConnectionType,
    val endpointUrl: String = "",
    val isConnected: Boolean = true,
    val allowedPermissions: Set<ToolPermission>,
    val isTrusted: Boolean = false,
    val description: String = ""
)

/**
 * Model Context Protocol (MCP) Manager for Wealth Tracker.
 * Implements strict boundary enforcement: External MCP servers never receive private financial data.
 */
class McpPermissionManager {

    private val _connections = MutableStateFlow(
        listOf(
            McpServerConfig(
                id = "local_sandbox",
                name = "Wealth Tracker (Local Sandbox)",
                type = McpConnectionType.LOCAL_SANDBOX,
                allowedPermissions = setOf(
                    ToolPermission.READ_FINANCIAL_DATA,
                    ToolPermission.CALCULATE,
                    ToolPermission.WRITE_FINANCIAL_DATA
                ),
                isConnected = true,
                isTrusted = true,
                description = "On-device Room database & mathematical calculations engine. Zero external network transfer."
            ),
            McpServerConfig(
                id = "web_market_research",
                name = "Web Market Research (DuckDuckGo)",
                type = McpConnectionType.BUILTIN_WEB,
                allowedPermissions = setOf(ToolPermission.WEB_SEARCH),
                isConnected = true,
                isTrusted = true,
                description = "Optional anonymous public market benchmarking. Personal numbers are automatically stripped before queries."
            ),
            McpServerConfig(
                id = "external_market_mcp",
                name = "External Market Data MCP",
                type = McpConnectionType.EXTERNAL_SERVER,
                allowedPermissions = setOf(ToolPermission.CALCULATE), // External server strictly blocked from personal financial data
                isConnected = false,
                isTrusted = false,
                description = "External MCP endpoint. Financial data permissions are permanently blocked for privacy."
            )
        )
    )
    val connections: StateFlow<List<McpServerConfig>> = _connections.asStateFlow()

    fun isPermissionAllowed(connectionId: String, permission: ToolPermission): Boolean {
        val server = _connections.value.find { it.id == connectionId } ?: return false
        if (!server.isConnected) return false
        return server.allowedPermissions.contains(permission)
    }

    fun toggleConnection(connectionId: String, isConnected: Boolean) {
        _connections.value = _connections.value.map {
            if (it.id == connectionId) it.copy(isConnected = isConnected) else it
        }
    }
}

/**
 * Adapts FinanceTool into standard MCP JSON-RPC protocol format.
 */
object McpToolAdapter {

    fun toMcpToolDefinition(tool: FinanceTool): JSONObject {
        val json = JSONObject()
        json.put("name", tool.name)
        json.put("description", tool.description)

        val inputSchema = JSONObject()
        inputSchema.put("type", "object")

        val properties = JSONObject()
        tool.parameterSchema.forEach { (paramName, paramDesc) ->
            val prop = JSONObject()
            prop.put("type", "string")
            prop.put("description", paramDesc)
            properties.put(paramName, prop)
        }
        inputSchema.put("properties", properties)
        json.put("inputSchema", inputSchema)

        return json
    }

    fun formatMcpResponse(id: String, result: ToolResult): JSONObject {
        val resp = JSONObject()
        resp.put("jsonrpc", "2.0")
        resp.put("id", id)

        val resultJson = JSONObject()
        resultJson.put("success", result.success)
        resultJson.put("summary", result.summaryText)

        val dataJson = JSONObject(result.data)
        resultJson.put("data", dataJson)

        if (result.error != null) {
            resultJson.put("error", result.error)
        }

        resp.put("result", resultJson)
        return resp
    }
}
