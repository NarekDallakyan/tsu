package social.tsu.android.viewModel.userProfile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import social.tsu.android.TsuApplication
import social.tsu.android.network.model.FamilyTreeResponse
import social.tsu.android.service.UserFamilyTreeService
import social.tsu.android.service.UserFamilyTreeServiceCallback
import social.tsu.android.utils.SingleLiveEvent
import javax.inject.Inject


class UserFamilyTreeViewModel @Inject constructor(
    private val application: TsuApplication,
    private val userFamilyTreeService: UserFamilyTreeService
) : ViewModel(), UserFamilyTreeServiceCallback {

    private val mutableFamilyTreeLiveData = MutableLiveData<FamilyTreeResponse>()
    val familyTreeLiveData: LiveData<FamilyTreeResponse> = mutableFamilyTreeLiveData
    val errorLiveData = SingleLiveEvent<String>()

    private var currentPage = 1
    private var hasNext = true

    init {
        userFamilyTreeService.callback = this
    }

    fun loadFamilyTree() {
        currentPage = 1
        hasNext = true
        userFamilyTreeService.loadUserFamilyTree(currentPage, 10)
    }

    fun loadNextFamilyTreePage() {
        if (hasNext) {
            currentPage++
            userFamilyTreeService.loadUserFamilyTree(currentPage, 10)
        }
    }

    override fun didLoadUserFamilyTree(data: FamilyTreeResponse) {
        if (data.children.size < 10) {
            hasNext = false
        }
        mutableFamilyTreeLiveData.postValue(data)
    }

    fun hasNext(): Boolean {
        return hasNext
    }

    override fun didErrorWith(message: String) {
        errorLiveData.postValue(message)
    }
}