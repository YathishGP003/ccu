package a75f.io.sanity.ui

import a75f.io.sanity.framework.SanityCase
import a75f.io.sanity.framework.SanityManager
import a75f.io.sanity.framework.SanityResult
import a75f.io.sanity.framework.SanityRunner
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SanityViewModel : ViewModel() {
    private val _results = MutableStateFlow<List<Pair<SanityCase, SanityResult>>>(emptyList())
    val results: StateFlow<List<Pair<SanityCase, SanityResult>>> = _results

    private val sanityManager = SanityManager()
    fun runSanityCheck(runner: SanityRunner, context: Context) {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("ccu_sanity_report", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val currentTime: String = sdf.format(Date())
            editor.putString("_Report_generated_at : ", currentTime)
            val list = mutableListOf<Pair<SanityCase, SanityResult>>()
            sanityManager.runOnce(runner).collect { pair ->
                list.add(pair)
                _results.value = list.toList()
                editor.putString(pair.first.getName(), pair.second.result.name)
            }
            editor.apply()
        }
    }
}