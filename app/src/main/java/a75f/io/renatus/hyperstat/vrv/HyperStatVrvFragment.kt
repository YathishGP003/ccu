package a75f.io.renatus.hyperstat.vrv

import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.vrv.VrvMasterController
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.R
import a75f.io.renatus.util.CCUUiUtil
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.RxjavaUtil
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.viewModels
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable

class HyperStatVrvFragment : BaseDialogFragment() {

    lateinit var tempOffset : NumberPicker
    lateinit var humidityMinSp : Spinner
    lateinit var humidityMaxSp : Spinner
    lateinit var masterControllerSp : Spinner
    lateinit var setButton : Button
    lateinit var masterControlUnInitText : TextView


    private val disposables = CompositeDisposable()           // All of our Rx subscriptions, for easy management.
    private val viewModel: HyperStatVrvViewModel by viewModels()

    override fun getIdString(): String {
        return ID
    }

    companion object {
        const val ID = "HyperStatVrvFragment"

        @JvmStatic
        fun newInstance(meshAddress: Short, roomName: String, floorName: String): HyperStatVrvFragment {
            val args = Bundle().apply {
                putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress)
                putString(FragmentCommonBundleArgs.ARG_NAME, roomName)
                putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName)
            }

            return HyperStatVrvFragment().apply {
                arguments = args
            }
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // register with view model

        viewModel.initData(requireArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR),
                        requireArguments().getString(FragmentCommonBundleArgs.ARG_NAME),
                        requireArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME))
        return inflater.inflate(R.layout.fragment_hyperstat_vrv_config, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(requireView()) {
            tempOffset = findViewById(R.id.temperatureOffset)
            humidityMinSp = findViewById(R.id.humidityMinSp)
            humidityMaxSp = findViewById(R.id.humidityMaxSp)
            masterControllerSp = findViewById(R.id.masterControllerSp)
            setButton = findViewById(R.id.setBtn)
            masterControlUnInitText = findViewById(R.id.masterControlUnInitText)
        }

        setUpSpinners()
        setUpViewListeners()

        disposables.add(
            viewModel.viewState
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ viewState -> updateUI(viewState) },
                    { error -> handleError(error) })
        )

    }

    private fun setUpSpinners() {

        //setNumberPickerDividerColor(tempOffsetSelector)
        tempOffset.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        val tempOffsetValues = viewModel.tempOffsetSpinnerValues()
        tempOffset.displayedValues = tempOffsetValues
        tempOffset.minValue = 0
        tempOffset.maxValue = tempOffsetValues.size - 1
        tempOffset.wrapSelectorWheel = false

        val humidityVals = viewModel.humiditySpinnerValues()
        val adapterHumidity: ArrayAdapter<*> = ArrayAdapter<String?>(
            requireContext(),
            R.layout.spinner_dropdown_item,
            humidityVals
        )
        humidityMinSp.adapter = adapterHumidity
        humidityMaxSp.adapter = adapterHumidity

        setUpMasterControllerSpinner()
    }

    private fun setUpViewListeners() {
        tempOffset.setOnValueChangedListener { _, _, newVal ->
            viewModel.tempOffsetSelected(newVal)
        }
        humidityMinSp.setOnItemSelected { position -> viewModel.humidityMinSelected(position) }
        humidityMaxSp.setOnItemSelected { position -> viewModel.humidityMaxSelected(position) }
        masterControllerSp.setOnItemSelected { position -> viewModel.masterControllerModeSelected(position) }
        setButton.setOnClickListener {
            disposables.add(RxjavaUtil.executeBackgroundTaskWithDisposable(
                {
                    ProgressDialogUtils.showProgressDialog(
                        activity, "Saving VRV Profile Configuration"
                    )
                },
                {
                    viewModel.saveProfile()
                    L.saveCCUState()
                },
                {
                    ProgressDialogUtils.hideProgressDialog()
                    closeAllBaseDialogFragments()
                    requireActivity().sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                }
            ))
        }

        CCUUiUtil.setSpinnerDropDownColor(humidityMinSp, context)
        CCUUiUtil.setSpinnerDropDownColor(humidityMaxSp, context)
    }

    private fun updateUI(viewState: VrvViewState) {
        tempOffset.value = viewState.tempOffsetPosition
        humidityMinSp.setSelection(viewState.humidityMinPosition)
        humidityMaxSp.setSelection(viewState.humidityMaxPosition)
        masterControllerSp.setSelection(viewState.masterControllerMode)
        if (viewState.iduConnectionStatus == IduConnectionStatus.Connected.ordinal) {
            masterControlUnInitText.visibility = View.GONE
        }
    }

    private fun handleError(error: Throwable) {
        //showErrorDialog(error::class.java.simpleName + " : " + error.localizedMessage)
    }

    // Extension method to kotlinize and pretty up our code above
    private fun Spinner.setOnItemSelected(onItemSelected: (position: Int) -> Unit) {
        onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onItemSelected.invoke(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {  } // no implementation
        }
    }

    private fun setUpMasterControllerSpinner() {

        val coolHeatRight = viewModel.viewState.value.coolHeatRight;
        val masterControllerList : MutableList<String> = arrayListOf()

        VrvMasterController.values().forEach { mode ->
            masterControllerList.add(mode.toString())
        }

        if (coolHeatRight == 0) {
            masterControllerSp.isEnabled = false
        } else {
            CCUUiUtil.setSpinnerDropDownColor(masterControllerSp, context)
        }

        val adapter:ArrayAdapter<String> = object: ArrayAdapter<String>(
            activity,
            R.layout.spinner_zone_item,
            masterControllerList
        ){
            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view:TextView = super.getDropDownView(
                    position,
                    convertView,
                    parent
                ) as TextView

                if (!canEnableMasterControllerMode(coolHeatRight, VrvMasterController.values()[position])) {
                    view.setTextColor(Color.LTGRAY)
                }
                return view
            }

            override fun isEnabled(position: Int): Boolean {
                return canEnableMasterControllerMode(coolHeatRight, VrvMasterController.values()[position])
            }
        }

        masterControllerSp.adapter = adapter
        val curSelection = viewModel.viewState.value.masterControllerMode
        if (curSelection <= masterControllerList.size - 1) {
            masterControllerSp.setSelection(curSelection, false)
        }
    }
}