package a75f.io.renatus

import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.compose.SubTitle
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment

class OaoFragment: Fragment() {

    companion object {
        val ID: String = OaoFragment::class.java.simpleName

        fun newInstance(): OaoFragment {
            val fragment = OaoFragment()
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val rootView = ComposeView(requireContext())
        rootView.apply {
            setContent { RootView() }
            return rootView
        }
    }

    //@Preview
    @Composable
    fun RootView() {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
                SubTitle(text = "Press \"Pair OAO Profile\" to proceed with pairing and configuration.")
                Spacer(modifier = Modifier.height(20.dp))
                SaveTextView("PAIR OAO") { oaoPairOnClick() }
            }
        }
    }

    private fun oaoPairOnClick() {
        val meshAddress: Short = L.generateSmartNodeAddress()
        showDialogFragment(FragmentBLEInstructionScreen.getInstance(meshAddress, "SYSTEM", "SYSTEM", ProfileType.OAO, NodeType.SMART_NODE), FragmentBLEInstructionScreen.ID);
    }

    private fun showDialogFragment(dialogFragment: DialogFragment, id: String) {
        val ft = parentFragmentManager.beginTransaction()
        val prev = parentFragmentManager.findFragmentByTag(id)
        if (prev != null) {
            parentFragmentManager.beginTransaction().remove(prev).commitAllowingStateLoss()
        }
        ft.addToBackStack(null)
        /*
        requireActivity().registerReceiver(
            FloorPlanFragment.mPairingReceiver,
            IntentFilter(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED)
        )
         */
        // Create and show the dialog.
        dialogFragment.show(ft, id)
    }

}