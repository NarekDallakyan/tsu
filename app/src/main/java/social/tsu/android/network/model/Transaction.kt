package social.tsu.android.network.model

import com.squareup.moshi.Json

data class Transaction (
    @field:Json(name = "created_at")
    val createdDate:String,
    val memo: String,
    val debit: Double,
    val credit: Double,
    val status: String
)
