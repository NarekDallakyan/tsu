package social.tsu.android.network.model

import com.squareup.moshi.Json

/*
        "pending_balance": 20.18,
        "royalty_yesterday": 0,
        "transactions": [
            {
                "created_at": "03/06/2020",
                "memo": "another transaction",
                "debit": 3.62,
                "status": ""
            },
            {
                "created_at": "03/06/2020",
                "memo": "test transaction",
                "credit": 1.19,
                "status": ""
            }
        ],
        "user_monetization_policy_version": 0,
        "current_monetization_policy_version": 1
    }
 */
data class Account (
    @field:Json(name = "pending_balance")
    val pendingBalance: Double,
    @field:Json(name = "royalty_yesterday")
    val royaltyYesterday: Double,
    val transactions: List<Transaction>,
    @field:Json(name = "user_monetization_policy_version")
    val userMonetizationPolicyVersion: Int,
    @field:Json(name = "current_monetization_policy_version")
    val currentMonetizationPolicyVersion: Int,
    @field:Json(name = "is_paypal_verified")
    val isPayPalVerified: Boolean
)

