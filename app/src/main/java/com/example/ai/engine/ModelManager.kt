package com.example.ai.engine

import android.content.Context
import android.content.SharedPreferences
import com.example.ai.ModelInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

interface ModelManager {
    val selectedModel: StateFlow<ModelInfo>
    val installProgress: StateFlow<Float?>
    fun getAvailableModels(): List<ModelInfo>
    fun selectModel(modelId: String)
    suspend fun installModel(modelId: String, onProgress: (Float) -> Unit): Result<Unit>
    suspend fun deleteModel(modelId: String): Result<Unit>
}

class LocalModelManager(private val context: Context) : ModelManager {

    private val prefs: SharedPreferences = context.getSharedPreferences("wealth_ai_models", Context.MODE_PRIVATE)

    private val modelsList = listOf(
        ModelInfo(
            id = "gemini_2_5_flash",
            name = "Gemini 2.5 Flash (Advanced Cloud & Hybrid AI)",
            modelFamily = "Gemini 2.5",
            parameterCount = "Frontier",
            quantization = "FP16 Cloud / Hybrid",
            memoryRequirementMb = 0,
            contextLength = 32768,
            downloadUrl = "https://ai.google.dev",
            localFilePath = "cloud://gemini-2.5-flash",
            isInstalled = true,
            license = "Google AI",
            description = "Google DeepMind's flagship fast frontier model for comprehensive wealth planning, investment strategies, and deep financial diagnostics."
        ),
        ModelInfo(
            id = "embedded_engine",
            name = "Wealth AI Embedded (Local Rule & Math Engine)",
            modelFamily = "WealthTracker",
            parameterCount = "Embedded",
            quantization = "Deterministic Native",
            memoryRequirementMb = 24,
            contextLength = 4096,
            downloadUrl = "local://embedded",
            localFilePath = "app://assets/embedded_engine",
            isInstalled = true,
            license = "Apache 2.0",
            description = "High-speed zero-latency local financial reasoning engine. 100% offline with ₹0 cost and zero download."
        ),
        ModelInfo(
            id = "gemma_2b_q4",
            name = "Gemma 2B Instruct (4-bit GGUF)",
            modelFamily = "Gemma 2",
            parameterCount = "2.6 Billion",
            quantization = "Q4_K_M",
            memoryRequirementMb = 1450,
            contextLength = 4096,
            downloadUrl = "https://huggingface.co/google/gemma-2b-it-GGUF",
            localFilePath = File(context.filesDir, "models/gemma-2b-q4.gguf").absolutePath,
            isInstalled = prefs.getBoolean("installed_gemma_2b_q4", true),
            license = "Gemma Open",
            description = "Google DeepMind lightweight instruct model optimized for Android CPU/NPU inference."
        ),
        ModelInfo(
            id = "qwen_1_5b_q4",
            name = "Qwen 2.5 1.5B Instruct (4-bit)",
            modelFamily = "Qwen 2.5",
            parameterCount = "1.5 Billion",
            quantization = "Q4_0",
            memoryRequirementMb = 980,
            contextLength = 8192,
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF",
            localFilePath = File(context.filesDir, "models/qwen-1.5b-q4.gguf").absolutePath,
            isInstalled = prefs.getBoolean("installed_qwen_1_5b_q4", false),
            license = "Apache 2.0",
            description = "Ultra-fast reasoning model with broad financial domain support and small memory footprint."
        ),
        ModelInfo(
            id = "tinyllama_1_1b",
            name = "TinyLlama 1.1B Chat (4-bit)",
            modelFamily = "TinyLlama",
            parameterCount = "1.1 Billion",
            quantization = "Q4_K_S",
            memoryRequirementMb = 640,
            contextLength = 2048,
            downloadUrl = "https://huggingface.co/TinyLlama/TinyLlama-1.1B-Chat-v1.0-GGUF",
            localFilePath = File(context.filesDir, "models/tinyllama-1.1b-q4.gguf").absolutePath,
            isInstalled = prefs.getBoolean("installed_tinyllama_1_1b", false),
            license = "Apache 2.0",
            description = "Compact model optimized for low-end devices and instant startup times."
        )
    )

    private val _selectedModel = MutableStateFlow(
        modelsList.find { it.id == prefs.getString("selected_model_id", "gemini_2_5_flash") } ?: modelsList.first()
    )
    override val selectedModel: StateFlow<ModelInfo> = _selectedModel.asStateFlow()

    private val _installProgress = MutableStateFlow<Float?>(null)
    override val installProgress: StateFlow<Float?> = _installProgress.asStateFlow()

    override fun getAvailableModels(): List<ModelInfo> {
        val currentId = _selectedModel.value.id
        return modelsList.map { model ->
            val isInst = if (model.id == "embedded_engine") true else prefs.getBoolean("installed_${model.id}", model.id == "gemma_2b_q4")
            model.copy(isInstalled = isInst)
        }
    }

    override fun selectModel(modelId: String) {
        val model = getAvailableModels().find { it.id == modelId } ?: return
        prefs.edit().putString("selected_model_id", modelId).apply()
        _selectedModel.value = model
    }

    override suspend fun installModel(modelId: String, onProgress: (Float) -> Unit): Result<Unit> {
        if (modelId == "embedded_engine") return Result.success(Unit)

        _installProgress.value = 0.0f
        try {
            // Emulate reliable chunked model download into local app directory
            for (step in 1..10) {
                delay(120)
                val progress = step / 10f
                _installProgress.value = progress
                onProgress(progress)
            }
            prefs.edit().putBoolean("installed_$modelId", true).apply()
            _installProgress.value = null
            selectModel(modelId)
            return Result.success(Unit)
        } catch (e: Exception) {
            _installProgress.value = null
            return Result.failure(e)
        }
    }

    override suspend fun deleteModel(modelId: String): Result<Unit> {
        if (modelId == "embedded_engine") return Result.failure(IllegalArgumentException("Cannot delete embedded engine."))
        prefs.edit().putBoolean("installed_$modelId", false).apply()
        if (_selectedModel.value.id == modelId) {
            selectModel("embedded_engine")
        }
        return Result.success(Unit)
    }
}
