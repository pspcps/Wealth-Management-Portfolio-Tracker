package com.example.ui.screens.creditcards

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CreditCardEntity
import com.example.data.local.entity.CreditCardStatementEntity
import com.example.data.repository.CreditCardPortfolioSummary
import com.example.data.repository.CreditCardRepository
import com.example.data.repository.UserProfileRepository
import com.example.util.CreditCardStatementPdfParser
import com.example.util.ParsedStatementResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MonthStatementSummary(
    val monthYear: String,          // e.g. "Sep 2026"
    val totalSpends: Double,
    val totalDues: Double,
    val totalCashback: Double,
    val statementsCount: Int,
    val statements: List<CreditCardStatementEntity>
)

data class CreditCardsUiState(
    // Active Tab: 0 = Statement Analysis, 1 = Card Management
    val selectedTabIndex: Int = 0,

    val cards: List<CreditCardEntity> = emptyList(),
    val filteredCards: List<CreditCardEntity> = emptyList(),
    val selectedBankFilter: String = "ALL",
    val availableBanks: List<String> = emptyList(),
    val summary: CreditCardPortfolioSummary = CreditCardPortfolioSummary(),
    val selectedCard: CreditCardEntity? = null,
    val cardStatements: List<CreditCardStatementEntity> = emptyList(),
    val allStatements: List<CreditCardStatementEntity> = emptyList(),

    // Statement Analysis Tab Filters
    val analysisCardFilterId: Long? = null, // null = All Cards Aggregate, or specific card id
    val analysisMonthFilter: String = "ALL", // "ALL" or specific "YYYY-MM" / "MMM YYYY"

    // Add / Edit Card
    val isAddEditOpen: Boolean = false,
    val editingCard: CreditCardEntity? = null,

    // Upload PDF Statement Sheet (can be standalone or card-specific)
    val isUploadStatementOpen: Boolean = false,
    val uploadingCard: CreditCardEntity? = null, // null means user uploaded from Analysis Tab
    val uploadPasswordInput: String = "",
    val isPasswordAutoPicked: Boolean = false,
    val passwordSourceLabel: String = "",
    val rememberPasswordForBank: Boolean = true,
    val parsedStatementPreview: ParsedStatementResult? = null,
    val isParsingStatement: Boolean = false,

    // Pay Bill Dialog
    val isPayBillDialogOpen: Boolean = false,
    val payingCard: CreditCardEntity? = null,

    // Messages
    val messageToast: String? = null
)

class CreditCardsViewModel(application: Application) : AndroidViewModel(application) {

    private val cardRepository = CreditCardRepository.getInstance(application)
    private val profileRepository = UserProfileRepository(application)

    private val _uiState = MutableStateFlow(CreditCardsUiState())
    val uiState: StateFlow<CreditCardsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                cardRepository.allCards,
                cardRepository.cardSummary,
                cardRepository.allStatements,
                _uiState.map { it.selectedBankFilter }.distinctUntilChanged()
            ) { cards, summary, statements, filter ->
                val banks = listOf("ALL") + cards.map { it.bankName }.distinct().sorted()
                val filtered = if (filter == "ALL") cards else cards.filter { it.bankName.equals(filter, ignoreCase = true) }
                data class StreamPack(
                    val cards: List<CreditCardEntity>,
                    val filtered: List<CreditCardEntity>,
                    val banks: List<String>,
                    val statements: List<CreditCardStatementEntity>,
                    val summary: CreditCardPortfolioSummary
                )
                StreamPack(cards, filtered, banks, statements, summary)
            }.collect { pack ->
                _uiState.update { state ->
                    val updatedSelected = state.selectedCard?.let { sel ->
                        pack.cards.find { it.id == sel.id }
                    } ?: pack.cards.firstOrNull()

                    state.copy(
                        cards = pack.cards,
                        filteredCards = pack.filtered,
                        availableBanks = pack.banks,
                        summary = pack.summary,
                        selectedCard = updatedSelected,
                        allStatements = pack.statements
                    )
                }
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
    }

    fun setAnalysisCardFilter(cardId: Long?) {
        _uiState.update { it.copy(analysisCardFilterId = cardId) }
    }

    fun setAnalysisMonthFilter(month: String) {
        _uiState.update { it.copy(analysisMonthFilter = month) }
    }

    fun setBankFilter(bank: String) {
        _uiState.update { state ->
            val filtered = if (bank == "ALL") state.cards else state.cards.filter { it.bankName.equals(bank, ignoreCase = true) }
            state.copy(selectedBankFilter = bank, filteredCards = filtered)
        }
    }

    fun selectCard(card: CreditCardEntity) {
        _uiState.update { it.copy(selectedCard = card) }
        viewModelScope.launch {
            cardRepository.getStatementsForCard(card.id).collect { stmts ->
                _uiState.update { it.copy(cardStatements = stmts) }
            }
        }
    }

    fun openAddCard() {
        _uiState.update {
            it.copy(
                isAddEditOpen = true,
                editingCard = null
            )
        }
    }

    fun openEditCard(card: CreditCardEntity) {
        _uiState.update {
            it.copy(
                isAddEditOpen = true,
                editingCard = card
            )
        }
    }

    fun closeAddEdit() {
        _uiState.update {
            it.copy(
                isAddEditOpen = false,
                editingCard = null
            )
        }
    }

    fun saveCard(card: CreditCardEntity) {
        viewModelScope.launch {
            cardRepository.saveCard(card)
            _uiState.update {
                it.copy(
                    isAddEditOpen = false,
                    editingCard = null,
                    messageToast = "Saved ${card.bankName} ${card.cardName}"
                )
            }
        }
    }

    fun deleteCard(card: CreditCardEntity) {
        viewModelScope.launch {
            cardRepository.deleteCard(card)
            _uiState.update { it.copy(messageToast = "Removed ${card.cardName}") }
        }
    }

    fun openUploadStatement(card: CreditCardEntity? = null) {
        val userProfile = profileRepository.userProfile.value
        val bankName = card?.bankName ?: "ICICI Bank"
        val savedBankPwd = cardRepository.getSavedPasswordForBank(bankName)
        val cardSavedPwd = card?.savedPassword?.ifBlank { null }
        val finalSaved = cardSavedPwd ?: savedBankPwd

        val (initialPassword, isAutoPicked, label) = if (!finalSaved.isNullOrBlank()) {
            Triple(finalSaved, true, "Auto-picked saved password for $bankName")
        } else {
            val defaultPwd = CreditCardStatementPdfParser.generateDefaultPassword(
                bankName = bankName,
                profileName = userProfile.name,
                profileDob = userProfile.dateOfBirth
            )
            Triple(defaultPwd, false, "Auto-generated default for $bankName")
        }

        _uiState.update {
            it.copy(
                isUploadStatementOpen = true,
                uploadingCard = card,
                uploadPasswordInput = initialPassword,
                isPasswordAutoPicked = isAutoPicked,
                passwordSourceLabel = label,
                parsedStatementPreview = null,
                isParsingStatement = false
            )
        }
    }

    fun closeUploadStatement() {
        _uiState.update {
            it.copy(
                isUploadStatementOpen = false,
                uploadingCard = null,
                parsedStatementPreview = null,
                isParsingStatement = false
            )
        }
    }

    fun updateUploadPassword(password: String) {
        _uiState.update {
            it.copy(
                uploadPasswordInput = password,
                isPasswordAutoPicked = false,
                passwordSourceLabel = "Custom password"
            )
        }
    }

    fun toggleRememberPassword(remember: Boolean) {
        _uiState.update { it.copy(rememberPasswordForBank = remember) }
    }

    fun handleStatementFileSelected(uri: Uri) {
        val currentPwd = _uiState.value.uploadPasswordInput
        val currentCard = _uiState.value.uploadingCard
        _uiState.update { it.copy(isParsingStatement = true) }

        viewModelScope.launch {
            val parsed = CreditCardStatementPdfParser.parsePdfStream(
                context = getApplication(),
                uri = uri,
                bankName = currentCard?.bankName ?: "",
                cardLimit = currentCard?.creditLimit ?: 100000.0,
                providedPassword = currentPwd
            )

            // If we didn't have an uploadingCard preselected, try to match by detected bank and last 4 digits
            val matchedCard = currentCard ?: if (parsed != null) {
                _uiState.value.cards.find { card ->
                    (parsed.last4Digits.isNotBlank() && card.last4Digits == parsed.last4Digits) ||
                    (parsed.detectedBankName.isNotBlank() && card.bankName.equals(parsed.detectedBankName, ignoreCase = true))
                }
            } else null

            _uiState.update {
                it.copy(
                    isParsingStatement = false,
                    parsedStatementPreview = parsed,
                    uploadingCard = matchedCard
                )
            }
        }
    }

    fun confirmSaveStatement(
        statementDate: String,
        dueDate: String,
        totalDue: Double,
        minDue: Double,
        cashbackEarned: Double,
        rewardPoints: Int,
        fileName: String = "Statement.pdf"
    ) {
        val pwd = _uiState.value.uploadPasswordInput
        val preview = _uiState.value.parsedStatementPreview

        viewModelScope.launch {
            // Find or create card
            val bankName = _uiState.value.uploadingCard?.bankName
                ?: preview?.detectedBankName?.ifBlank { "Credit Card" }
                ?: "Credit Card"

            var card = _uiState.value.uploadingCard
            var wasCardAutoAdded = false

            if (card == null) {
                // Auto-create the card from statement!
                val cardName = preview?.detectedCardVariant?.ifBlank { "Credit Card" } ?: "Credit Card"
                val last4 = preview?.last4Digits?.ifBlank { "0000" } ?: "0000"
                val limit = if (preview != null && preview.creditLimit > 1000) preview.creditLimit else 100000.0
                val avail = if (preview != null && preview.availableLimit > 0) preview.availableLimit else (limit - totalDue).coerceAtLeast(0.0)

                // Compute billing day from statement date
                val billDay = try {
                    val dayPart = statementDate.split("-").lastOrNull()?.toIntOrNull() ?: 15
                    dayPart
                } catch (e: Exception) { 15 }

                val newCard = CreditCardEntity(
                    bankName = bankName,
                    cardName = cardName,
                    cardType = "Rewards",
                    last4Digits = last4,
                    creditLimit = limit,
                    availableLimit = avail,
                    totalDue = totalDue,
                    minDue = minDue,
                    dueDate = dueDate,
                    statementDate = statementDate,
                    billingCycleDay = billDay,
                    paymentDueDays = 20,
                    savedPassword = pwd,
                    cardNetwork = "VISA",
                    colorHex = "#1E40AF",
                    isActive = true,
                    notes = "Auto-added from uploaded PDF statement"
                )
                val newId = cardRepository.saveCard(newCard)
                card = newCard.copy(id = newId)
                wasCardAutoAdded = true
            }

            // 1. Save password if user requested
            if (_uiState.value.rememberPasswordForBank && pwd.isNotBlank()) {
                cardRepository.savePasswordForBank(card.bankName, pwd)
                cardRepository.saveCard(card.copy(savedPassword = pwd))
            }

            // 2. Determine actual credit limit & available limit from statement or card
            val actualLimit = if (preview != null && preview.creditLimit > 1000) preview.creditLimit else card.creditLimit
            val actualAvail = if (preview != null && preview.availableLimit > 0) preview.availableLimit else (actualLimit - totalDue).coerceAtLeast(0.0)

            // Update card credit limit & statement date & billingCycleDay if needed
            val computedBillingDay = try {
                statementDate.split("-").lastOrNull()?.toIntOrNull() ?: card.billingCycleDay
            } catch (e: Exception) { card.billingCycleDay }

            cardRepository.saveCard(
                card.copy(
                    creditLimit = actualLimit,
                    availableLimit = actualAvail,
                    totalDue = totalDue,
                    minDue = minDue,
                    dueDate = dueDate,
                    statementDate = statementDate,
                    billingCycleDay = computedBillingDay
                )
            )

            // 3. Save Statement Entity
            val stmt = CreditCardStatementEntity(
                cardId = card.id,
                statementDate = statementDate,
                dueDate = dueDate,
                totalDue = totalDue,
                minDue = minDue,
                spends = preview?.spends ?: totalDue,
                cashbackEarned = cashbackEarned,
                rewardPoints = rewardPoints,
                availableLimit = actualAvail,
                totalCreditLimit = actualLimit,
                fileName = fileName,
                isPaid = totalDue <= 0.0
            )
            cardRepository.saveStatement(stmt, updateCardInfo = true)

            val autoAddNote = if (wasCardAutoAdded) " (New card automatically added!)" else ""
            val toastMsg = if (preview?.isCreditBalance == true) {
                "Statement saved! Excess credit balance of ₹${preview.creditBalanceAmount} CR recorded (no payment required).$autoAddNote"
            } else {
                "Statement saved for ${card.cardName}! Due: ₹${Math.round(totalDue)} on $dueDate$autoAddNote"
            }

            _uiState.update {
                it.copy(
                    isUploadStatementOpen = false,
                    uploadingCard = null,
                    parsedStatementPreview = null,
                    messageToast = toastMsg
                )
            }
        }
    }

    fun openPayBill(card: CreditCardEntity) {
        _uiState.update {
            it.copy(
                isPayBillDialogOpen = true,
                payingCard = card
            )
        }
    }

    fun closePayBill() {
        _uiState.update {
            it.copy(
                isPayBillDialogOpen = false,
                payingCard = null
            )
        }
    }

    fun confirmPayBill(card: CreditCardEntity, amount: Double) {
        viewModelScope.launch {
            if (amount >= card.totalDue) {
                cardRepository.clearCardBill(card.id)
            } else {
                val newDue = (card.totalDue - amount).coerceAtLeast(0.0)
                cardRepository.saveCard(card.copy(totalDue = newDue, minDue = (newDue * 0.05).coerceAtLeast(0.0), availableLimit = card.availableLimit + amount))
            }
            _uiState.update {
                it.copy(
                    isPayBillDialogOpen = false,
                    payingCard = null,
                    messageToast = "Cleared ₹${Math.round(amount)} payment for ${card.cardName}"
                )
            }
        }
    }

    fun deleteStatement(statement: CreditCardStatementEntity) {
        viewModelScope.launch {
            cardRepository.deleteStatement(statement)
            _uiState.update { it.copy(messageToast = "Statement deleted successfully") }
        }
    }

    fun dismissToast() {
        _uiState.update { it.copy(messageToast = null) }
    }
}
