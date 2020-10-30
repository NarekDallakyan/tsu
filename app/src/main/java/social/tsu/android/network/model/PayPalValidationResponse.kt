package social.tsu.android.network.model

import com.squareup.moshi.Json
import java.util.*

data class PayPalValidationResponse(
    @field:Json(name = "id")
    val id: Long,
    @field:Json(name = "payer_id")
    val payerId: String,
    @field:Json(name = "is_paypal_verified")
    val isPaypalVerified: Boolean,
    @field:Json(name = "user_id")
    val userId: Long,
    @field:Json(name = "currency")
    val currency: Double,
    @field:Json(name = "status")
    val status: Int,
    @field:Json(name = "primary")
    val primary: Boolean,
    @field:Json(name = "unpaid_limit")
    val unpaidLimit: Double,
    @field:Json(name = "balance")
    val balance: Double,
    @field:Json(name = "pending_balance")
    val pendingBalance: Double,
    @field:Json(name = "blocked_credit")
    val blockedCredit: Boolean,
    @field:Json(name = "blocked_debit")
    val blockedDebit: Boolean,
    @field:Json(name = "tax_ref_type")
    val taxRefType: String?,
    @field:Json(name = "tax_ref")
    val taxRef: String?,
    @field:Json(name = "created_at")
    val createdAt: Date,
    @field:Json(name = "updated_at")
    val updatedAt: Date

)
