package com.gamearena.booster.panels.wallet

import com.gamearena.booster.model.Transaction
import com.gamearena.booster.model.TransactionStatus
import com.gamearena.booster.model.TransactionType
import com.gamearena.booster.model.WalletInfo
import com.gamearena.booster.network.GameArenaApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletManager @Inject constructor(
    private val api: GameArenaApi
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _walletInfo = MutableStateFlow(
        WalletInfo(balance = 0.0, pendingDeposits = 0.0, pendingWithdrawals = 0.0,
            pendingPrizes = 0.0, totalEarned = 0.0, totalSpent = 0.0)
    )
    val walletInfo: StateFlow<WalletInfo> = _walletInfo.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun refreshWallet() {
        scope.launch {
            _isLoading.value = true
            try {
                val walletRes = api.getWallet()
                if (walletRes.isSuccessful) {
                    val w = walletRes.body()!!
                    _walletInfo.value = WalletInfo(
                        balance = w.balance,
                        pendingDeposits = 0.0,
                        pendingWithdrawals = 0.0,
                        pendingPrizes = 0.0,
                        totalEarned = w.total_earned,
                        totalSpent = w.total_spent,
                        currency = w.currency
                    )
                }
                val txRes = api.getTransactions()
                if (txRes.isSuccessful) {
                    _transactions.value = txRes.body()?.map { it.toDomain() } ?: emptyList()
                }
            } catch (e: Exception) { } finally {
                _isLoading.value = false
            }
        }
    }

    fun depositMpesa(amount: Double, phoneNumber: String, onResult: (Boolean, String) -> Unit) {
        scope.launch {
            _isLoading.value = true
            try {
                val res = api.mpesaDeposit(mapOf("amount" to amount, "phone_number" to phoneNumber))
                if (res.isSuccessful) {
                    onResult(true, "STK push sent to $phoneNumber")
                    refreshWallet()
                } else {
                    val err = res.errorBody()?.string() ?: "Deposit failed"
                    onResult(false, try { org.json.JSONObject(err).optString("error", err) } catch (e: Exception) { err })
                }
            } catch (e: Exception) {
                onResult(false, "Network error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun depositStripe(amount: Double, onResult: (Boolean, String, String?) -> Unit) {
        scope.launch {
            _isLoading.value = true
            try {
                val res = api.stripeDeposit(mapOf("amount" to amount, "currency" to "kes"))
                if (res.isSuccessful) {
                    val body = res.body()!!
                    onResult(true, "Payment ready", body["clientSecret"] as? String)
                } else {
                    val err = res.errorBody()?.string() ?: "Deposit failed"
                    onResult(false, try { org.json.JSONObject(err).optString("error", err) } catch (e: Exception) { err }, null)
                }
            } catch (e: Exception) {
                onResult(false, "Network error: ${e.message}", null)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun depositPaypal(amount: Double, onResult: (Boolean, String, String?) -> Unit) {
        scope.launch {
            _isLoading.value = true
            try {
                val res = api.paypalDeposit(mapOf("amount" to amount.toString(), "currency" to "USD"))
                if (res.isSuccessful) {
                    val body = res.body()!!
                    onResult(true, "Order created", body["orderId"] as? String)
                } else {
                    val err = res.errorBody()?.string() ?: "Deposit failed"
                    onResult(false, try { org.json.JSONObject(err).optString("error", err) } catch (e: Exception) { err }, null)
                }
            } catch (e: Exception) {
                onResult(false, "Network error: ${e.message}", null)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun formatAmount(amount: Double, currency: String = "KES"): String {
        return when (currency) {
            "KES" -> "KES ${String.format("%,.2f", amount)}"
            "USD" -> "$${String.format("%,.2f", amount)}"
            else -> "$currency ${String.format("%,.2f", amount)}"
        }
    }

    private fun com.gamearena.booster.network.ApiTransaction.toDomain() = Transaction(
        id = id,
        type = when (type) {
            "deposit" -> TransactionType.DEPOSIT
            "withdrawal" -> TransactionType.WITHDRAWAL
            "tournament_entry" -> TransactionType.TOURNAMENT_ENTRY
            "tournament_prize" -> TransactionType.PRIZE_PAYOUT
            "challenge_entry" -> TransactionType.TOURNAMENT_ENTRY
            "challenge_prize" -> TransactionType.PRIZE_PAYOUT
            "refund" -> TransactionType.REFUND
            "bonus" -> TransactionType.BONUS
            "referral" -> TransactionType.REFERRAL
            else -> TransactionType.DEPOSIT
        },
        amount = amount,
        currency = currency,
        status = when (status) {
            "completed" -> TransactionStatus.COMPLETED
            "pending" -> TransactionStatus.PENDING
            "failed" -> TransactionStatus.FAILED
            "cancelled" -> TransactionStatus.CANCELLED
            else -> TransactionStatus.PENDING
        },
        description = description ?: "",
        timestamp = System.currentTimeMillis(),
        reference = payment_gateway
    )
}
