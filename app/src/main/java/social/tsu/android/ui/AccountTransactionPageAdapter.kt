package social.tsu.android.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import social.tsu.android.R
import social.tsu.android.currency
import social.tsu.android.network.model.Transaction

class AccountTransactionPageAdapter :
    RecyclerView.Adapter<AccountTransactionPageAdapter.TransactionViewHolder>() {

    private val transactions = mutableListOf<Transaction>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.account_transaction, parent, false)

        return TransactionViewHolder(view)
    }

    override fun getItemCount(): Int {
        return transactions.size
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.reset()
        holder.bind(transactions[position])
    }

    fun updateTransactions(newTransactions: List<Transaction>) {
        transactions.clear()
        transactions.addAll(newTransactions)
        notifyDataSetChanged()
    }

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val transactionDate: TextView = itemView.findViewById(R.id.transactionDate)
        private val transactionMemo: TextView = itemView.findViewById(R.id.transactionMemo)
        private val transactionAmount: TextView = itemView.findViewById(R.id.transactionAmount)

        fun reset() {
            transactionDate.text = ""
            transactionMemo.text = ""
            transactionAmount.text = ""
        }

        fun bind(transaction: Transaction) {
            transactionDate.text = transaction.createdDate
            transactionMemo.text = transaction.memo
            transactionAmount.text = formatAmount(transaction)
        }

        private fun formatAmount(transaction: Transaction): String = when {
            (transaction.credit > 0) -> transaction.credit.currency()
            (transaction.debit > 0) -> "(${transaction.debit.currency()})"
            else -> "n/a"
        }
    }

}
