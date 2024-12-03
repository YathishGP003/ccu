package a75f.io.renatus.profiles.hyperstatv2.ui

import a75f.io.renatus.profiles.hyperstatv2.viewmodels.CpuV2ViewModel
import a75f.io.renatus.profiles.hyperstatv2.viewmodels.HpuV2ViewModel
import androidx.fragment.app.viewModels

/**
 * Created by Manjunath K on 26-09-2024.
 */

class HyperStatV2HpuFragment : HyperStatFragmentV2(){
    override val viewModel: HpuV2ViewModel by viewModels()
    companion object {
        const val ID = "HyperStatFragmentHpu"
    }

    override fun getIdString() =  ID
    override fun onPairingComplete() {
        TODO("Not yet implemented")
    }
}