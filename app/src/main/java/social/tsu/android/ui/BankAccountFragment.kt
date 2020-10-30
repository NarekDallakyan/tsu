package social.tsu.android.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.paypal.android.sdk.payments.PayPalAuthorization
import com.paypal.android.sdk.payments.PayPalConfiguration
import com.paypal.android.sdk.payments.PayPalOAuthScopes
import com.paypal.android.sdk.payments.PayPalProfileSharingActivity
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.bank_account.*
import org.json.JSONException
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.currency
import social.tsu.android.helper.AnalyticsHelper
import social.tsu.android.network.api.HostEndpoint
import social.tsu.android.network.api.HostProvider
import social.tsu.android.network.model.Account
import social.tsu.android.network.model.UserProfile
import social.tsu.android.service.PayPalConfigService.REQUEST_CODE_PROFILE_SHARING
import social.tsu.android.ui.model.Data
import social.tsu.android.utils.createPayPalIntent
import social.tsu.android.utils.hide
import social.tsu.android.utils.show
import social.tsu.android.utils.snack
import javax.inject.Inject


private const val TAG = "BankAccountFragment"

class BankAccountFragment : Fragment() {

    @Inject
    lateinit var payPalConfiguration: PayPalConfiguration

    @Inject
    lateinit var oauthScopes: PayPalOAuthScopes

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    @Inject
    lateinit var viewModel: BankAccountViewModel

    lateinit var recyclerView: RecyclerView

    private val transactionAdapter = AccountTransactionPageAdapter()

    private lateinit var profileCover: ImageView
    private lateinit var profileAvatar: CircleImageView
    private lateinit var profileName: TextView
    private lateinit var yesterdayBalance: TextView
    private lateinit var currentBalance: TextView
    private lateinit var redeemButton: Button
    val properties = HashMap<String, Any?>()

    private var payPalVerified = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.bank_account, container, false)

        val fragmentComponent = (activity?.application as TsuApplication)
            .appComponent.fragmentComponent().create()

        fragmentComponent.inject(this)

        redeemButton = view.findViewById(R.id.bank_redeem_btn)
        profileCover = view.findViewById(R.id.bank_profile_cover)
        profileAvatar = view.findViewById(R.id.bank_profile_avatar)
        profileName = view.findViewById(R.id.bank_profile_name)
        yesterdayBalance = view.findViewById(R.id.bank_yesterday_balance)
        currentBalance = view.findViewById(R.id.bank_account_balance)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewManager = LinearLayoutManager(context)

        recyclerView = view.findViewById<RecyclerView>(R.id.bank_transactions_list).apply {
            layoutManager = viewManager
            adapter = transactionAdapter
        }

        viewModel.userProfileLiveData.observe(viewLifecycleOwner, Observer { userProfile ->
            bindUserProfile(userProfile)
        })

        if (requireActivity().isInternetAvailable()) {
            viewModel.fetchAccountBalance().observe(viewLifecycleOwner, Observer { account ->
                when (account) {
                    is Data.Success<Account> -> bindUserBalance(account.data)

                    is Data.Error -> {
                        bank_transactions_list.show()
                        progressBar.hide()
                        Log.e(TAG, "error = ${account.throwable.message}")
                        snack("There was an error: ${account.throwable.message}")
                    }

                    is Data.Loading -> {
                        bank_transactions_list.hide()
                        progressBar.show()
                    }
                }
            })
        } else
            requireActivity().internetSnack()

        redeemButton.setOnClickListener {
            if (payPalVerified) {
                findNavController().navigate(R.id.redeemFragment)
            } else {
                val intent = createPayPalIntent(payPalConfiguration, oauthScopes)
                startActivityForResult(intent, REQUEST_CODE_PROFILE_SHARING)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == REQUEST_CODE_PROFILE_SHARING) {
            if (resultCode == Activity.RESULT_OK) {
                val auth =
                    data?.getParcelableExtra<PayPalAuthorization>(
                        PayPalProfileSharingActivity.EXTRA_RESULT_AUTHORIZATION
                    )
                if (auth != null) {
                    try {
                        val obj = auth.toJSONObject()
                        if (requireActivity().isInternetAvailable()) {
                            viewModel.validateResponse(obj).observe(this, Observer { response ->
                                when (response) {
                                    is Data.Success<Any> -> {
                                        Log.d(TAG, "A-OK ${response.data}")
                                        findNavController().navigate(R.id.redeemFragment)
                                    }

                                    is Data.Error -> {
                                        Log.e(TAG, "error = ${response.throwable.message}")
                                        snack("There was an error connecting to PayPal ${response.throwable.message}")

                                    }
                                }

                            })
                        } else
                            requireActivity().internetSnack()

                        val authorizationCode = auth.authorizationCode
                        Log.i(TAG, authorizationCode)

                        Log.d(TAG, "Profile Sharing code received from PayPal")

                    } catch (e: JSONException) {
                        Log.e(TAG, "an extremely unlikely failure occurred: ", e)
                    }
                }
            }
        }
    }

    private fun bindUserBalance(account: Account) {
        bank_transactions_list.show()
        progressBar.hide()

        yesterdayBalance.text = account.royaltyYesterday.currency()
        currentBalance.text = account.pendingBalance.currency()
        payPalVerified = account.isPayPalVerified

        transactionAdapter.updateTransactions(account.transactions)
        properties["balance"] = currentBalance.text
        analyticsHelper.logEvent("bank_info_viewed", properties)
    }

    private fun bindUserProfile(userProfile: UserProfile) {
        Glide.with(this)
            .load(userProfile.coverPictureUrl)
            .into(profileCover)

        Glide.with(this)
            .load(formatAvatarUrl(userProfile.profilePictureUrl))
            .override(300)
            .into(profileAvatar)

        profileName.text = userProfile.fullName
    }

    private fun formatAvatarUrl(part: String): Uri {
        if (part.startsWith("/")) {
            return Uri.parse("${HostProvider.host(HostEndpoint.image)}${part}")
        }

        return Uri.parse(part)
    }

}



