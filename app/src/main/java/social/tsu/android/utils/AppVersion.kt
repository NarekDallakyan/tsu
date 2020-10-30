package social.tsu.android.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build

object AppVersion {

    lateinit var packageInfo: PackageInfo

    fun init(context: Context) {
        packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    }

    val versionName by lazy {
        packageInfo.versionName
    }

    val versionCode by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else packageInfo.versionCode.toLong()
    }

    val versionNameCodeConcat by lazy {
        "$versionName($versionCode)"
    }

}