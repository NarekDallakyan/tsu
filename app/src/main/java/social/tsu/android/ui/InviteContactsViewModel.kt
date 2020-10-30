package social.tsu.android.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import social.tsu.android.RxSchedulers
import social.tsu.android.TsuApplication
import social.tsu.android.network.api.ReferralApi
import social.tsu.android.rx.plusAssign
import social.tsu.android.service.Hasher
import social.tsu.android.service.handleResponse
import social.tsu.android.ui.model.Data
import javax.inject.Inject

class InviteContactsViewModel @Inject constructor(
    private val application: TsuApplication,
    private val contactsRepository: ContactRepository,
    private val schedulers: RxSchedulers,
    private val referralApi: ReferralApi
) : ViewModel() {

    private val _contacts = MutableLiveData<List<ContactRepository.ContactItem>>()

    val contacts: LiveData<List<ContactRepository.ContactItem>>
        get() = _contacts

    private val _selectedContact = MutableLiveData<ContactRepository.ContactItem>()

    val selectedContact: LiveData<ContactRepository.ContactItem>
        get() = _selectedContact

    val _invitedContacts = MutableLiveData<Data<List<String>>>()

    val invitedContacts: LiveData<Data<List<String>>>
        get() = _invitedContacts

    val compositeDisposable = CompositeDisposable()
    fun getInvitedList() {
        compositeDisposable += referralApi.getInvitedContacts()
            .subscribeOn(schedulers.io())
            .doOnSubscribe { _invitedContacts.postValue(Data.Loading()) }
            .subscribe({ response ->
                handleResponse(
                    application,
                    response,
                    onSuccess = { result ->
                        _invitedContacts.postValue(Data.Success(result.data.contacts))
                    },
                    onFailure = {
                        _invitedContacts.postValue(Data.Error(Throwable(it)))
                    }
                )

            }, {
                _invitedContacts.postValue(Data.Error(it))

            })
    }

    fun queryContacts(){

        val queryResult = contactsRepository.queryContacts()

        val grouped = queryResult.groupBy {
            Pair(it.contactId, it.displayName)
        }.map { entry ->
            val contactMethods = entry.value.flatMap { item ->
                item.contactMethods
            }
            ContactRepository.ContactItem(
                entry.key.first,
                entry.key.second,
                contactMethods)
        }.sortedBy {
            it.displayName
        }
        _contacts.postValue(grouped)

        Log.d("InviteContactsViewModel", "done ${grouped.size}")
    }

    fun showInviteOptions(contact: ContactRepository.ContactItem) {
        _selectedContact.postValue(contact)
    }

    fun hasBeenInvited(contact: ContactRepository.ContactItem): Boolean {
        val value = invitedContacts.value
        return when(value) {
            is Data.Success -> value.data.contains(hashContact(contact))
            else -> false
        }
    }

    fun hashContact(contact: ContactRepository.ContactItem): String {
        return Hasher.md5("${contact.displayName}-${contact.contactId}")
    }

    fun markInvited(contact: ContactRepository.ContactItem) {
        val hash = hashContact(contact)

        compositeDisposable+= referralApi.markContactInvited(hash)
            .subscribeOn(schedulers.io())
            .subscribe({
                Log.d("InviteContactsViewModel", "marked has invited $hash")
            }, {
                Log.e("InviteContactsViewModel", "error marked has invited $hash", it)
            })

    }

}