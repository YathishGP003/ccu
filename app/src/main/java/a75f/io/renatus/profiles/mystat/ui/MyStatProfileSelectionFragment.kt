package a75f.io.renatus.profiles.mystat.ui

import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.FragmentBLEInstructionScreen
import a75f.io.renatus.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT

/**
 * Created by Manjunath K on 15-01-2025.
 */

class MyStatProfileSelectionFragment : BaseDialogFragment() {

    private val mNodeAddress: Short
        get() = requireArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
    private val mRoomName: String
        get() = requireArguments().getString(FragmentCommonBundleArgs.ARG_NAME)!!
    private val mFloorName: String
        get() = requireArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME)!!
    private val deviceVersion : String
        get() = requireArguments().getString(FragmentCommonBundleArgs.DEVICE_VERSION)!!

    companion object {

        const val MYSTAT_SELECTION_ID = "MyStatSelection"

        @JvmStatic
        fun newInstance(
            meshAddress: Short, roomName: String, floorName: String , deviceVersion: String
        ): MyStatProfileSelectionFragment {
            val args = Bundle()
            args.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress)
            args.putString(FragmentCommonBundleArgs.ARG_NAME, roomName)
            args.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName)
            args.putString(FragmentCommonBundleArgs.DEVICE_VERSION,deviceVersion)

            val fragment = MyStatProfileSelectionFragment()
            fragment.arguments = args
            return fragment
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mystat_module_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupOnClickListeners(view)
    }


    private fun setupOnClickListeners(view: View) {

        val goBack = view.findViewById<View>(R.id.goBackImage)
        val cpuCell = view.findViewById<View>(R.id.cpuCell)
        val hpuCell = view.findViewById<View>(R.id.hpuCell)
        val twoPipeCell = view.findViewById<View>(R.id.twoPipeCell)
        val fourPipeCell = view.findViewById<View>(R.id.fourPipeCell)

        goBack.setOnClickListener { removeDialogFragment(MYSTAT_SELECTION_ID) }
        cpuCell.setOnClickListener { showFragment(ProfileType.MYSTAT_CPU) }
        hpuCell.setOnClickListener { showFragment(ProfileType.MYSTAT_HPU) }
        twoPipeCell.setOnClickListener { showFragment(ProfileType.MYSTAT_PIPE2) }
        fourPipeCell.setOnClickListener { showFragment(ProfileType.MYSTAT_PIPE4) }
    }


    private fun showFragment(profileType: ProfileType) {
        showDialogFragment(
            FragmentBLEInstructionScreen.getInstance(
                mNodeAddress, mRoomName, mFloorName, profileType, NodeType.MYSTAT,deviceVersion), FragmentBLEInstructionScreen.ID
        )
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(MATCH_PARENT, MATCH_PARENT)
    }

    override fun getIdString() = MYSTAT_SELECTION_ID

}