package a75f.io.renatus.profiles.hyperstatv2.ui

import a75f.io.renatus.profiles.hyperstatv2.viewmodels.Pipe2V2ViewModel
import androidx.fragment.app.viewModels

/**
 * Created by Manjunath K on 26-09-2024.
 */

class HyperStatV2Pipe2Fragment : HyperStatFragmentV2(){
    override val viewModel: Pipe2V2ViewModel by viewModels()

    companion object {
        const val ID = "HyperStatFragmentPipe2"
    }

    override fun getIdString() =  ID
    override fun onPairingComplete() {
        TODO("Not yet implemented")
    }
}