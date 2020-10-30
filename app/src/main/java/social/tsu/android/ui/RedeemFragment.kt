package social.tsu.android.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import kotlinx.android.synthetic.main.fragment_redeem.*
import social.tsu.android.R
import social.tsu.android.TsuApplication
import social.tsu.android.currency
import social.tsu.android.network.model.Account
import social.tsu.android.network.model.TsuMessage
import social.tsu.android.service.SharedPrefManager
import social.tsu.android.ui.model.Data
import social.tsu.android.utils.dismissKeyboard
import social.tsu.android.utils.snack
import javax.inject.Inject

class RedeemFragment : Fragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var sharedPrefManager: SharedPrefManager

    lateinit var bankAccountViewModel: BankAccountViewModel

    private var balance: Double? = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_redeem, container, false)

        val fragmentComponent = (activity?.application as TsuApplication)
            .appComponent.fragmentComponent().create()

        fragmentComponent.inject(this)

        bankAccountViewModel =
            ViewModelProvider(this, viewModelFactory)[BankAccountViewModel::class.java]

        view.findViewById<Button>(R.id.submitRedemption).setOnClickListener {
            sendPaymentRequest(view)
        }

        handleRedemptionResponse(bankAccountViewModel.redeemResponse)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bankAccountViewModel.fetchAccountBalance().observe(viewLifecycleOwner, Observer { account ->
            when (account) {
                is Data.Success<Account> -> {
                    balance = account.data.pendingBalance
                    textViewBalance.text = account.data.pendingBalance.currency()
                }

                is Data.Error -> {
                    Log.e("accountFragment", "error = ${account.throwable.message}")
                    snack("There was an error: ${account.throwable.message}")
                }
            }
        })
    }

    fun handleRedemptionResponse(response: LiveData<Data<TsuMessage>>) {
        response.observe(viewLifecycleOwner, Observer {
            when (it) {
                is Data.Success -> {
                    snack("Sent payment request")
                    Navigation.findNavController(requireView()).popBackStack()
                }
                is Data.Error -> {
                    snack("unable to process your request")
                }
            }

        })

    }

    private fun sendPaymentRequest(myView: View) {
        dismissKeyboard()
        val myVal: Double =
            try {
                editAmount.text.toString().toDouble()
            } catch (e: NumberFormatException) {
                0.0
            }
        val myBal = balance ?: 0.0

        if (myVal >= sharedPrefManager.getMinRedeemBalanceValue() && myVal <= myBal) {
            if (requireActivity().isInternetAvailable())
                bankAccountViewModel.requestRedemption(myVal)
            else
                requireActivity().internetSnack()
        } else {
            snack("Oops! Please enter a valid amount over $${sharedPrefManager.getMinRedeemBalanceValue()}(USD).")
        }
    }

}
