package com.example.ui.screens.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.AiResponseMetadata
import com.example.ai.AiSearchMode
import com.example.ai.ChatMessage
import com.example.ai.ChatRole
import com.example.ai.ModelInfo
import com.example.ai.Source
import com.example.ai.SourceCategory
import com.example.ai.ToolCall
import com.example.ai.WriteStatus
import com.example.ai.mcp.McpConnectionType
import com.example.ai.mcp.McpServerConfig
import com.example.ai.tools.ToolPermission
import com.example.ui.components.GlobalPrivacyEyeButton
import com.example.ui.components.MarkdownFormattedText
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WealthAiScreen(
    viewModel: WealthAiViewModel,
    onNavigateBack: () -> Unit
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isThinking by viewModel.isThinking.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val installProgress by viewModel.installProgress.collectAsStateWithLifecycle()
    val mcpConnections by viewModel.mcpConnections.collectAsStateWithLifecycle()
    val selectedSearchMode by viewModel.selectedSearchMode.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Chat, 1: Models, 2: MCP & Privacy
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll when new messages arrive
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = HighDensityBg,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            Surface(
                color = HighDensityBg,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.testTag("btn_ai_back")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = HighDensityPrimary)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Wealth AI Copilot",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                                        color = HighDensityPrimary
                                    )
                                    val isCloud = selectedModel.id.contains("gemini")
                                    Surface(
                                        color = if (isCloud) Color(0xFFEDE9FE) else Color(0xFFDCFCE7),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (isCloud) "✨ FRONTIER AI" else "🔒 LOCAL",
                                            color = if (isCloud) Color(0xFF6D28D9) else Color(0xFF166534),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${selectedModel.name.take(24)}... • Deterministic Math",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            GlobalPrivacyEyeButton(
                                size = 36.dp,
                                iconSize = 18.dp,
                                testTag = "btn_ai_privacy_eye"
                            )
                            IconButton(
                                onClick = { viewModel.clearChat() },
                                modifier = Modifier.testTag("btn_ai_clear_chat")
                            ) {
                                Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear Chat", tint = Color(0xFF64748B))
                            }
                        }
                    }

                    // Navigation Tabs
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = HighDensityBg,
                        contentColor = HighDensityPrimary,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Copilot Chat", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("tab_ai_chat")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("AI Models", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(Icons.Outlined.Psychology, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("tab_ai_models")
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("MCP & Privacy", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(Icons.Outlined.Shield, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("tab_ai_mcp")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> AiChatView(
                    messages = messages,
                    isThinking = isThinking,
                    selectedSearchMode = selectedSearchMode,
                    onSelectSearchMode = { viewModel.setSearchMode(it) },
                    inputText = inputText,
                    onInputTextChange = { inputText = it },
                    onSendMessage = {
                        viewModel.sendMessage(it)
                        inputText = ""
                    },
                    onConfirmWrite = { msgId, callId -> viewModel.confirmWrite(msgId, callId) },
                    onCancelWrite = { msgId, callId -> viewModel.cancelWrite(msgId, callId) },
                    listState = listState
                )
                1 -> LocalModelsView(
                    selectedModel = selectedModel,
                    availableModels = viewModel.getAvailableModels(),
                    installProgress = installProgress,
                    onSelectModel = { viewModel.selectModel(it) },
                    onInstallModel = { viewModel.installModel(it) },
                    onDeleteModel = { viewModel.deleteModel(it) }
                )
                2 -> McpPrivacyView(
                    connections = mcpConnections,
                    onToggleConnection = { id, enabled -> viewModel.toggleMcpConnection(id, enabled) }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 1. Chat Tab View
// -------------------------------------------------------------
@Composable
fun AiChatView(
    messages: List<ChatMessage>,
    isThinking: Boolean,
    selectedSearchMode: AiSearchMode,
    onSelectSearchMode: (AiSearchMode) -> Unit,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onConfirmWrite: (String, String) -> Unit,
    onCancelWrite: (String, String) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    val quickPrompts = when (selectedSearchMode) {
        AiSearchMode.WEB_SEARCH -> listOf(
            "🌐 Nifty 50 & Sensex historical CAGRs",
            "🌐 S&P 500 US equity returns benchmark",
            "🌐 Current EPFO & PPF interest rates",
            "🌐 Indian LTCG & STCG tax slabs",
            "🌐 RBI Sovereign Gold Bond rules",
            "🌐 Index funds vs active mutual funds"
        )
        AiSearchMode.LOCAL_VAULT -> listOf(
            "🔒 What is my current Net Worth?",
            "🔒 How much did I spend this month?",
            "🔒 What is my savings rate?",
            "🔒 Show my portfolio asset allocation",
            "🔒 Am I on track for FIRE retirement?",
            "➕ Add ₹850 dinner expense"
        )
        AiSearchMode.CALCULATORS -> listOf(
            "🧮 Calculate SIP for ₹25,000 at 12% for 15 yrs",
            "🧮 Calculate Step-Up SIP with 10% annual hike",
            "🧮 Calculate Lumpsum ₹5 Lakhs for 10 yrs at 12%",
            "🧮 Calculate FIRE Retirement Corpus",
            "🧮 Calculate Real Return with 6% inflation"
        )
        AiSearchMode.AUTO -> listOf(
            "📊 What is my current Net Worth?",
            "🌐 Benchmark Nifty 50 historical CAGR",
            "💳 How much did I spend this month?",
            "🧮 Calculate SIP for ₹25,000 at 12% for 15 yrs",
            "🔥 Am I on track for FIRE retirement?",
            "📈 What is my savings rate?",
            "➕ Add ₹850 dinner expense"
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Search & Source Mode Selector Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Source:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    )

                    AiSearchMode.entries.forEach { mode ->
                        val isSelected = selectedSearchMode == mode
                        val bg = if (isSelected) {
                            when (mode) {
                                AiSearchMode.WEB_SEARCH -> Color(0xFFF3E8FF)
                                AiSearchMode.LOCAL_VAULT -> Color(0xFFDCFCE7)
                                AiSearchMode.CALCULATORS -> Color(0xFFE0F2FE)
                                AiSearchMode.AUTO -> HighDensityPrimary.copy(alpha = 0.12f)
                            }
                        } else {
                            Color(0xFFF8FAFC)
                        }

                        val textColor = if (isSelected) {
                            when (mode) {
                                AiSearchMode.WEB_SEARCH -> Color(0xFF7E22CE)
                                AiSearchMode.LOCAL_VAULT -> Color(0xFF15803D)
                                AiSearchMode.CALCULATORS -> Color(0xFF0369A1)
                                AiSearchMode.AUTO -> HighDensityPrimary
                            }
                        } else {
                            Color(0xFF64748B)
                        }

                        val border = if (isSelected) {
                            BorderStroke(1.5.dp, textColor.copy(alpha = 0.5f))
                        } else {
                            BorderStroke(1.dp, Color(0xFFE2E8F0))
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = bg,
                            border = border,
                            modifier = Modifier
                                .clickable { onSelectSearchMode(mode) }
                                .testTag("search_mode_${mode.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(mode.icon, fontSize = 12.sp)
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.5.sp,
                                        color = textColor
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick suggestion chips bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickPrompts.forEach { prompt ->
                SuggestionChip(
                    onClick = { onSendMessage(prompt.substringAfter(" ")) },
                    label = { Text(prompt, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp)) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = HighDensityPrimary
                    ),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                )
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                ChatMessageItem(
                    message = message,
                    onConfirmWrite = { callId -> onConfirmWrite(message.id, callId) },
                    onCancelWrite = { callId -> onCancelWrite(message.id, callId) }
                )
            }

            if (isThinking) {
                item {
                    ThinkingIndicatorBubble()
                }
            }
        }

        // Active mode notification banner if specific mode is set
        if (selectedSearchMode == AiSearchMode.WEB_SEARCH) {
            Surface(
                color = Color(0xFFFAF5FF),
                border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🌐", fontSize = 12.sp)
                        Text(
                            "Live Web Search Active • External market & tax benchmarks",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Color(0xFF6B21A8), fontWeight = FontWeight.Medium)
                        )
                    }
                    Text(
                        "Reset",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF9333EA), fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        modifier = Modifier.clickable { onSelectSearchMode(AiSearchMode.AUTO) }
                    )
                }
            }
        }

        // Bottom Input Bar with exact Union Insets (zero gap above keyboard, flush with nav bar when closed)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).union(WindowInsets.ime)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val placeholderText = when (selectedSearchMode) {
                    AiSearchMode.WEB_SEARCH -> "Search live web & market intelligence..."
                    AiSearchMode.LOCAL_VAULT -> "Query your private vault & expenses..."
                    AiSearchMode.CALCULATORS -> "Enter numbers for SIP, FIRE, or CAGR math..."
                    AiSearchMode.AUTO -> "Ask anything about your wealth..."
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    placeholder = { Text(placeholderText, fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_ai_query"),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HighDensityPrimary,
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedContainerColor = HighDensityBg,
                        unfocusedContainerColor = HighDensityBg
                    ),
                    maxLines = 4
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText.trim())
                        }
                    },
                    enabled = inputText.isNotBlank() && !isThinking,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank() && !isThinking) HighDensityPrimary else Color(0xFFCBD5E1))
                        .testTag("btn_ai_send")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Chat Bubble Component with Provenance & Write Action Cards
// -------------------------------------------------------------
@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onConfirmWrite: (String) -> Unit,
    onCancelWrite: (String) -> Unit
) {
    val isUser = message.role == ChatRole.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F766E)),
                contentAlignment = Alignment.Center
            ) {
                Text("🤖", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Provenance Badges
            if (!isUser && message.metadata.toolsUsed.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (message.metadata.usedLocalData) {
                        ProvenanceBadge("🔒 Local Data", Color(0xFFDCFCE7), Color(0xFF166534))
                    }
                    if (message.metadata.usedCalculators) {
                        ProvenanceBadge("🧮 Deterministic Math", Color(0xFFE0F2FE), Color(0xFF0369A1))
                    }
                    if (message.metadata.usedWebSearch) {
                        ProvenanceBadge("🌐 Web Research", Color(0xFFF3E8FF), Color(0xFF7E22CE))
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) HighDensityPrimary else MaterialTheme.colorScheme.surface,
                border = if (isUser) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = if (isUser) 1.dp else 2.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    MarkdownFormattedText(
                        text = message.content,
                        isUser = isUser
                    )

                    // Write Confirmation Cards
                    message.toolCalls.filter { it.isWriteOperation }.forEach { call ->
                        Spacer(modifier = Modifier.height(10.dp))
                        WriteConfirmationCard(
                            toolCall = call,
                            onConfirm = { onConfirmWrite(call.id) },
                            onCancel = { onCancelWrite(call.id) }
                        )
                    }

                    // Sources Carousel / Badges
                    if (message.sources.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Sources Cited:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF64748B)))
                        Spacer(modifier = Modifier.height(4.dp))
                        message.sources.forEach { src ->
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Text(src.title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A)))
                                    if (src.snippet.isNotBlank()) {
                                        Text(src.snippet, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Color(0xFF64748B)), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProvenanceBadge(text: String, bgColor: Color, textColor: Color) {
    Surface(color = bgColor, shape = RoundedCornerShape(4.dp)) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun WriteConfirmationCard(
    toolCall: ToolCall,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        color = Color(0xFFFFFBEB),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Security, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                Text(
                    text = "Write Action Verification",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            toolCall.writePayloadPreview.forEach { (k, v) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("$k:", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = Color(0xFF78350F)))
                    Text(v, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F)))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (toolCall.writeStatus) {
                WriteStatus.PENDING_CONFIRMATION -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB91C1C))
                        ) {
                            Text("Cancel", fontSize = 12.sp)
                        }
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                        ) {
                            Text("Confirm & Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                WriteStatus.EXECUTED -> {
                    Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = "✅ Saved to local Room database",
                            color = Color(0xFF166534),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
                WriteStatus.CANCELLED -> {
                    Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = "🚫 Action Cancelled",
                            color = Color(0xFF991B1B),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun ThinkingIndicatorBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F766E)),
            contentAlignment = Alignment.Center
        ) {
            Text("🤖", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = HighDensityPrimary)
                Text(
                    text = "Running local deterministic tools...",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = Color(0xFF64748B))
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 2. Local Models Tab View
// -------------------------------------------------------------
@Composable
fun LocalModelsView(
    selectedModel: ModelInfo,
    availableModels: List<ModelInfo>,
    installProgress: Float?,
    onSelectModel: (String) -> Unit,
    onInstallModel: (String) -> Unit,
    onDeleteModel: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFF166534), modifier = Modifier.size(20.dp))
                        Text("100% On-Device Privacy Architecture", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF166534)))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Zero external LLM APIs (No OpenAI, No Anthropic, No Gemini Cloud)\n" +
                                "• ₹0 Ongoing AI / API cost\n" +
                                "• Inference runs on your phone's CPU/NPU\n" +
                                "• Financial computations executed by deterministic Kotlin domain functions",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, color = Color(0xFF14532D), lineHeight = 18.sp)
                    )
                }
            }
        }

        item {
            Text("Available On-Device LLM Models", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = HighDensityPrimary))
        }

        items(availableModels) { model ->
            val isSelected = model.id == selectedModel.id
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFFF8FAFC) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) HighDensityPrimary else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(model.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.5.sp))
                            Text("${model.modelFamily} • ${model.quantization} • ${model.parameterCount}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, color = Color(0xFF64748B)))
                        }
                        if (isSelected) {
                            Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(6.dp)) {
                                Text("ACTIVE", color = Color(0xFF166534), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(model.description, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = Color(0xFF475569)))

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("RAM: ~${model.memoryRequirementMb} MB • Context: ${model.contextLength} tokens", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = Color(0xFF94A3B8)))

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (model.isInstalled) {
                                if (!isSelected) {
                                    Button(
                                        onClick = { onSelectModel(model.id) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
                                    ) {
                                        Text("Select", fontSize = 12.sp)
                                    }
                                }
                                if (model.id != "embedded_engine") {
                                    OutlinedButton(
                                        onClick = { onDeleteModel(model.id) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                                    ) {
                                        Text("Delete", fontSize = 12.sp)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { onInstallModel(model.id) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                                ) {
                                    Text("Download & Install", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    if (installProgress != null && !model.isInstalled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { installProgress }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. MCP & Privacy Sandbox Tab View
// -------------------------------------------------------------
@Composable
fun McpPrivacyView(
    connections: List<McpServerConfig>,
    onToggleConnection: (String, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Hub, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(20.dp))
                        Text("Model Context Protocol (MCP) Boundary", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8)))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Wealth Tracker isolates the LLM from direct SQL/Room database access. The assistant queries verified data strictly through deterministic tool interfaces.\n\nExternal MCP servers and web search providers are permanently blocked from receiving personal financial data (salaries, net worth, assets, and expenses).",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, color = Color(0xFF1E3A8A), lineHeight = 18.sp)
                    )
                }
            }
        }

        item {
            Text("Configured MCP Connections", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = HighDensityPrimary))
        }

        items(connections) { config ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(config.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp))
                                if (config.isTrusted) {
                                    Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(4.dp)) {
                                        Text("TRUSTED", color = Color(0xFF166534), style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Text("Type: ${config.type.name}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, color = Color(0xFF64748B)))
                        }

                        Switch(
                            checked = config.isConnected,
                            onCheckedChange = { onToggleConnection(config.id, it) },
                            enabled = config.id != "local_sandbox" // Local sandbox is required for core functionality
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(config.description, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = Color(0xFF475569)))

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Allowed Permissions:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF64748B)))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        config.allowedPermissions.forEach { perm ->
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = perm.name,
                                    color = Color(0xFF334155),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
