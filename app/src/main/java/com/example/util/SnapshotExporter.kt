package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.AssetEntity
import com.example.data.local.entity.AssetMonthlyValueEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RecurringCashFlowEntity
import com.example.ui.components.CurrencyFormatter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SnapshotExporter {

    fun generateCsvContent(
        snapshotId: String,
        snapshotLabel: String,
        categories: List<CategoryEntity>,
        assets: List<AssetEntity>,
        values: List<AssetMonthlyValueEntity>,
        sips: List<RecurringCashFlowEntity>
    ): String {
        val categoriesById = categories.associateBy { it.id }
        val valuesByAsset = values.associateBy { it.assetId }
        val sipsByAsset = sips.filter { it.isActive && it.startMonth <= snapshotId && (it.endMonth == null || it.endMonth >= snapshotId) }
            .groupBy { it.assetId }

        val sb = StringBuilder()
        sb.append("Wealth Portfolio Snapshot Report - $snapshotLabel ($snapshotId)\n")
        sb.append("Generated on: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}\n\n")
        sb.append("Category,Asset Name,Invested Amount (INR),Current Value (INR),Gain / Loss (INR),Return (%),Active Monthly SIP (INR),Status\n")

        var totalInvested = 0.0
        var totalCurrent = 0.0
        var totalSip = 0.0

        val groupedAssets = assets.groupBy { it.categoryId }
        categories.forEach { category ->
            val catAssets = groupedAssets[category.id] ?: emptyList()
            if (catAssets.isNotEmpty()) {
                catAssets.forEach { asset ->
                    val valueObj = valuesByAsset[asset.id]
                    val currentVal = valueObj?.value ?: 0.0
                    val investedVal = if (valueObj != null && valueObj.investedAmount > 0) valueObj.investedAmount else asset.initialInvestedAmount
                    val gainLoss = currentVal - investedVal
                    val returnPct = if (investedVal > 0) (gainLoss / investedVal) * 100.0 else 0.0
                    val assetSips = sipsByAsset[asset.id]?.sumOf { it.monthlyAmount } ?: 0.0

                    totalInvested += investedVal
                    totalCurrent += currentVal
                    totalSip += assetSips

                    val cleanCatName = category.name.replace(",", " ")
                    val cleanAssetName = asset.name.replace(",", " ")
                    sb.append("\"$cleanCatName\",\"$cleanAssetName\",%.2f,%.2f,%.2f,%.2f,%.2f,\"%s\"\n".format(
                        Locale.US,
                        investedVal,
                        currentVal,
                        gainLoss,
                        returnPct,
                        assetSips,
                        asset.status
                    ))
                }
            }
        }

        sb.append("\n")
        val totalGain = totalCurrent - totalInvested
        val totalReturnPct = if (totalInvested > 0) (totalGain / totalInvested) * 100.0 else 0.0
        sb.append("\"TOTAL PORTFOLIO\",\"-\",%.2f,%.2f,%.2f,%.2f,%.2f,\"-\"\n".format(
            Locale.US,
            totalInvested,
            totalCurrent,
            totalGain,
            totalReturnPct,
            totalSip
        ))
        return sb.toString()
    }

    fun generateStatementContent(
        snapshotId: String,
        snapshotLabel: String,
        totalPortfolioValue: Double,
        categories: List<CategoryEntity>,
        assets: List<AssetEntity>,
        values: List<AssetMonthlyValueEntity>,
        sips: List<RecurringCashFlowEntity>
    ): String {
        val categoriesById = categories.associateBy { it.id }
        val valuesByAsset = values.associateBy { it.assetId }
        val activeSips = sips.filter { it.isActive && it.startMonth <= snapshotId && (it.endMonth == null || it.endMonth >= snapshotId) }
        val sipsByAsset = activeSips.groupBy { it.assetId }

        val sb = StringBuilder()
        sb.append("====================================================\n")
        sb.append("           WEALTH PORTFOLIO SNAPSHOT STATEMENT       \n")
        sb.append("====================================================\n")
        sb.append("Period: $snapshotLabel ($snapshotId)\n")
        sb.append("Date: ${SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()).format(Date())}\n")
        sb.append("----------------------------------------------------\n\n")

        var totalInvested = 0.0
        var totalCurrent = 0.0
        val groupedAssets = assets.groupBy { it.categoryId }

        categories.forEach { category ->
            val catAssets = groupedAssets[category.id] ?: emptyList()
            val catTotal = catAssets.sumOf { valuesByAsset[it.id]?.value ?: 0.0 }
            val catInvested = catAssets.sumOf { 
                val v = valuesByAsset[it.id]
                if (v != null && v.investedAmount > 0) v.investedAmount else it.initialInvestedAmount
            }
            if (catTotal > 0 || catAssets.isNotEmpty()) {
                sb.append("📁 [${category.name.uppercase()}]\n")
                sb.append("   Category Total: ${CurrencyFormatter.formatInr(catTotal, ignorePrivacy = true)} (Invested: ${CurrencyFormatter.formatInr(catInvested, ignorePrivacy = true)})\n")
                catAssets.forEach { asset ->
                    val currentVal = valuesByAsset[asset.id]?.value ?: 0.0
                    val investedVal = valuesByAsset[asset.id]?.let { if (it.investedAmount > 0) it.investedAmount else asset.initialInvestedAmount } ?: asset.initialInvestedAmount
                    val assetSip = sipsByAsset[asset.id]?.sumOf { it.monthlyAmount } ?: 0.0
                    val gain = currentVal - investedVal
                    val pct = if (investedVal > 0) (gain / investedVal) * 100.0 else 0.0
                    
                    val sipTag = if (assetSip > 0) " (SIP: ${CurrencyFormatter.formatInr(assetSip, ignorePrivacy = true)}/mo)" else ""
                    sb.append("   • ${asset.name}: ${CurrencyFormatter.formatInr(currentVal, ignorePrivacy = true)} [Gain: ${if (gain >= 0) "+" else ""}${CurrencyFormatter.formatInr(gain, ignorePrivacy = true)} (${String.format(Locale.US, "%.1f", pct)}%)]$sipTag\n")
                    totalInvested += investedVal
                    totalCurrent += currentVal
                }
                sb.append("\n")
            }
        }

        val totalGain = totalCurrent - totalInvested
        val totalReturnPct = if (totalInvested > 0) (totalGain / totalInvested) * 100.0 else 0.0
        val totalMonthlySip = activeSips.filter { it.type == "SIP" || it.type == "RECURRING_INVESTMENT" }.sumOf { it.monthlyAmount }

        sb.append("====================================================\n")
        sb.append("SUMMARY OVERVIEW\n")
        sb.append("----------------------------------------------------\n")
        sb.append("Total Portfolio Net Worth: ${CurrencyFormatter.formatInr(totalCurrent, ignorePrivacy = true)}\n")
        sb.append("Total Capital Invested:    ${CurrencyFormatter.formatInr(totalInvested, ignorePrivacy = true)}\n")
        sb.append("Total Unrealized Gain:     ${if (totalGain >= 0) "+" else ""}${CurrencyFormatter.formatInr(totalGain, ignorePrivacy = true)} (${String.format(Locale.US, "%.2f", totalReturnPct)}%)\n")
        sb.append("Active Monthly SIPs:       ${CurrencyFormatter.formatInr(totalMonthlySip, ignorePrivacy = true)} / month\n")
        sb.append("====================================================\n")
        return sb.toString()
    }

    fun generateJsonContent(
        snapshotId: String,
        snapshotLabel: String,
        totalPortfolioValue: Double,
        categories: List<CategoryEntity>,
        assets: List<AssetEntity>,
        values: List<AssetMonthlyValueEntity>,
        sips: List<RecurringCashFlowEntity>
    ): String {
        val valuesByAsset = values.associateBy { it.assetId }
        val jsonBuilder = StringBuilder()
        jsonBuilder.append("{\n")
        jsonBuilder.append("  \"snapshotId\": \"$snapshotId\",\n")
        jsonBuilder.append("  \"snapshotLabel\": \"$snapshotLabel\",\n")
        jsonBuilder.append("  \"exportedAt\": \"${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())}\",\n")
        jsonBuilder.append("  \"totalPortfolioValue\": $totalPortfolioValue,\n")
        jsonBuilder.append("  \"holdings\": [\n")

        assets.forEachIndexed { index, asset ->
            val v = valuesByAsset[asset.id]
            val currentVal = v?.value ?: 0.0
            val investedVal = if (v != null && v.investedAmount > 0) v.investedAmount else asset.initialInvestedAmount
            val comma = if (index < assets.size - 1) "," else ""
            jsonBuilder.append("    {\n")
            jsonBuilder.append("      \"assetId\": ${asset.id},\n")
            jsonBuilder.append("      \"categoryId\": \"${asset.categoryId}\",\n")
            jsonBuilder.append("      \"name\": \"${asset.name.replace("\"", "\\\"")}\",\n")
            jsonBuilder.append("      \"investedAmount\": $investedVal,\n")
            jsonBuilder.append("      \"currentValue\": $currentVal,\n")
            jsonBuilder.append("      \"status\": \"${asset.status}\"\n")
            jsonBuilder.append("    }$comma\n")
        }
        jsonBuilder.append("  ],\n")
        jsonBuilder.append("  \"recurringSips\": [\n")
        sips.forEachIndexed { index, sip ->
            val comma = if (index < sips.size - 1) "," else ""
            jsonBuilder.append("    {\n")
            jsonBuilder.append("      \"id\": ${sip.id},\n")
            jsonBuilder.append("      \"name\": \"${sip.name.replace("\"", "\\\"")}\",\n")
            jsonBuilder.append("      \"type\": \"${sip.type}\",\n")
            jsonBuilder.append("      \"monthlyAmount\": ${sip.monthlyAmount},\n")
            jsonBuilder.append("      \"startMonth\": \"${sip.startMonth}\",\n")
            jsonBuilder.append("      \"endMonth\": ${if (sip.endMonth != null) "\"${sip.endMonth}\"" else "null"},\n")
            jsonBuilder.append("      \"isActive\": ${sip.isActive}\n")
            jsonBuilder.append("    }$comma\n")
        }
        jsonBuilder.append("  ]\n")
        jsonBuilder.append("}")
        return jsonBuilder.toString()
    }

    fun exportSnapshotCsv(context: Context, snapshotId: String, snapshotLabel: String, categories: List<CategoryEntity>, assets: List<AssetEntity>, values: List<AssetMonthlyValueEntity>, sips: List<RecurringCashFlowEntity>) {
        val file = saveFile(context, "portfolio_snapshot_${snapshotId}.csv", generateCsvContent(snapshotId, snapshotLabel, categories, assets, values, sips))
        Toast.makeText(context, "Saved to ${file.name}", Toast.LENGTH_LONG).show()
    }

    fun shareSnapshotCsv(context: Context, snapshotId: String, snapshotLabel: String, categories: List<CategoryEntity>, assets: List<AssetEntity>, values: List<AssetMonthlyValueEntity>, sips: List<RecurringCashFlowEntity>) {
        val file = saveFile(context, "portfolio_snapshot_${snapshotId}.csv", generateCsvContent(snapshotId, snapshotLabel, categories, assets, values, sips))
        shareFile(context, file, "text/csv", "Portfolio Snapshot $snapshotId (CSV)")
    }

    fun exportSnapshotStatement(context: Context, snapshotId: String, snapshotLabel: String, totalPortfolioValue: Double, categories: List<CategoryEntity>, assets: List<AssetEntity>, values: List<AssetMonthlyValueEntity>, sips: List<RecurringCashFlowEntity>) {
        val file = saveFile(context, "portfolio_statement_${snapshotId}.txt", generateStatementContent(snapshotId, snapshotLabel, totalPortfolioValue, categories, assets, values, sips))
        Toast.makeText(context, "Saved to ${file.name}", Toast.LENGTH_LONG).show()
    }

    fun shareSnapshotStatement(context: Context, snapshotId: String, snapshotLabel: String, totalPortfolioValue: Double, categories: List<CategoryEntity>, assets: List<AssetEntity>, values: List<AssetMonthlyValueEntity>, sips: List<RecurringCashFlowEntity>) {
        val file = saveFile(context, "portfolio_statement_${snapshotId}.txt", generateStatementContent(snapshotId, snapshotLabel, totalPortfolioValue, categories, assets, values, sips))
        shareFile(context, file, "text/plain", "Portfolio Statement $snapshotId")
    }

    fun exportSnapshotJson(context: Context, snapshotId: String, snapshotLabel: String, totalPortfolioValue: Double, categories: List<CategoryEntity>, assets: List<AssetEntity>, values: List<AssetMonthlyValueEntity>, sips: List<RecurringCashFlowEntity>) {
        val file = saveFile(context, "portfolio_snapshot_${snapshotId}.json", generateJsonContent(snapshotId, snapshotLabel, totalPortfolioValue, categories, assets, values, sips))
        Toast.makeText(context, "Saved to ${file.name}", Toast.LENGTH_LONG).show()
    }

    fun shareSnapshotJson(context: Context, snapshotId: String, snapshotLabel: String, totalPortfolioValue: Double, categories: List<CategoryEntity>, assets: List<AssetEntity>, values: List<AssetMonthlyValueEntity>, sips: List<RecurringCashFlowEntity>) {
        val file = saveFile(context, "portfolio_snapshot_${snapshotId}.json", generateJsonContent(snapshotId, snapshotLabel, totalPortfolioValue, categories, assets, values, sips))
        shareFile(context, file, "application/json", "Portfolio Snapshot $snapshotId (JSON)")
    }

    private fun saveFile(context: Context, filename: String, content: String): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, filename)
        file.writeText(content)
        return file
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Here is the $title exported from Wealth Portfolio Tracker.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Download / Share $title").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
