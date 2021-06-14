package a75f.io.renatus

import a75f.io.logger.CcuLog
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT


const val HYPERSTAT_PROFILE_SELECTION_ID = "HyperStatProfileSelection"

/**
 * @author tcase@75f.io
 * Created on 6/11/21.
 */
class HyperStatProfileSelectionFragment : BaseDialogFragment() {

   private val mNodeAddress: Short
      get() = requireArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
   private val mRoomName: String
      get() = requireArguments().getString(FragmentCommonBundleArgs.ARG_NAME)!!
   private val mFloorName: String
      get() = requireArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME)!!


   companion object {

      @JvmStatic
      fun newInstance(meshAddress: Short, roomName: String, floorName: String): HyperStatProfileSelectionFragment {
         val args = Bundle()
         args.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress)
         args.putString(FragmentCommonBundleArgs.ARG_NAME, roomName)
         args.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName)

         val fragment = HyperStatProfileSelectionFragment()
         fragment.arguments = args
         return fragment
      }
   }

   override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
      return inflater.inflate(R.layout.fragment_hypestat_module_selection, container, false)
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
      return HYPERSTAT_PROFILE_SELECTION_ID
   }

   private fun setupOnClickListeners(view: View) {

      // todo: replace with view binding after (ticket #)
      val goBack = view.findViewById<View>(R.id.goBackImage)
      val cpuCell = view.findViewById<View>(R.id.cpuCell)
      val hpuCell = view.findViewById<View>(R.id.hpuCell)
      val twoPipeCell = view.findViewById<View>(R.id.twoPipeCell)
      val fourPipeCell = view.findViewById<View>(R.id.fourPipeCell)
      val vrvCell = view.findViewById<View>(R.id.vrvCell)

      goBack.setOnClickListener { removeDialogFragment(HYPERSTAT_PROFILE_SELECTION_ID) }
      cpuCell.setOnClickListener { showCPUConfigFragment()

         // NOTE: once we are ready to pair hyperstats (and not just rely on biskit), we open a BLEInstructionScreen like from the other fragments:
         /* showDialogFragment(
               FragmentBLEInstructionScreen.getInstance(
                  mNodeAddress,
                  mRoomName,
                  mFloorName,
                  ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT,
                  NodeType.HYPER_STAT
               ), FragmentBLEInstructionScreen.ID
            ) */
      }
   }

   private fun showCPUConfigFragment() {
      CcuLog.i("CCU_", "TC: showCPUConfigFragment");
      showDialogFragment(
         HyperstatCpuFragment(), HyperstatCpuFragment.ID
      )
   }
}


