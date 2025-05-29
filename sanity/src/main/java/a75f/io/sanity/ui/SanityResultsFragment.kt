package a75f.io.sanity.ui

import a75f.io.sanity.framework.SanityRunner
import a75f.io.sanity.R
import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope

class SanityResultsFragment : DialogFragment() {

    private val viewModel: SanityViewModel by viewModels()


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.fragment_sanity_results, null)

        val listView: ListView = view.findViewById(R.id.sanityListView)
        val adapter = SanityResultAdapter(requireContext(), mutableListOf())
        listView.adapter = adapter

        val runner = SanityRunner()
        viewModel.runSanityCheck(runner)

        lifecycleScope.launchWhenStarted {
            viewModel.results.collect { pairList ->
                val resultItems = pairList.map { (sanityCase, sanityResult) ->
                    SanityResultViewItem(
                        testName = sanityCase.getName(),
                        description = sanityCase.getDescription(),
                        result = sanityResult.result
                    )
                }

                adapter.clear()
                adapter.addAll(resultItems)
                adapter.notifyDataSetChanged()
            }
        }

        builder.setView(view)
            .setTitle("Sanity Check Results")
            .setPositiveButton("Close") { _, _ -> dismiss() }

        return builder.create()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.60).toInt(),
            (resources.displayMetrics.heightPixels * 0.60).toInt()
        )
    }
}