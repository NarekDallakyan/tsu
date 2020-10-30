package social.tsu.android.utils

import android.util.Patterns
import java.util.regex.Pattern


object LinkExtractor {

    private const val REGEX_HOST = "(\\b(?!.*((www)|(://)))\\w\\S+\\.\\w{2,3}\\b)"

    private val hostPattern : Pattern by lazy { Pattern.compile(REGEX_HOST) }

    fun extractLinks(text: String?): List<String> {
        val result = arrayListOf<String>()

        if (text != null) {
            val matcher = Patterns.WEB_URL.matcher(text)
            while (matcher.find()) {
                var link = matcher.group()
                if (!link.startsWith("http")) {
                    link = "http://$link"
                }
                result.add(link)
            }
        }
        return result
    }

    fun extractHost(text: String?): String? {
        if (text == null) return null

        val matcher = hostPattern.matcher(text)
        if (matcher.find()) {
            return matcher.group()
        }
        return null
    }

}
