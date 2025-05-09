package a75f.io.renatus.ENGG

import a75f.io.logger.CcuLog
import a75f.io.logic.service.FileBackupJobReceiver
import a75f.io.logic.util.bacnet.BacnetConfigConstants.BROADCAST_BACNET_APP_GLOBAL_PARAM
import a75f.io.renatus.ENGG.bacnet.services.FragmentReadProperty
import a75f.io.renatus.ENGG.bacnet.services.FragmentReadPropertyMultiple
import a75f.io.renatus.ENGG.bacnet.services.FragmentWhoIs
import a75f.io.renatus.ENGG.bacnet.services.FragmentWriteProperty
import a75f.io.renatus.R
import a75f.io.renatus.UtilityApplication
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Spinner
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class BacnetServicesFragment : Fragment() {

    private val IS_GLOBAL = "isGlobal"
    private val TAG = "BacnetServicesFragment"
    private val READ_PROPERTY = "Read Property"
    private val READ_PROPERTY_MULTIPLE = "Read Property Multiple"
    private val WRITE_PROPERTY = "Write Property"
    private val WHO_IS = "Who Is"
    private val propertyArray =
        arrayOf(READ_PROPERTY, READ_PROPERTY_MULTIPLE, WRITE_PROPERTY, WHO_IS)


    override fun onCreateView(
        inflator: LayoutInflater, container: ViewGroup?, saveInstanceState: Bundle?
    ): View? {
        return inflator.inflate(R.layout.lyt_bacnet_services, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureSpinner(view)

        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(UtilityApplication.context)
            val isGlobal = sharedPreferences.getBoolean(IS_GLOBAL, true)

        val checkBoxGlobal = view.findViewById<CheckBox>(R.id.cb_global)
            checkBoxGlobal.isChecked = isGlobal

        checkBoxGlobal.setOnCheckedChangeListener { _, isChecked ->
            sendBroadcastToBacApp(isChecked)
            FileBackupJobReceiver.performConfigFileBackup()
        }
    }

    private fun sendBroadcastToBacApp(isChecked: Boolean) {
        val applicationContext = UtilityApplication.context.applicationContext
        CcuLog.d(TAG, "------sendBroadcastToBacApp----$isChecked")
        val intent = Intent(BROADCAST_BACNET_APP_GLOBAL_PARAM)
        intent.putExtra(IS_GLOBAL, isChecked)
        applicationContext.sendBroadcast(intent)
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(UtilityApplication.context)
        sharedPreferences.edit().putBoolean(IS_GLOBAL, isChecked).apply()
    }

    private fun configureSpinner(view: View) {

        val spin = view.findViewById(R.id.spinner) as Spinner
        spin.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                CcuLog.d(TAG, "selected item is ${propertyArray[position]}")
                updateView(propertyArray[position])
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                CcuLog.d(TAG, "onNothingSelected")
            }

        }
        val aa: ArrayAdapter<*> = ArrayAdapter<Any?>(
            requireContext(), R.layout.spinner_item_type_2, propertyArray
        )
        aa.setDropDownViewResource(R.layout.spinner_item_type_2)
        spin.adapter = aa
    }

    private fun updateView(selectedView: String) {
        CcuLog.d(TAG, "update view -> $selectedView")
        var fragment: Fragment = FragmentReadProperty()
        when (selectedView) {
            READ_PROPERTY -> {
                fragment = FragmentReadProperty()
            }

            READ_PROPERTY_MULTIPLE -> {
                fragment = FragmentReadPropertyMultiple()
            }

            WRITE_PROPERTY -> {
                fragment = FragmentWriteProperty()
            }

            WHO_IS -> {
                fragment = FragmentWhoIs()
            }
        }

        val fragmentManager = requireActivity().supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.container_property_fragment, fragment)
        transaction.commit()
    }
}