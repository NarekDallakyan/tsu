package social.tsu.android.service

import android.net.Uri
import com.paypal.android.sdk.payments.PayPalConfiguration

object PayPalConfigService {
    const val REQUEST_CODE_PAYMENT = 1
    const val REQUEST_CODE_FUTURE_PAYMENT = 2
    const val REQUEST_CODE_PROFILE_SHARING = 3

    fun createConfiguration(
        environment: String,
        clientId: String,
        merchantName: String,
        privacyPolicyUri: Uri,
        userAgreementUri: Uri
    ): PayPalConfiguration {
        return PayPalConfiguration()
            .environment(environment)
            .clientId(clientId)
            .merchantName(merchantName)
            .merchantPrivacyPolicyUri(privacyPolicyUri)
            .merchantUserAgreementUri(userAgreementUri)
    }

}
