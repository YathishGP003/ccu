package a75f.io.renatus.hyperstatsplit

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


const val HYPERSTATSPLIT_PROFILE_SELECTION_ID = "HyperStatSplitProfileSelection"

/**
 * @author nprill@75f.io
 * Created on 7/11/23.
 */
class HyperStatSplitProfileSelectionFragment : BaseDialogFragment() {

   private val mNodeAddress: Short
      get() = requireArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
   private val mRoomName: String
      get() = requireArguments().getString(FragmentCommonBundleArgs.ARG_NAME)!!
   private val mFloorName: String
      get() = requireArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME)!!

   companion object {

      @JvmStatic
      fun newInstance(meshAddress: Short, roomName: String, floorName: String): HyperStatSplitProfileSelectionFragment {
         val args = Bundle()
         args.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress)
         args.putString(FragmentCommonBundleArgs.ARG_NAME, roomName)
         args.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName)

         val fragment = HyperStatSplitProfileSelectionFragment()
         fragment.arguments = args
         return fragment
      }
   }

   override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
      return inflater.inflate(R.layout.fragment_hyperstatsplit_module_selection, container, false)
   }

   override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      super.onViewCreated(view, savedInstanceState)
      setupOnClickListeners(view)
   }

   override fun onStart() {
      super.onStart()
      // copied from existing smartstat dialog ui logic
      dialog?.window?.setLayout(MATCH_PARENT, MATCH_PARENT)
   }


   override fun getIdString(): String {
      return HYPERSTATSPLIT_PROFILE_SELECTION_ID
   }

   private fun setupOnClickListeners(view: View) {


      val goBack = view.findViewById<View>(R.id.goBackImage)
      val cpuEconCell = view.findViewById<View>(R.id.cpuEconCell)
      val pipe4 = view.findViewById<View>(R.id.pipe4cell)

      goBack.setOnClickListener { removeDialogFragment(HYPERSTATSPLIT_PROFILE_SELECTION_ID) }
      cpuEconCell.setOnClickListener { showCPUEconConfigFragment() }
      pipe4.setOnClickListener{showPipe4EconConfigFragment()}

   }

   private fun showCPUEconConfigFragment() {
      showDialogFragment(
         FragmentBLEInstructionScreen.getInstance(
            mNodeAddress,
            mRoomName,
            mFloorName,
            ProfileType.HYPERSTATSPLIT_CPU,
            NodeType.HYPERSTATSPLIT
         ), FragmentBLEInstructionScreen.ID
      )
   }
   private fun showPipe4EconConfigFragment() {
      showDialogFragment(
         FragmentBLEInstructionScreen.getInstance(
            mNodeAddress,
            mRoomName,
            mFloorName,
            ProfileType.HYPERSTATSPLIT_4PIPE_UV,
            NodeType.HYPERSTATSPLIT
         ), FragmentBLEInstructionScreen.ID
      )
   }

}


