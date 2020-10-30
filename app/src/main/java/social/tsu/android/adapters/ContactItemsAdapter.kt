package social.tsu.android.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import social.tsu.android.BR
import social.tsu.android.ui.ContactRepository
import social.tsu.android.ui.InviteContactsViewModel

class ContactItemsAdapter(
    @LayoutRes val layoutId: Int,
    private val viewModel: InviteContactsViewModel
): RecyclerView.Adapter<ContactItemsAdapter.ContactItemViewHolder>() {
    private var contacts = mutableListOf<ContactRepository.ContactItem>()

    class ContactItemViewHolder(
        private val binding: ViewDataBinding,
        private val viewModel: InviteContactsViewModel
    ) : RecyclerView.ViewHolder(binding.root){

        fun bind(contact: ContactRepository.ContactItem) {
            binding.setVariable(BR.contact, contact)
            binding.setVariable(BR.viewModel, viewModel)
            contact.contactMethods.forEach {
                when(it) {
                    is ContactRepository.ContactMethod.Email ->{
                        binding.setVariable(BR.email, it.emailAddress)
                    }
                }
            }
            binding.executePendingBindings()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return layoutId
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)

        val binding = DataBindingUtil.inflate<ViewDataBinding>(layoutInflater,
            viewType, parent, false)

        return ContactItemViewHolder(binding, viewModel)
    }

    override fun getItemCount(): Int = contacts.size

    override fun onBindViewHolder(holder: ContactItemViewHolder, position: Int) {
        val contact = contacts[position]

        holder.bind(contact)
    }

    fun updateContacts(newContacts: List<ContactRepository.ContactItem>) {
        contacts.clear()
        contacts.addAll(newContacts)
    }
}

