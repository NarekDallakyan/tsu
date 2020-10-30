package social.tsu.android.network.model

data class EmailUniqueResponse (val data: EmailUnique)
data class EmailUnique (val unique: Boolean)

data class UsernameUniqueResponse (val data: UsernameUnique)
data class UsernameUnique (val unique: Boolean, val reserved: Boolean)
