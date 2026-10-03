package com.example.ui.screens.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ChatMessage
import com.example.ai.ModelInfo
import com.example.ai.agent.FinanceAgent
import com.example.ai.agent.ToolRegistry
import com.example.ai.engine.LocalLlmEngineImpl
import com.example.ai.engine.LocalModelManager
import com.example.ai.mcp.McpPermissionManager
import com.example.ai.mcp.McpServerConfig
import com.example.ai.tools.*
import com.example.ai.web.DuckDuckGoSearchProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.FireSettingsRepository
import com.example.data.repository.PortfolioRepository
import com.example.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WealthAiViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val portfolioRepository = PortfolioRepository(db)
    private val fireSettingsRepository = FireSettingsRepository(application)
    private val userProfileRepository = UserProfileRepository(application)
    private val loanRepository = com.example.data.repository.LoanRepository.getInstance(application)
    private val insuranceRepository = com.example.data.repository.InsuranceRepository.getInstance(application)

    val mcpPermissionManager = McpPermissionManager()
    val modelManager = LocalModelManager(application)
    val webSearchProvider = DuckDuckGoSearchProvider()

    private val toolRegistry = ToolRegistry(mcpPermissionManager).apply {
        // Read Tools
        register(GetNetWorthTool(portfolioRepository))
        register(GetNetWorthHistoryTool(portfolioRepository))
        register(GetPortfolioTool(portfolioRepository))
        register(GetAssetAllocationTool(portfolioRepository))
        register(GetExpensesTool(portfolioRepository))
        register(GetIncomeTool(portfolioRepository))
        register(GetSavingsRateTool(portfolioRepository))
        register(GetCashFlowTool(portfolioRepository))
        register(GetMonthlyComparisonTool(portfolioRepository))
        register(GetFireProjectionTool(portfolioRepository, fireSettingsRepository, userProfileRepository))
        register(GetLoansAndLiabilitiesTool(loanRepository))
        register(GetInsuranceCoverageTool(insuranceRepository))

        // Calculation Tools
        register(CalculateSipTool())
        register(CalculateStepUpSipTool())
        register(CalculateLumpsumTool())
        register(CalculateXirrTool())
        register(CalculateModifiedDietzTool())
        register(CalculateFireCorpusTool())
        register(CalculateRealReturnTool())
        register(CalculateInflationAdjustedValueTool())
        register(CalculateCoastFireTool())

        // Write Tools (Require explicit user confirmation)
        register(AddExpenseTool(portfolioRepository))
        register(AddIncomeTool(portfolioRepository))
        register(AddHoldingTool(portfolioRepository))

        // Web Search Tool
        register(WebSearchTool(webSearchProvider))
    }

    private val llmEngine = LocalLlmEngineImpl(modelManager)

    private val agent = FinanceAgent(
        llmEngine = llmEngine,
        toolRegistry = toolRegistry,
        scope = viewModelScope
    )

    val messages: StateFlow<List<ChatMessage>> = agent.messages
    val isThinking: StateFlow<Boolean> = agent.isThinking
    val selectedModel: StateFlow<ModelInfo> = modelManager.selectedModel
    val installProgress: StateFlow<Float?> = modelManager.installProgress
    val mcpConnections: StateFlow<List<McpServerConfig>> = mcpPermissionManager.connections

    private val _selectedSearchMode = MutableStateFlow(com.example.ai.AiSearchMode.AUTO)
    val selectedSearchMode: StateFlow<com.example.ai.AiSearchMode> = _selectedSearchMode.asStateFlow()

    fun setSearchMode(mode: com.example.ai.AiSearchMode) {
        _selectedSearchMode.value = mode
    }

    fun getAvailableModels(): List<ModelInfo> = modelManager.getAvailableModels()

    fun sendMessage(text: String, mode: com.example.ai.AiSearchMode? = null) {
        val activeMode = mode ?: _selectedSearchMode.value
        agent.sendMessage(text, activeMode)
    }

    fun confirmWrite(messageId: String, toolCallId: String) {
        agent.confirmWriteOperation(messageId, toolCallId)
    }

    fun cancelWrite(messageId: String, toolCallId: String) {
        agent.cancelWriteOperation(messageId, toolCallId)
    }

    fun clearChat() {
        agent.clearChat()
    }

    fun selectModel(modelId: String) {
        modelManager.selectModel(modelId)
    }

    fun installModel(modelId: String) {
        viewModelScope.launch {
            modelManager.installModel(modelId) {}
        }
    }

    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            modelManager.deleteModel(modelId)
        }
    }

    fun toggleMcpConnection(connectionId: String, isConnected: Boolean) {
        mcpPermissionManager.toggleConnection(connectionId, isConnected)
        if (connectionId == "web_market_research") {
            webSearchProvider.isEnabled = isConnected
        }
    }
}
