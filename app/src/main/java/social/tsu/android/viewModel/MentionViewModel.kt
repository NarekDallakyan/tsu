package social.tsu.android.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MentionViewModel : ViewModel() {

    var selected: String? = null

    private val tagValue: MutableLiveData<String> = MutableLiveData<String>()

    private val tagMainPostValue: MutableLiveData<String> = MutableLiveData<String>()


    fun setMainPostTag(value: String) {
        tagMainPostValue.value = value
    }

    fun getMainPosttag(): LiveData<String> {
        return tagMainPostValue
    }

    fun selectTag(value: String) {
        tagValue.value = value
    }

    fun getTag(): LiveData<String> {
        return tagValue
    }


}