package social.tsu.android.service

import android.content.SharedPreferences
import javax.inject.Inject

class SharedPrefManager @Inject constructor(private val sharedPreferences: SharedPreferences) {

    companion object {
        private const val LAST_FETCH_KEY = "LASTFETCH"
        private const val MIN_REDEEM_BALANCE_KEY = "MIN_REDEEM_BALANCE"
        private const val DEFAULT_MIN_BALANCE_REDEEM = 15f

        private const val MAX_TSUPPORTRS_PER_DAY = "MAX_TSUPPORTRS_PER_DAY"
        private const val DEFAULT_MAX_TSUPPORTRS_PER_DAY = 99

        private const val TSUPPORTRS_COUNTER = "TSUPPORTRS_COUNTER"

        private const val LAST_TSUPPORT_DATE = "LAST_TSUPPORT_DATE"
        private const val LAUNCH_TIME = "launchPostTime"
        private const val EXCLUSIVE_POST_TIME = "exclusivePostTime"
        private const val SUPPORT_POST_ID = "supportPostId"
        private const val USER_AGE = "USER_AGE"
        private const val USER_ID ="USER_ID"
        private const val ACCOUNT_CREATION_DATE = "ACCOUNT_CREATION_DATE"

        private const val MAIN_FEED_TYPE = "MAIN_FEED_TYPE"
        const val MAIN_FEED_TYPE_CHRONO = "chrono"
        const val MAIN_FEED_TYPE_TREND = "trend"
    }

    fun setLastFetch(value: Long) {
        sharedPreferences.edit().apply {
            putLong(LAST_FETCH_KEY, value)
        }.apply()
    }

    fun getLastFetch(): Long {
        return sharedPreferences.getLong(LAST_FETCH_KEY, System.currentTimeMillis())
    }

    fun getMinRedeemBalanceValue(): Float {
        return sharedPreferences.getFloat(MIN_REDEEM_BALANCE_KEY, DEFAULT_MIN_BALANCE_REDEEM)
    }

    fun setMinRedeemBalanceValue(minRedeemValue: Float) {
        sharedPreferences.edit().putFloat(MIN_REDEEM_BALANCE_KEY, minRedeemValue).apply()
    }

    fun getMaxTsupportersPerDayValue(): Int {
        return sharedPreferences.getInt(MAX_TSUPPORTRS_PER_DAY, DEFAULT_MAX_TSUPPORTRS_PER_DAY)
    }

    fun setMaxTsupportersPerDayValue(maxTsupportersPerDay: Int) {
        sharedPreferences.edit().putInt(MAX_TSUPPORTRS_PER_DAY, maxTsupportersPerDay).apply()
    }

    fun getTsupportsCounterValue(): Int {
        return sharedPreferences.getInt(TSUPPORTRS_COUNTER, 0)
    }

    fun setTsupportsCounterValue(tsupportsCounter: Int) {
        sharedPreferences.edit().putInt(TSUPPORTRS_COUNTER, tsupportsCounter).apply()
    }

    fun getLastTsupportDateValue(): String {
        return sharedPreferences.getString(LAST_TSUPPORT_DATE, "") ?: ""
    }

    fun setLastTsupportDateValue(date: String) {
        sharedPreferences.edit().putString(LAST_TSUPPORT_DATE, date).apply()
    }

    fun isTutorialCompleted(tutorialKey: String): Boolean {
        return sharedPreferences.getBoolean("tutorial_${tutorialKey}", false)
    }

    fun setTutorialCompleted(tutorialKey: String) {
        sharedPreferences.edit().putBoolean("tutorial_${tutorialKey}", true).apply()
    }

    fun setLaunchTime(launchTime: String) {
        sharedPreferences.edit().putString(LAUNCH_TIME, launchTime).apply()
    }

    fun getLaunchTime(): String? {
        return sharedPreferences.getString(LAUNCH_TIME,"")
    }

    fun setExclusivePostTime(exclusivePostTime: String) {
        sharedPreferences.edit().putString(EXCLUSIVE_POST_TIME, exclusivePostTime).apply()
    }

    fun getExclusivePostTime(): String? {
        return sharedPreferences.getString(EXCLUSIVE_POST_TIME,"")
    }

    fun setSupportPostId(supportedPostId: String) {
        sharedPreferences.edit().putString(SUPPORT_POST_ID, supportedPostId).apply()
    }

    fun getSupportPostId(): String? {
        return sharedPreferences.getString(SUPPORT_POST_ID, "")
    }

    fun getAge(): Int {
        return sharedPreferences.getInt(USER_AGE, 0)
    }

    fun setAge(userAge: Int) {
        sharedPreferences.edit().putInt(USER_AGE, userAge).apply()
    }

    fun getUserId(): Int {
        return sharedPreferences.getInt(USER_ID, 0)
    }

    fun setUserId(userId: Int) {
        sharedPreferences.edit().putInt(USER_ID, userId).apply()
    }

    fun getCreatedAt(): Int {
        return sharedPreferences.getInt(ACCOUNT_CREATION_DATE, 0)
    }

    fun setCreatedAt(createdAt: Int) {
        sharedPreferences.edit().putInt(ACCOUNT_CREATION_DATE, createdAt).apply()
    }

    fun setFeedType(feedType: String) {
        sharedPreferences.edit().putString(MAIN_FEED_TYPE, feedType).apply()
    }

    fun getFeedType(): String? {
        return sharedPreferences.getString(MAIN_FEED_TYPE, MAIN_FEED_TYPE_CHRONO)
    }

}
