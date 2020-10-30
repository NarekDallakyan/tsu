package social.tsu.android.viewModel

import androidx.lifecycle.LiveData

import androidx.lifecycle.MutableLiveData

import androidx.lifecycle.ViewModel


class SharedViewModel : ViewModel() {
    private val selected: MutableLiveData<Boolean> = MutableLiveData<Boolean>()
    fun select(value: Boolean) {
        try {
            selected.value = value
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    fun getSelected(): LiveData<Boolean> {
        return selected
    }
}