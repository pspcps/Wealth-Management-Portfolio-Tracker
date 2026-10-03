package com.example.domain.finance

import com.example.data.local.entity.AssetEntity
import com.example.data.local.entity.AssetMonthlyValueEntity
import com.example.data.local.entity.MonthlySnapshotEntity
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

object XirrEngine {

    /**
     * Builds the complete list of cash flows for an asset and calculates its XIRR summary.
     */
    fun calculateAssetXirr(
        asset: AssetEntity,
        historicalValues: List<AssetMonthlyValueEntity>,
        snapshots: List<MonthlySnapshotEntity>,
        currentValue: Double,
        asOfSnapshotId: String? = null
    ): AssetXirrSummary {
        val sortedSnapshots = snapshots.sortedWith(compareBy({ it.year }, { it.month }))
        val valuesBySnapshot = historicalValues.associateBy { it.snapshotId }

        val activeSnapshot = sortedSnapshots.lastOrNull { asOfSnapshotId == null || it.id <= asOfSnapshotId }
            ?: sortedSnapshots.lastOrNull()
        val currentEvalDate = activeSnapshot?.let {
            "${it.year}-${String.format("%02d", it.month)}-28"
        } ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Determine effective start date
        val firstSnapshotWithValue = sortedSnapshots.firstOrNull { snap ->
            val v = valuesBySnapshot[snap.id]
            v != null && (v.value > 0 || v.investedAmount > 0)
        }

        val effectiveStartDate = when {
            asset.investmentDate.isNotBlank() -> asset.investmentDate
            firstSnapshotWithValue != null -> "${firstSnapshotWithValue.year}-${String.format("%02d", firstSnapshotWithValue.month)}-01"
            else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(asset.createdAt))
        }

        val firstRecordedVal = firstSnapshotWithValue?.let { valuesBySnapshot[it.id] }
        val initialInvested = when {
            asset.initialInvestedAmount > 0 -> asset.initialInvestedAmount
            firstRecordedVal != null && firstRecordedVal.investedAmount > 0 -> firstRecordedVal.investedAmount
            firstRecordedVal != null && firstRecordedVal.value > 0 -> firstRecordedVal.value
            else -> 0.0
        }

        val cashFlows = mutableListOf<CashFlowItem>()

        // 1. Initial Outflow
        if (initialInvested > 0) {
            cashFlows.add(CashFlowItem(date = effectiveStartDate, amount = -initialInvested))
        }

        // 2. Subsequent monthly flows
        var previousInvested = initialInvested
        sortedSnapshots.forEach { snap ->
            // Only consider snapshots up to the current active evaluation snapshot
            if (asOfSnapshotId != null && snap.id > asOfSnapshotId) return@forEach

            val snapDate = "${snap.year}-${String.format("%02d", snap.month)}-05"
            // Skip if on or before initial investment date
            if (snapDate <= effectiveStartDate) return@forEach

            val v = valuesBySnapshot[snap.id] ?: return@forEach

            if (v.contributions > 0) {
                cashFlows.add(CashFlowItem(date = snapDate, amount = -v.contributions))
            } else if (v.withdrawals > 0) {
                cashFlows.add(CashFlowItem(date = snapDate, amount = v.withdrawals))
            } else if (v.investedAmount > 0 && previousInvested > 0) {
                val delta = v.investedAmount - previousInvested
                if (delta > 10.0) { // New capital added
                    cashFlows.add(CashFlowItem(date = snapDate, amount = -delta))
                } else if (delta < -10.0) { // Capital withdrawn
                    cashFlows.add(CashFlowItem(date = snapDate, amount = -delta))
                }
            }

            if (v.investedAmount > 0) {
                previousInvested = v.investedAmount
            }
        }

        // 3. Terminal Inflow (Current Market Value)
        if (currentValue > 0) {
            cashFlows.add(CashFlowItem(date = currentEvalDate, amount = currentValue))
        }

        // Calculate holding period
        val days = try {
            val start = LocalDate.parse(effectiveStartDate.take(10))
            val end = LocalDate.parse(currentEvalDate.take(10))
            ChronoUnit.DAYS.between(start, end).coerceAtLeast(0)
        } catch (e: Exception) {
            0L
        }

        val totalOutflows = cashFlows.filter { it.amount < 0 }.sumOf { -it.amount }
        val absoluteGain = currentValue - totalOutflows
        val returnPercentage = if (totalOutflows > 0) (absoluteGain / totalOutflows) * 100.0 else null

        val xirrResult = FinanceCalculators.calculateXirr(cashFlows)

        return AssetXirrSummary(
            xirrPercentage = if (xirrResult.isValid) xirrResult.annualRatePercentage else null,
            isValid = xirrResult.isValid,
            holdingPeriodDays = days,
            holdingPeriodFormatted = FinanceCalculators.formatHoldingPeriod(days),
            totalInvested = totalOutflows,
            currentValue = currentValue,
            absoluteGain = absoluteGain,
            returnPercentage = returnPercentage,
            cashFlows = cashFlows,
            message = xirrResult.message
        )
    }

    /**
     * Calculates combined XIRR for a group of assets (e.g. Category or Entire Portfolio).
     */
    fun calculateAggregatedXirr(
        assetXirrSummaries: List<AssetXirrSummary>,
        asOfDate: String
    ): Double? {
        val combinedFlows = mutableListOf<CashFlowItem>()
        assetXirrSummaries.forEach { summary ->
            // Take all outflows and intermediate flows (everything except terminal value)
            val nonTerminal = summary.cashFlows.filter { it.amount < 0 }
            combinedFlows.addAll(nonTerminal)
        }
        val totalCurrentVal = assetXirrSummaries.sumOf { it.currentValue }
        if (totalCurrentVal > 0) {
            combinedFlows.add(CashFlowItem(date = asOfDate, amount = totalCurrentVal))
        }

        if (combinedFlows.size < 2) return null
        val res = FinanceCalculators.calculateXirr(combinedFlows)
        return if (res.isValid) res.annualRatePercentage else null
    }
}
