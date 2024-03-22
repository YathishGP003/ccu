package a75f.io.renatus

import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.compose.SubTitle
import a75f.io.renatus.modbus.ModbusConfigView
import a75f.io.renatus.modbus.util.ModbusLevel
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

class BtuMeterFragment: Fragment() {

    companion object {
        val ID: String = BtuMeterFragment::class.java.simpleName

        fun newInstance(): BtuMeterFragment {
            val fragment = BtuMeterFragment()
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
                SubTitle(text = "Press \"Pair BTU Meter\" to proceed with pairing and configuration.")
                Spacer(modifier = Modifier.height(20.dp))
                SaveTextView("PAIR BTU METER") { btuPairOnClick() }
            }
        }
    }

    private fun btuPairOnClick() {
        showDialogFragment(
            ModbusConfigView.newInstance(
                L.generateSmartNodeAddress(),
                "SYSTEM",
                "SYSTEM",
                ProfileType.MODBUS_BTU,
                ModbusLevel.SYSTEM,
                "btu"
            ), ModbusConfigView.ID
        )
    }

    private fun showDialogFragment(dialogFragment: DialogFragment, id: String?) {
        val fragmentManager = requireActivity().supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        val prev = fragmentManager.findFragmentByTag(id)
        if (prev != null) {
            transaction.remove(prev)
        }
        transaction.addToBackStack(null)
        dialogFragment.show(transaction, id)
    }
}