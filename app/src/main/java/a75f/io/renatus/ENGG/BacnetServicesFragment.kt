package a75f.io.renatus.ENGG

import a75f.io.logger.CcuLog
import a75f.io.renatus.ENGG.bacnet.services.FragmentReadProperty
import a75f.io.renatus.ENGG.bacnet.services.FragmentReadPropertyMultiple
import a75f.io.renatus.ENGG.bacnet.services.FragmentWhoIs
import a75f.io.renatus.ENGG.bacnet.services.FragmentWriteProperty
import a75f.io.renatus.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class BacnetServicesFragment : Fragment() {

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