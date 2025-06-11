package a75f.io.sanity.ui

import a75f.io.sanity.framework.SanityCase
import a75f.io.sanity.framework.SanityManager
import a75f.io.sanity.framework.SanityResult
import a75f.io.sanity.framework.SanityRunner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SanityViewModel : ViewModel() {
    private val _results = MutableStateFlow<List<Pair<SanityCase, SanityResult>>>(emptyList())
    val results: StateFlow<List<Pair<SanityCase, SanityResult>>> = _results

    private val sanityManager = SanityManager()
    fun runSanityCheck(runner: SanityRunner) {
        viewModelScope.launch {
            val list = mutableListOf<Pair<SanityCase, SanityResult>>()
            sanityManager.runOnce(runner).collect { pair ->
                list.add(pair)
                _results.value = list.toList()
            }
        }
    }
}