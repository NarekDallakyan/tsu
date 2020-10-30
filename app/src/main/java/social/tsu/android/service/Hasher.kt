package social.tsu.android.service

import java.security.MessageDigest


object Hasher {
    fun md5(toEncrypt: String): String {
        return try {
            val digest: MessageDigest = MessageDigest.getInstance("md5")
            digest.update(toEncrypt.toByteArray())
            val bytes: ByteArray = digest.digest()
            val sb = StringBuilder()
            for (i in bytes.indices) {
                sb.append(String.format("%02X", bytes[i]))
            }
            sb.toString().toLowerCase()
        } catch (exc: Exception) {
            ""
        }
    }
}
