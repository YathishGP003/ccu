package a75f.io.renatus.profiles.mystat.ui


import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.R
import a75f.io.renatus.modbus.util.MYSTAT_V1_DEVICE
import a75f.io.renatus.modbus.util.MYSTAT_V2_DEVICE
import a75f.io.renatus.util.CCUUiUtil.updateBackgroundWaterMaker
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.Toast
import androidx.cardview.widget.CardView

class MyStatDeviceSelectionFragment : BaseDialogFragment() {

    private var myStatDevice = ""

    private val mNodeAddress: Short
        get() = requireArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
    private val mRoomName: String
        get() = requireArguments().getString(FragmentCommonBundleArgs.ARG_NAME)!!
    private val mFloorName: String
        get() = requireArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME)!!

    companion object {
        val MYSTAT_DEVICES_SELCETION = "MyStatDevices"

        @JvmStatic
        fun newInstance(
            meshAddress: Short,
            roomName: String,
            floorName: String
        ): MyStatDeviceSelectionFragment {
            val args = Bundle()
            args.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress)
            args.putString(FragmentCommonBundleArgs.ARG_NAME, roomName)
            args.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName)
            val fragment = MyStatDeviceSelectionFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_statdevice_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var mainLayout = view.findViewById<LinearLayout>(R.id.main_layout)
        updateBackgroundWaterMaker(mainLayout)
        setupOnClickListeners(view)
    }


    @SuppressLint("ResourceAsColor")
    private fun setupOnClickListeners(view: View) {

        var myStatV1Card = view.findViewById<CardView>(R.id.cardMyStatV1)
        var myStatV2Card = view.findViewById<CardView>(R.id.cardMyStatV2)
        var linearlayoutCardV1 = view.findViewById<LinearLayout>(R.id.linearlayoutCardV1)
        var linearlayoutCardV2 = view.findViewById<LinearLayout>(R.id.linearlayoutCardV2)
        val goBack = view.findViewById<View>(R.id.btnBack)
        val nextButton = view.findViewById<View>(R.id.nextBtn)
        myStatV1Card.setOnClickListener {
            myStatDevice = MYSTAT_V1_DEVICE
            linearlayoutCardV1.setBackgroundResource(R.drawable.cardview_border)
            linearlayoutCardV2.setBackgroundResource(R.drawable.card_view_blur_border)
        }
        myStatV2Card.setOnClickListener {
            myStatDevice = MYSTAT_V2_DEVICE
            linearlayoutCardV2.setBackgroundResource(R.drawable.cardview_border)
            linearlayoutCardV1.setBackgroundResource(R.drawable.card_view_blur_border)
        }

        nextButton.setOnClickListener {

            if (myStatDevice.equals("")) {
                Toast.makeText(context, getString(R.string.error_msg_device), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val myStatProfile = MyStatProfileSelectionFragment.newInstance(
                mNodeAddress,
                mRoomName,
                mFloorName,
                myStatDevice
            )
            showDialogFragment(myStatProfile, myStatProfile.idString)
        }

        goBack.setOnClickListener { removeDialogFragment(MYSTAT_DEVICES_SELCETION) }

    }


    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(MATCH_PARENT, MATCH_PARENT)
    }

    override fun getIdString(): String {
        return MYSTAT_DEVICES_SELCETION
    }

}