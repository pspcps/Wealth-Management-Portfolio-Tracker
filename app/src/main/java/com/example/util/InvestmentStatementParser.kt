package com.example.util

import android.content.Context
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipFile

enum class InvestmentStatementType(val displayName: String, val defaultCategoryId: String) {
    MUTUAL_FUNDS("Mutual Funds", "mutual_funds"),
    STOCKS("Stocks & ETFs", "indian_stocks"),
    AUTO_DETECT("Auto-Detect from File", "mutual_funds")
}

data class ParsedInvestmentHolding(
    val name: String,
    val categoryId: String,
    val subCategory: String = "",
    val amcOrIssuer: String = "",
    val identifier: String = "", // Folio No. or ISIN
    val units: Double = 0.0,
    val avgBuyPrice: Double = 0.0,
    val investedAmount: Double = 0.0,
    val currentValue: Double = 0.0,
    val pnl: Double = 0.0,
    val xirrPercent: Double? = null,
    val investmentDate: String = ""
)

data class ParsedInvestmentStatement(
    val investmentType: InvestmentStatementType,
    val investorName: String = "",
    val clientOrPan: String = "",
    val statementDate: String = "", // "YYYY-MM-DD"
    val targetSnapshotId: String = "2026-09", // "YYYY-MM"
    val totalInvested: Double = 0.0,
    val totalCurrentValue: Double = 0.0,
    val totalPnl: Double = 0.0,
    val totalXirr: Double? = null,
    val rawRowCount: Int = 0,
    val holdings: List<ParsedInvestmentHolding> = emptyList()
)

data class InvestmentImportResult(
    val success: Boolean,
    val message: String,
    val importedCount: Int = 0,
    val updatedCount: Int = 0,
    val snapshotId: String = "",
    val totalInvested: Double = 0.0,
    val totalValue: Double = 0.0
)

object InvestmentStatementParser {

    /**
     * Parses an input stream from an Excel (.xlsx) or CSV/TSV file.
     */
    fun parseStream(
        context: Context,
        inputStream: InputStream,
        fileName: String? = null,
        requestedType: InvestmentStatementType = InvestmentStatementType.AUTO_DETECT
    ): ParsedInvestmentStatement {
        // Check if stream is a zip archive (.xlsx has magic bytes 0x50, 0x4B)
        val tempFile = File.createTempFile("investment_stmt_", ".tmp", context.cacheDir)
        try {
            FileOutputStream(tempFile).use { out ->
                inputStream.copyTo(out)
            }

            val isZip = tempFile.length() >= 4 && checkIsZip(tempFile)
            val rows: List<List<String>> = if (isZip) {
                parseXlsxFile(tempFile)
            } else {
                parseCsvFile(tempFile)
            }

            return parseRows(rows, requestedType)
        } finally {
            tempFile.delete()
        }
    }

    /**
     * Parses raw CSV or TSV text.
     */
    fun parseText(
        text: String,
        requestedType: InvestmentStatementType = InvestmentStatementType.AUTO_DETECT
    ): ParsedInvestmentStatement {
        val rows = parseCsvText(text)
        return parseRows(rows, requestedType)
    }

    private fun checkIsZip(file: File): Boolean {
        return try {
            file.inputStream().use { stream ->
                val b1 = stream.read()
                val b2 = stream.read()
                b1 == 0x50 && b2 == 0x4B // 'P' 'K'
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Parses standard .xlsx Excel file using ZipFile and XmlPullParser without heavy dependencies.
     */
    private fun parseXlsxFile(file: File): List<List<String>> {
        val allRows = mutableListOf<List<String>>()
        try {
            ZipFile(file).use { zip ->
                // 1. Read Shared Strings (xl/sharedStrings.xml)
                val sharedStrings = mutableListOf<String>()
                val sstEntry = zip.getEntry("xl/sharedStrings.xml")
                if (sstEntry != null) {
                    zip.getInputStream(sstEntry).use { sstStream ->
                        val factory = XmlPullParserFactory.newInstance()
                        val parser = factory.newPullParser()
                        parser.setInput(sstStream, "UTF-8")

                        var eventType = parser.eventType
                        var insideSi = false
                        var insideT = false
                        var currentSiText = StringBuilder()

                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            when (eventType) {
                                XmlPullParser.START_TAG -> {
                                    if (parser.name == "si") {
                                        insideSi = true
                                        currentSiText = StringBuilder()
                                    } else if (parser.name == "t") {
                                        insideT = true
                                    }
                                }
                                XmlPullParser.TEXT -> {
                                    if (insideSi && insideT) {
                                        currentSiText.append(parser.text)
                                    }
                                }
                                XmlPullParser.END_TAG -> {
                                    if (parser.name == "t") {
                                        insideT = false
                                    } else if (parser.name == "si") {
                                        insideSi = false
                                        sharedStrings.add(currentSiText.toString())
                                    }
                                }
                            }
                            eventType = parser.next()
                        }
                    }
                }

                // 2. Read Primary Worksheet (xl/worksheets/sheet1.xml)
                val sheetEntry = zip.getEntry("xl/worksheets/sheet1.xml")
                    ?: zip.entries().asSequence().firstOrNull { it.name.startsWith("xl/worksheets/sheet") && it.name.endsWith(".xml") }

                if (sheetEntry != null) {
                    zip.getInputStream(sheetEntry).use { sheetStream ->
                        val factory = XmlPullParserFactory.newInstance()
                        val parser = factory.newPullParser()
                        parser.setInput(sheetStream, "UTF-8")

                        var eventType = parser.eventType
                        var currentRowCells = mutableMapOf<Int, String>()
                        var currentColIndex = 0
                        var currentCellType = ""
                        var insideV = false
                        var insideIs = false
                        var insideT = false
                        var inlineText = StringBuilder()

                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            when (eventType) {
                                XmlPullParser.START_TAG -> {
                                    when (parser.name) {
                                        "row" -> {
                                            currentRowCells = mutableMapOf()
                                        }
                                        "c" -> {
                                            val rAttr = parser.getAttributeValue(null, "r") ?: ""
                                            currentColIndex = cellRefToColIndex(rAttr)
                                            currentCellType = parser.getAttributeValue(null, "t") ?: ""
                                        }
                                        "v" -> insideV = true
                                        "is" -> {
                                            insideIs = true
                                            inlineText = StringBuilder()
                                        }
                                        "t" -> if (insideIs) insideT = true
                                    }
                                }
                                XmlPullParser.TEXT -> {
                                    if (insideV) {
                                        val text = parser.text
                                        val value = if (currentCellType == "s") {
                                            val idx = text.toIntOrNull()
                                            if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else text
                                        } else {
                                            text
                                        }
                                        currentRowCells[currentColIndex] = value
                                    } else if (insideIs && insideT) {
                                        inlineText.append(parser.text)
                                    }
                                }
                                XmlPullParser.END_TAG -> {
                                    when (parser.name) {
                                        "v" -> insideV = false
                                        "t" -> if (insideIs) insideT = false
                                        "is" -> {
                                            insideIs = false
                                            currentRowCells[currentColIndex] = inlineText.toString()
                                        }
                                        "row" -> {
                                            if (currentRowCells.isNotEmpty()) {
                                                val maxCol = currentRowCells.keys.maxOrNull() ?: 0
                                                val rowList = (0..maxCol).map { currentRowCells[it] ?: "" }
                                                allRows.add(rowList)
                                            } else {
                                                allRows.add(emptyList())
                                            }
                                        }
                                    }
                                }
                            }
                            eventType = parser.next()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return allRows
    }

    private fun cellRefToColIndex(ref: String): Int {
        var colLetters = ""
        for (ch in ref) {
            if (ch.isLetter()) colLetters += ch.uppercaseChar()
            else break
        }
        if (colLetters.isEmpty()) return 0
        var col = 0
        for (ch in colLetters) {
            col = col * 26 + (ch - 'A' + 1)
        }
        return col - 1
    }

    private fun parseCsvFile(file: File): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        file.bufferedReader().use { reader ->
            reader.lineSequence().forEach { line ->
                if (line.isNotBlank()) {
                    rows.add(parseCsvRow(line))
                }
            }
        }
        return rows
    }

    private fun parseCsvText(text: String): List<List<String>> {
        return text.lines()
            .filter { it.isNotBlank() }
            .map { parseCsvRow(it) }
    }

    private fun parseCsvRow(line: String): List<String> {
        if (line.contains("\t")) {
            return line.split("\t").map { it.trim().trim('\"') }
        }
        val result = mutableListOf<String>()
        var cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    cur.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(cur.toString().trim())
                cur = StringBuilder()
            } else {
                cur.append(c)
            }
            i++
        }
        result.add(cur.toString().trim())
        return result
    }

    /**
     * Determines statement type and parses holdings.
     */
    fun parseRows(
        rows: List<List<String>>,
        requestedType: InvestmentStatementType = InvestmentStatementType.AUTO_DETECT
    ): ParsedInvestmentStatement {
        val detectedType = if (requestedType != InvestmentStatementType.AUTO_DETECT) {
            requestedType
        } else {
            detectStatementType(rows)
        }

        return when (detectedType) {
            InvestmentStatementType.STOCKS -> parseStockStatement(rows)
            else -> parseMutualFundStatement(rows)
        }
    }

    private fun detectStatementType(rows: List<List<String>>): InvestmentStatementType {
        for (row in rows.take(30)) {
            val joined = row.joinToString(" ").lowercase()
            if (joined.contains("stock name") || joined.contains("isin") || joined.contains("unrealised p&l") || joined.contains("stocks as on")) {
                return InvestmentStatementType.STOCKS
            }
            if (joined.contains("scheme name") || joined.contains("amc") || joined.contains("folio no") || joined.contains("groww mutual fund")) {
                return InvestmentStatementType.MUTUAL_FUNDS
            }
        }
        return InvestmentStatementType.MUTUAL_FUNDS
    }

    /**
     * Parses Groww / standard Mutual Fund statement.
     */
    private fun parseMutualFundStatement(rows: List<List<String>>): ParsedInvestmentStatement {
        var investorName = ""
        var panNumber = ""
        var statementDate = ""
        var totalInvested = 0.0
        var totalCurrent = 0.0
        var totalPnl = 0.0
        var totalXirr: Double? = null

        // 1. Extract Personal Details and Summary from Top Rows
        for (i in 0 until minOf(rows.size, 25)) {
            val row = rows[i]
            for (j in row.indices) {
                val cell = row[j].trim()
                if (cell.equals("Name", ignoreCase = true) && investorName.isEmpty()) {
                    investorName = row.getOrNull(j + 1)?.trim() ?: ""
                }
                if (cell.equals("PAN", ignoreCase = true) && panNumber.isEmpty()) {
                    panNumber = row.getOrNull(j + 1)?.trim() ?: ""
                }
                if (cell.contains("HOLDINGS AS ON", ignoreCase = true) || cell.contains("Holdings as on", ignoreCase = true)) {
                    val extracted = extractDateFromText(cell)
                    if (extracted.isNotBlank()) statementDate = extracted
                }
            }

            // Check summary table (Total Investments, Current Portfolio Value, etc.)
            val joinedLower = row.joinToString(" ").lowercase()
            if (joinedLower.contains("total investments") && joinedLower.contains("current portfolio value")) {
                val nextRow = rows.getOrNull(i + 1)
                if (nextRow != null && nextRow.isNotEmpty()) {
                    totalInvested = cleanNumber(nextRow.getOrNull(0))
                    totalCurrent = cleanNumber(nextRow.getOrNull(1))
                    totalPnl = cleanNumber(nextRow.getOrNull(2))
                    totalXirr = cleanPercentage(nextRow.getOrNull(4))
                }
            }
        }

        // 2. Locate Holdings Table Header
        var headerRowIndex = -1
        for (i in rows.indices) {
            val row = rows[i]
            if (row.any { it.contains("Scheme Name", ignoreCase = true) || it.equals("Scheme", ignoreCase = true) }) {
                headerRowIndex = i
                break
            }
        }

        val holdings = mutableListOf<ParsedInvestmentHolding>()

        if (headerRowIndex != -1) {
            val headerRow = rows[headerRowIndex].map { it.trim().lowercase() }
            val schemeCol = headerRow.indexOfFirst { it.contains("scheme") || it.contains("fund name") || it.contains("security") || it.contains("description") }
            val amcCol = headerRow.indexOfFirst { it.contains("amc") || it.contains("fund house") }
            val catCol = headerRow.indexOfFirst { it.equals("category") }
            val subCatCol = headerRow.indexOfFirst { it.contains("sub-category") || it.contains("sub category") || it.contains("asset class") }
            val folioCol = headerRow.indexOfFirst { it.contains("folio") || it.contains("account no") }
            val unitsCol = headerRow.indexOfFirst { it.contains("units") || it.contains("unit balance") || it.contains("qty") || it.contains("quantity") || it.contains("balance") }
            val navCol = headerRow.indexOfFirst { it.contains("nav") || it.contains("rate") || (it.contains("price") && !it.contains("buy")) }
            val investedCol = headerRow.indexOfFirst {
                it.contains("invested") || it.contains("cost value") || it.contains("purchase cost") ||
                it.contains("cost") || it.contains("buy val") || it.contains("amount invested") || it.contains("total cost")
            }
            val currentValCol = headerRow.indexOfFirst {
                it.contains("current value") || it.contains("market value") || it.contains("mkt value") ||
                it.contains("mkt. value") || it.contains("valuation") || it.contains("present value") ||
                it.contains("closing value") || it.contains("cur val") || it.contains("cur. val") ||
                it.contains("latest value") || it.contains("holding value") || it.contains("portfolio value") ||
                (it.contains("current") && !it.contains("nav") && !it.contains("price")) ||
                it == "value" || it == "amount"
            }
            val returnsCol = headerRow.indexOfFirst { it.contains("returns") || it.contains("profit") || it.contains("gain") || it.contains("p&l") || it.contains("pnl") || it.contains("unrealised") }
            val xirrCol = headerRow.indexOfFirst { it.contains("xirr") || it.contains("cagr") || it.contains("return %") }

            for (r in (headerRowIndex + 1) until rows.size) {
                val row = rows[r]
                val schemeName = row.getOrNull(schemeCol)?.trim() ?: ""
                if (schemeName.isBlank() || schemeName.equals("Total", ignoreCase = true) || schemeName.startsWith("Holdings as on", ignoreCase = true)) {
                    continue
                }

                val amc = if (amcCol >= 0) row.getOrNull(amcCol)?.trim() ?: "" else ""
                val subCategory = if (subCatCol >= 0) row.getOrNull(subCatCol)?.trim() ?: "" else ""
                val folioNo = if (folioCol >= 0) row.getOrNull(folioCol)?.trim() ?: "" else ""
                val units = if (unitsCol >= 0) cleanNumber(row.getOrNull(unitsCol)) else 0.0
                val nav = if (navCol >= 0) cleanNumber(row.getOrNull(navCol)) else 0.0
                var invested = if (investedCol >= 0) cleanNumber(row.getOrNull(investedCol)) else 0.0
                var currentVal = if (currentValCol >= 0) cleanNumber(row.getOrNull(currentValCol)) else 0.0
                val pnlVal = if (returnsCol >= 0) cleanNumber(row.getOrNull(returnsCol)) else null
                val xirr = if (xirrCol >= 0) cleanPercentage(row.getOrNull(xirrCol)) else null

                // Fallback calculations so currentVal is never 0 if information is available
                if (currentVal <= 0.0 && units > 0.0 && nav > 0.0) {
                    currentVal = units * nav
                }
                if (currentVal <= 0.0 && invested > 0.0 && pnlVal != null && pnlVal != 0.0) {
                    currentVal = invested + pnlVal
                }
                if (currentVal <= 0.0 && invested > 0.0) {
                    currentVal = invested
                }
                if (invested <= 0.0 && currentVal > 0.0 && pnlVal != null && pnlVal != 0.0) {
                    invested = currentVal - pnlVal
                }
                if (invested <= 0.0 && currentVal > 0.0) {
                    invested = currentVal
                }

                val pnl = pnlVal ?: (currentVal - invested)

                holdings.add(
                    ParsedInvestmentHolding(
                        name = schemeName,
                        categoryId = "mutual_funds",
                        subCategory = subCategory,
                        amcOrIssuer = amc,
                        identifier = folioNo,
                        units = units,
                        investedAmount = invested,
                        currentValue = currentVal,
                        pnl = pnl,
                        xirrPercent = xirr,
                        investmentDate = statementDate
                    )
                )
            }
        }

        // Compute aggregates if summary row wasn't present
        if (totalInvested <= 0.0 && holdings.isNotEmpty()) {
            totalInvested = holdings.sumOf { it.investedAmount }
            totalCurrent = holdings.sumOf { it.currentValue }
            totalPnl = holdings.sumOf { it.pnl }
        }

        val snapshotId = computeSnapshotId(statementDate)

        return ParsedInvestmentStatement(
            investmentType = InvestmentStatementType.MUTUAL_FUNDS,
            investorName = investorName,
            clientOrPan = panNumber,
            statementDate = statementDate,
            targetSnapshotId = snapshotId,
            totalInvested = totalInvested,
            totalCurrentValue = totalCurrent,
            totalPnl = totalPnl,
            totalXirr = totalXirr,
            rawRowCount = rows.size,
            holdings = holdings
        )
    }

    /**
     * Parses Groww / standard Stock & ETF holdings statement.
     */
    private fun parseStockStatement(rows: List<List<String>>): ParsedInvestmentStatement {
        var investorName = ""
        var clientCode = ""
        var statementDate = ""
        var totalInvested = 0.0
        var totalCurrent = 0.0
        var totalPnl = 0.0

        // 1. Extract Top Metadata
        for (i in 0 until minOf(rows.size, 15)) {
            val row = rows[i]
            for (j in row.indices) {
                val cell = row[j].trim()
                if (cell.equals("Name", ignoreCase = true) && investorName.isEmpty()) {
                    investorName = row.getOrNull(j + 1)?.trim() ?: ""
                }
                if (cell.contains("Unique Client Code", ignoreCase = true) && clientCode.isEmpty()) {
                    clientCode = row.getOrNull(j + 1)?.trim() ?: ""
                }
                if (cell.contains("stocks as on", ignoreCase = true) || cell.contains("as on", ignoreCase = true)) {
                    val extracted = extractDateFromText(cell)
                    if (extracted.isNotBlank()) statementDate = extracted
                }
                if (cell.equals("Invested Value", ignoreCase = true)) {
                    totalInvested = cleanNumber(row.getOrNull(j + 1))
                }
                if (cell.equals("Closing Value", ignoreCase = true)) {
                    totalCurrent = cleanNumber(row.getOrNull(j + 1))
                }
                if (cell.equals("Unrealised P&L", ignoreCase = true)) {
                    totalPnl = cleanNumber(row.getOrNull(j + 1))
                }
            }
        }

        // 2. Locate Table Header
        var headerRowIndex = -1
        for (i in rows.indices) {
            val row = rows[i]
            if (row.any { it.contains("Stock Name", ignoreCase = true) || it.contains("ISIN", ignoreCase = true) }) {
                headerRowIndex = i
                break
            }
        }

        val holdings = mutableListOf<ParsedInvestmentHolding>()

        if (headerRowIndex != -1) {
            val headerRow = rows[headerRowIndex].map { it.trim().lowercase() }
            val stockCol = headerRow.indexOfFirst { it.contains("stock") || it.contains("symbol") || it.contains("company") || it.contains("instrument") || it.contains("security") }
            val isinCol = headerRow.indexOfFirst { it.contains("isin") }
            val qtyCol = headerRow.indexOfFirst { it.contains("quantity") || it.contains("qty") || it.contains("shares") || it.contains("units") }
            val avgBuyCol = headerRow.indexOfFirst { it.contains("average buy") || it.contains("avg buy") || it.contains("avg. cost") || it.contains("buy avg") || it.contains("buy price") }
            val buyValCol = headerRow.indexOfFirst { it.contains("buy value") || it.contains("invested") || it.contains("cost value") || it.contains("total cost") || it.contains("amount invested") }
            val closingPriceCol = headerRow.indexOfFirst { it.contains("ltp") || it.contains("cmp") || it.contains("closing price") || it.contains("market price") || it.contains("last price") || it.contains("rate") }
            val closingValCol = headerRow.indexOfFirst {
                it.contains("cur. val") || it.contains("cur val") || it.contains("current value") ||
                it.contains("market value") || it.contains("mkt value") || it.contains("mkt. value") ||
                it.contains("closing value") || it.contains("present value") || it.contains("valuation") ||
                it.contains("total value") || (it.contains("current") && !it.contains("price")) ||
                it == "value" || it == "amount"
            }
            val pnlCol = headerRow.indexOfFirst { it.contains("unrealised") || it.contains("p&l") || it.contains("profit") || it.contains("gain") || it.contains("pnl") }

            for (r in (headerRowIndex + 1) until rows.size) {
                val row = rows[r]
                val stockName = row.getOrNull(stockCol)?.trim() ?: ""
                if (stockName.isBlank() || stockName.equals("Total", ignoreCase = true) || stockName.contains("statement", ignoreCase = true)) {
                    continue
                }

                val isin = if (isinCol >= 0) row.getOrNull(isinCol)?.trim() ?: "" else ""
                val qty = if (qtyCol >= 0) cleanNumber(row.getOrNull(qtyCol)) else 0.0
                val avgBuy = if (avgBuyCol >= 0) cleanNumber(row.getOrNull(avgBuyCol)) else 0.0
                var buyVal = if (buyValCol >= 0) cleanNumber(row.getOrNull(buyValCol)) else 0.0
                if (buyVal <= 0.0 && qty > 0.0 && avgBuy > 0.0) buyVal = qty * avgBuy

                val closingPrice = if (closingPriceCol >= 0) cleanNumber(row.getOrNull(closingPriceCol)) else 0.0
                var closingVal = if (closingValCol >= 0) cleanNumber(row.getOrNull(closingValCol)) else 0.0
                if (closingVal <= 0.0 && qty > 0.0 && closingPrice > 0.0) closingVal = qty * closingPrice

                val pnlVal = if (pnlCol >= 0) cleanNumber(row.getOrNull(pnlCol)) else null
                
                // Fallbacks so closingVal and buyVal are robust
                if (closingVal <= 0.0 && buyVal > 0.0 && pnlVal != null && pnlVal != 0.0) {
                    closingVal = buyVal + pnlVal
                }
                if (closingVal <= 0.0 && buyVal > 0.0) {
                    closingVal = buyVal
                }
                if (buyVal <= 0.0 && closingVal > 0.0 && pnlVal != null && pnlVal != 0.0) {
                    buyVal = closingVal - pnlVal
                }
                if (buyVal <= 0.0 && closingVal > 0.0) {
                    buyVal = closingVal
                }

                val pnl = pnlVal ?: (closingVal - buyVal)

                holdings.add(
                    ParsedInvestmentHolding(
                        name = stockName,
                        categoryId = "indian_stocks",
                        amcOrIssuer = if (stockName.contains("-")) stockName.substringBefore("-").trim() else "",
                        identifier = isin,
                        units = qty,
                        avgBuyPrice = avgBuy,
                        investedAmount = buyVal,
                        currentValue = closingVal,
                        pnl = pnl,
                        investmentDate = statementDate
                    )
                )
            }
        }

        if (totalInvested <= 0.0 && holdings.isNotEmpty()) {
            totalInvested = holdings.sumOf { it.investedAmount }
            totalCurrent = holdings.sumOf { it.currentValue }
            totalPnl = holdings.sumOf { it.pnl }
        }

        val snapshotId = computeSnapshotId(statementDate)

        return ParsedInvestmentStatement(
            investmentType = InvestmentStatementType.STOCKS,
            investorName = investorName,
            clientOrPan = clientCode,
            statementDate = statementDate,
            targetSnapshotId = snapshotId,
            totalInvested = totalInvested,
            totalCurrentValue = totalCurrent,
            totalPnl = totalPnl,
            rawRowCount = rows.size,
            holdings = holdings
        )
    }

    private fun extractDateFromText(text: String): String {
        // Match YYYY-MM-DD
        val ymd = Regex("""(\d{4})[-/](\d{1,2})[-/](\d{1,2})""").find(text)
        if (ymd != null) {
            val (y, m, d) = ymd.destructured
            return "$y-${m.padStart(2, '0')}-${d.padStart(2, '0')}"
        }
        // Match DD-MM-YYYY
        val dmy = Regex("""(\d{1,2})[-/](\d{1,2})[-/](\d{4})""").find(text)
        if (dmy != null) {
            val (d, m, y) = dmy.destructured
            return "$y-${m.padStart(2, '0')}-${d.padStart(2, '0')}"
        }
        return ""
    }

    private fun computeSnapshotId(dateStr: String): String {
        if (dateStr.length >= 7 && dateStr.contains("-")) {
            val parts = dateStr.split("-")
            if (parts.size >= 2) {
                return "${parts[0]}-${parts[1].padStart(2, '0')}"
            }
        }
        return SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    }

    fun cleanNumber(value: String?): Double {
        if (value == null) return 0.0
        val clean = value.replace(",", "").replace("₹", "").replace("$", "").trim()
        return clean.toDoubleOrNull() ?: 0.0
    }

    fun cleanPercentage(value: String?): Double? {
        if (value == null || value.equals("N/A", ignoreCase = true) || value.isBlank()) return null
        val clean = value.replace("%", "").replace(",", "").trim()
        val num = clean.toDoubleOrNull() ?: return null
        // If Excel formatted as decimal (e.g. 0.1408), convert to percent 14.08
        return if (num > 0.0 && num < 1.0 && !value.contains("%")) num * 100.0 else num
    }

    /**
     * Built-in sample data matching the user's uploaded Mutual Fund statement image.
     */
    fun getSampleMutualFundStatement(): ParsedInvestmentStatement {
        val holdings = listOf(
            ParsedInvestmentHolding("Quant ELSS Tax Saver Fund Direct Growth", "mutual_funds", "ELSS", "Quant Mutual Fund", "51013135874", 378.596, 0.0, 122993.20, 174964.28, 51971.08, 14.08, "2026-09-11"),
            ParsedInvestmentHolding("Motilal Oswal Large and Midcap Fund Direct Growth", "mutual_funds", "Large & MidCap", "Motilal Oswal Mutual Fund", "910169995342", 122.417, 0.0, 4999.76, 5094.76, 95.01, null, "2026-09-11"),
            ParsedInvestmentHolding("Quant Multi Asset Allocation Fund Direct Growth", "mutual_funds", "Multi Asset Allocation", "Quant Mutual Fund", "51012203791", 269.591, 0.0, 39997.87, 48959.67, 8961.80, 14.20, "2026-09-11"),
            ParsedInvestmentHolding("Nippon India Multi Cap Fund Direct Growth", "mutual_funds", "Multi Cap", "Nippon India Mutual Fund", "477248187591", 36.894, 0.0, 12012.62, 12179.53, 166.91, 6.09, "2026-09-11"),
            ParsedInvestmentHolding("ICICI Prudential Healthcare Fund Direct Growth", "mutual_funds", "Sectoral", "ICICI Prudential Mutual Fund", "46052403", 42.488, 0.0, 1999.91, 1977.39, -22.52, null, "2026-09-11"),
            ParsedInvestmentHolding("Motilal Oswal Midcap Fund Direct Growth", "mutual_funds", "Mid Cap", "Motilal Oswal Mutual Fund", "91037411012", 109.876, 0.0, 12269.62, 13260.43, 990.81, 11.70, "2026-09-11"),
            ParsedInvestmentHolding("Invesco India Large Cap Fund Direct Growth", "mutual_funds", "Large Cap", "Invesco Mutual Fund", "31048960472", 56.171, 0.0, 4999.78, 4900.36, -99.42, -18.90, "2026-09-11"),
            ParsedInvestmentHolding("ICICI Prudential Commodities Fund Direct Growth", "mutual_funds", "Thematic", "ICICI Prudential Mutual Fund", "16897853", 875.785, 0.0, 32998.35, 45234.30, 12235.95, 14.90, "2026-09-11"),
            ParsedInvestmentHolding("Canara Robeco Small Cap Fund Direct Growth", "mutual_funds", "Small Cap", "Canara Robeco Mutual Fund", "17752587395", 280.421, 0.0, 11880.53, 12904.97, 1024.45, 12.03, "2026-09-11"),
            ParsedInvestmentHolding("Mirae Asset Midcap Fund Direct Growth", "mutual_funds", "Mid Cap", "Mirae Asset Mutual Fund", "77756971872", 4632.709, 0.0, 128993.46, 204714.78, 75721.31, 17.92, "2026-09-11"),
            ParsedInvestmentHolding("Motilal Oswal Large and Midcap Fund Direct Growth (SIP)", "mutual_funds", "Large & MidCap", "Motilal Oswal Mutual Fund", "91037411012", 1102.142, 0.0, 39998.04, 45869.06, 5871.01, 26.91, "2026-09-11"),
            ParsedInvestmentHolding("Edelweiss Recently Listed IPO Fund Direct Growth", "mutual_funds", "Thematic", "Edelweiss Mutual Fund", "91019803433", 875.991, 0.0, 24181.04, 31605.23, 7424.18, 15.63, "2026-09-11"),
            ParsedInvestmentHolding("Mirae Asset Midcap Fund Direct Growth (Lump)", "mutual_funds", "Mid Cap", "Mirae Asset Mutual Fund", "78891043906", 223.978, 0.0, 9999.50, 9897.36, -102.13, null, "2026-09-11"),
            ParsedInvestmentHolding("ICICI Prudential BHARAT 22 FOF Direct Growth", "mutual_funds", "Thematic", "ICICI Prudential Mutual Fund", "16897853", 414.191, 0.0, 13072.38, 13734.12, 661.74, 4.27, "2026-09-11"),
            ParsedInvestmentHolding("Quant Dynamic Asset Allocation Fund Direct Growth", "mutual_funds", "Dynamic Asset Allocation", "Quant Mutual Fund", "51012203791", 723.577, 0.0, 12285.53, 12357.90, 72.37, 8.78, "2026-09-11"),
            ParsedInvestmentHolding("Nippon India Small Cap Fund Direct Growth", "mutual_funds", "Small Cap", "Nippon India Mutual Fund", "477248187591", 421.506, 0.0, 64996.50, 88281.90, 23285.40, 14.86, "2026-09-11")
        )

        return ParsedInvestmentStatement(
            investmentType = InvestmentStatementType.MUTUAL_FUNDS,
            investorName = "Prakash Choudhary",
            clientOrPan = "PAN: BBOPC6002J",
            statementDate = "2026-09-11",
            targetSnapshotId = "2026-09",
            totalInvested = 1802615.27,
            totalCurrentValue = 2124213.85,
            totalPnl = 321598.58,
            totalXirr = 13.71,
            rawRowCount = 40,
            holdings = holdings
        )
    }

    /**
     * Built-in sample data matching the user's uploaded Stock statement image.
     */
    fun getSampleStockStatement(): ParsedInvestmentStatement {
        val holdings = listOf(
            ParsedInvestmentHolding("GROWWAMC - GROWWHOSPI", "indian_stocks", "", "GROWWAMC", "INF666M01OD9", 28.0, 53.26, 1491.28, 1474.20, -17.08, null, "2026-09-10"),
            ParsedInvestmentHolding("ICICIPRAMC - ICICIBANKP", "indian_stocks", "", "ICICIPRAMC", "INF109KC18U7", 351.0, 27.86, 9778.86, 9666.54, -112.32, null, "2026-09-10"),
            ParsedInvestmentHolding("MIRAEAMC - MAFANG", "indian_stocks", "", "MIRAEAMC", "INF769K01HF4", 26.0, 164.15, 4267.90, 5868.20, 1600.30, null, "2026-09-10"),
            ParsedInvestmentHolding("MOTILAL OS NASDAQ100 ETF", "indian_stocks", "ETF", "MOTILAL OS", "INF247L01AP3", 18.0, 236.23, 4252.14, 5997.78, 1745.64, null, "2026-09-10"),
            ParsedInvestmentHolding("MOTILALAMC - MODEFENCE", "indian_stocks", "ETF", "MOTILALAMC", "INF247L01DJ0", 211.0, 92.06, 19424.66, 22707.82, 3283.16, null, "2026-09-10"),
            ParsedInvestmentHolding("NIP IND ETF HANGSENG BEES", "indian_stocks", "ETF", "NIP IND", "INF204KB19I1", 30.0, 508.11, 15243.30, 13981.80, -1261.50, null, "2026-09-10"),
            ParsedInvestmentHolding("NIP IND ETF NIFTY BEES", "indian_stocks", "ETF", "NIP IND", "INF204KB14I2", 65.0, 280.45, 18229.25, 17366.05, -863.20, null, "2026-09-10"),
            ParsedInvestmentHolding("RELIANCE INDUSTRIES LTD", "indian_stocks", "Equity", "RELIANCE", "INE002A01018", 1.0, 1314.00, 1314.00, 1274.00, -40.00, null, "2026-09-10"),
            ParsedInvestmentHolding("TATA MOTORS LIMITED", "indian_stocks", "Equity", "TATA", "INE1TAE01010", 25.0, 116.65, 2916.25, 10981.25, 8065.00, null, "2026-09-10"),
            ParsedInvestmentHolding("TATAAML-TATAGOLD", "indian_stocks", "Gold ETF", "TATAAML", "INF277KA1976", 166.0, 14.84, 2463.44, 2461.78, -1.66, null, "2026-09-10"),
            ParsedInvestmentHolding("TATAAML-TATSILV", "indian_stocks", "Silver ETF", "TATAAML", "INF277KA1984", 109.0, 22.54, 2456.86, 2473.21, 16.35, null, "2026-09-10")
        )

        return ParsedInvestmentStatement(
            investmentType = InvestmentStatementType.STOCKS,
            investorName = "Prakash Choudhary",
            clientOrPan = "Client: 2234657868",
            statementDate = "2026-09-10",
            targetSnapshotId = "2026-09",
            totalInvested = 81841.31,
            totalCurrentValue = 94252.63,
            totalPnl = 12411.32,
            rawRowCount = 25,
            holdings = holdings
        )
    }
}
