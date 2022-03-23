package a75f.io.renatus.hyperstat.cpu

import a75f.io.api.haystack.HSUtil
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.hyperstat.vrv.HyperStatVrvFragment
import a75f.io.renatus.FragmentBLEInstructionScreen
import a75f.io.renatus.HyperStatSensePairScreen
import a75f.io.renatus.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.Toast
import a75f.io.renatus.BuildConfig
import a75f.io.renatus.util.CCUUiUtil
import androidx.core.view.isVisible


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


      val goBack = view.findViewById<View>(R.id.goBackImage)
      val cpuCell = view.findViewById<View>(R.id.cpuCell)
      val hpuCell = view.findViewById<View>(R.id.hpuCell)
      val twoPipeCell = view.findViewById<View>(R.id.twoPipeCell)
      val fourPipeCell = view.findViewById<View>(R.id.fourPipeCell)
      val vrvCell = view.findViewById<View>(R.id.vrvCell)
      val senseCell = view.findViewById<View>(R.id.hypersenseCell)

      vrvCell.isVisible = BuildConfig.BUILD_TYPE == "daikin_prod" || CCUUiUtil.isDaikinThemeEnabled(context)

      goBack.setOnClickListener { removeDialogFragment(HYPERSTAT_PROFILE_SELECTION_ID) }
      cpuCell.setOnClickListener { showCPUConfigFragment() }
      senseCell.setOnClickListener{ showSenseconfigFragment() }
      vrvCell.setOnClickListener{ showVrvConfigFragment() }
   }

   private fun showCPUConfigFragment() {
      showDialogFragment(
         FragmentBLEInstructionScreen.getInstance(
            mNodeAddress,
            mRoomName,
            mFloorName,
            ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT,
            NodeType.HYPER_STAT
         ), FragmentBLEInstructionScreen.ID
      )
   }

   private fun showSenseconfigFragment() {
      val zoneEquips   = HSUtil.getEquips(mRoomName).size;
      if (zoneEquips == 0) {
         showDialogFragment(
                 HyperStatSensePairScreen.newInstance(mNodeAddress,
                         mRoomName,
                         mFloorName,
                         ProfileType.HYPERSTAT_SENSE),
                 HyperStatSensePairScreen.ID
         )
      }else{
         Toast.makeText(context,"Please delete other profiles",Toast.LENGTH_LONG).show();
      }
   }

   private fun showVrvConfigFragment() {
      val zoneEquips   = HSUtil.getEquips(mRoomName).size;
      if (zoneEquips == 0) {
         showDialogFragment(
            FragmentBLEInstructionScreen.getInstance(
               mNodeAddress,
               mRoomName,
               mFloorName,
               ProfileType.HYPERSTAT_VRV,
               NodeType.HYPER_STAT
            ), FragmentBLEInstructionScreen.ID
         )

         /*showDialogFragment(
            HyperStatVrvFragment.newInstance(
               mNodeAddress,
               mRoomName,
               mFloorName,
            ),HyperStatVrvFragment.ID
         )*/
      }else{
         Toast.makeText(context,"Please delete other profiles",Toast.LENGTH_LONG).show();
      }
   }

}


