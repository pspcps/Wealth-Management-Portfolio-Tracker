package com.example.ai.tools

import com.example.ai.Source
import com.example.ai.SourceCategory
import com.example.ai.web.SearchPrivacyFilter
import com.example.ai.web.WebSearchProvider

class WebSearchTool(
    private val searchProvider: WebSearchProvider
) : FinanceTool {
    override val name: String = "web_search"
    override val description: String = "Performs external web market research for public benchmark queries. Strictly filters out personal financial data."
    override val permission: ToolPermission = ToolPermission.WEB_SEARCH
    override val parameterSchema: Map<String, String> = mapOf(
        "query" to "Public financial or market research query (e.g. 'Nifty 50 historical CAGR', 'EPF interest rate', 'PPF rules')"
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        if (!searchProvider.isEnabled) {
            return ToolResult.unavailable("Web search research is currently disabled in your privacy settings.")
        }

        val rawQuery = arguments["query"]?.toString()?.trim() ?: ""
        if (rawQuery.isBlank()) {
            return ToolResult.unavailable("Search query is empty.")
        }

        val sanitized = SearchPrivacyFilter.sanitizeQuery(rawQuery)

        val results = searchProvider.search(sanitized)
        if (results.isEmpty()) {
            return ToolResult.unavailable("No public market information found for query '$sanitized'.")
        }

        val first = results.first()
        val formattedSummary = results.joinToString("\n\n") { res ->
            "• **${res.title}** (${res.url}):\n${res.snippet}"
        }

        val data = mapOf(
            "query" to sanitized,
            "resultsCount" to results.size,
            "results" to results.map { mapOf("title" to it.title, "url" to it.url, "snippet" to it.snippet) }
        )

        return ToolResult.success(
            data = data,
            summaryText = "Web Research for '$sanitized':\n$formattedSummary",
            source = Source(
                title = first.title,
                url = first.url,
                snippet = first.snippet,
                category = SourceCategory.WEB_RESEARCH
            )
        )
    }
}
