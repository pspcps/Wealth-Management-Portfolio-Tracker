package com.example.util

import android.content.Context
import com.example.data.local.entity.AssetEntity
import com.example.data.local.entity.AssetMonthlyValueEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.MonthlySnapshotEntity
import com.example.data.local.entity.RecurringCashFlowEntity
import com.example.data.repository.PortfolioRepository
import org.json.JSONArray
import org.json.JSONObject

data class ImportResult(
    val success: Boolean,
    val message: String,
    val snapshotId: String = "",
    val snapshotLabel: String = "",
    val holdingsCount: Int = 0,
    val sipsCount: Int = 0,
    val totalValue: Double = 0.0
)

data class ImportPreview(
    val snapshotId: String,
    val snapshotLabel: String,
    val totalValue: Double,
    val holdingsCount: Int,
    val sipsCount: Int,
    val sampleHoldings: List<String>
)

object SnapshotImporter {

    fun parsePreview(jsonString: String): ImportPreview? {
        return try {
            val root = JSONObject(jsonString.trim())
            val snapshotId = root.optString("snapshotId", "2026-08")
            val snapshotLabel = root.optString("snapshotLabel", snapshotId)
            val totalValue = root.optDouble("totalPortfolioValue", 0.0)
            
            val holdingsArray = root.optJSONArray("holdings") ?: JSONArray()
            val sipsArray = root.optJSONArray("recurringSips") ?: JSONArray()
            
            val samples = mutableListOf<String>()
            for (i in 0 until minOf(5, holdingsArray.length())) {
                val h = holdingsArray.getJSONObject(i)
                val name = h.optString("name", "Holding")
                val curVal = h.optDouble("currentValue", 0.0)
                samples.add("$name (₹${Math.round(curVal)})")
            }

            ImportPreview(
                snapshotId = snapshotId,
                snapshotLabel = snapshotLabel,
                totalValue = totalValue,
                holdingsCount = holdingsArray.length(),
                sipsCount = sipsArray.length(),
                sampleHoldings = samples
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun importSnapshotJson(
        repository: PortfolioRepository,
        jsonString: String
    ): ImportResult {
        return try {
            val root = JSONObject(jsonString.trim())
            val snapshotId = root.optString("snapshotId", "").ifEmpty { "2026-08" }
            val snapshotLabel = root.optString("snapshotLabel", snapshotId)
            val totalPortfolioValue = root.optDouble("totalPortfolioValue", 0.0)

            // Parse year and month
            val parts = snapshotId.split("-")
            val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
            val month = parts.getOrNull(1)?.toIntOrNull() ?: 8

            // 1. Ensure snapshot entity exists
            val existingSnap = repository.getSnapshotById(snapshotId)
            if (existingSnap == null) {
                repository.saveSnapshot(
                    MonthlySnapshotEntity(
                        id = snapshotId,
                        year = year,
                        month = month,
                        notes = "Imported from JSON on ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}",
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            // Ensure standard categories exist
            repository.ensureDefaults()
            val existingCategories = repository.getAllCategoriesList().associateBy { it.id }.toMutableMap()

            // 2. Process Holdings
            val holdingsArray = root.optJSONArray("holdings") ?: JSONArray()
            val existingAssets = repository.getAllAssetsList().toMutableList()
            val valuesToSave = mutableListOf<AssetMonthlyValueEntity>()
            var importedHoldingsCount = 0

            for (i in 0 until holdingsArray.length()) {
                val h = holdingsArray.getJSONObject(i)
                val rawCatId = h.optString("categoryId", "other").trim().lowercase()
                val name = h.optString("name", "").trim()
                if (name.isEmpty()) continue

                val investedAmount = h.optDouble("investedAmount", 0.0)
                val currentValue = h.optDouble("currentValue", investedAmount)
                val status = h.optString("status", "ACTIVE")
                val investmentDate = h.optString("investmentDate", "")

                // Map or create category if not exists
                val categoryId = if (existingCategories.containsKey(rawCatId)) {
                    rawCatId
                } else {
                    // Create dynamic category
                    val newCat = CategoryEntity(
                        id = rawCatId,
                        name = rawCatId.replace("_", " ").replaceFirstChar { it.uppercase() },
                        colorHex = "#3B82F6",
                        iconName = "account_balance_wallet",
                        groupType = "INVESTMENT",
                        orderIndex = existingCategories.size + 1
                    )
                    repository.saveCategory(newCat)
                    existingCategories[rawCatId] = newCat
                    rawCatId
                }

                // Check if asset already exists by name (case-insensitive) or category
                var matchedAsset = existingAssets.firstOrNull { it.name.equals(name, ignoreCase = true) }
                if (matchedAsset == null) {
                    val newAsset = AssetEntity(
                        name = name,
                        categoryId = categoryId,
                        initialInvestedAmount = investedAmount,
                        investmentDate = investmentDate,
                        status = status
                    )
                    val generatedId = repository.saveAsset(newAsset)
                    matchedAsset = newAsset.copy(id = generatedId)
                    existingAssets.add(matchedAsset)
                } else {
                    // Update initialInvestedAmount if needed
                    if (investedAmount > 0 && matchedAsset.initialInvestedAmount <= 0) {
                        val updatedAsset = matchedAsset.copy(initialInvestedAmount = investedAmount, categoryId = categoryId)
                        repository.saveAsset(updatedAsset)
                        matchedAsset = updatedAsset
                    }
                }

                // Prepare monthly value entity
                valuesToSave.add(
                    AssetMonthlyValueEntity(
                        assetId = matchedAsset.id,
                        snapshotId = snapshotId,
                        value = currentValue,
                        investedAmount = investedAmount,
                        contributions = 0.0,
                        withdrawals = 0.0,
                        notes = ""
                    )
                )
                importedHoldingsCount++
            }

            if (valuesToSave.isNotEmpty()) {
                repository.saveAssetValues(valuesToSave)
            }

            // 3. Process Recurring Cash Flows (SIPs, Salary, Meal Coupon)
            val sipsArray = root.optJSONArray("recurringSips") ?: JSONArray()
            val existingFlows = repository.getAllRecurringCashFlowsList()
            var importedSipsCount = 0

            for (i in 0 until sipsArray.length()) {
                val s = sipsArray.getJSONObject(i)
                val sipName = s.optString("name", "").trim()
                if (sipName.isEmpty()) continue

                val type = s.optString("type", "SIP")
                val monthlyAmount = s.optDouble("monthlyAmount", 0.0)
                val startMonth = s.optString("startMonth", "2025-01")
                val endMonth = if (s.has("endMonth") && !s.isNull("endMonth")) s.optString("endMonth") else null
                val isActive = s.optBoolean("isActive", true)

                // Match existing by name
                val existing = existingFlows.firstOrNull { it.name.equals(sipName, ignoreCase = true) }
                val entity = RecurringCashFlowEntity(
                    id = existing?.id ?: 0,
                    name = sipName,
                    type = type,
                    monthlyAmount = monthlyAmount,
                    startMonth = startMonth,
                    endMonth = endMonth,
                    isActive = isActive
                )
                repository.saveRecurringCashFlow(entity)
                importedSipsCount++
            }

            ImportResult(
                success = true,
                message = "Successfully imported $importedHoldingsCount holdings & $importedSipsCount recurring flows into $snapshotLabel ($snapshotId).",
                snapshotId = snapshotId,
                snapshotLabel = snapshotLabel,
                holdingsCount = importedHoldingsCount,
                sipsCount = importedSipsCount,
                totalValue = if (totalPortfolioValue > 0) totalPortfolioValue else valuesToSave.sumOf { it.value }
            )
        } catch (e: Exception) {
            ImportResult(
                success = false,
                message = "Failed to parse and import JSON: ${e.localizedMessage ?: "Invalid JSON format"}"
            )
        }
    }
}
