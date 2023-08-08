package a75f.io.renatus.ENGG.bacnet.services

import a75f.io.device.bacnet.BacnetConfigConstants
import a75f.io.renatus.R
import a75f.io.renatus.UtilityApplication
import a75f.io.renatus.util.CCUUiUtil
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class FragmentWhoIs : Fragment() {

    override fun onCreateView(
        inflator: LayoutInflater,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): View? {
        return inflator.inflate(R.layout.lyt_frag_who_is, container, false)
    }

    private val TAG = "BacnetServicesFragment"

    private val OBJ_ANALOG_INPUT = "Unicast"
    private val OBJ_ANALOG_OUTPUT = "Broadcast Local"
    private val OBJ_ANALOG_VALUE = "Broadcast Global"
    private val objectTypeArray =
        arrayOf(
            OBJ_ANALOG_INPUT,
            OBJ_ANALOG_OUTPUT,
            OBJ_ANALOG_VALUE,
        )

    private var selectedObjectType = objectTypeArray[0]
    private lateinit var etDestinationIp: EditText
    private lateinit var etPort: EditText

    private lateinit var btnReadProperty: Button
    private lateinit var etLowLimit: EditText
    private lateinit var etHighLimit: EditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "--onViewCreated--")
        configureObjectTypeSpinner(view)

        etLowLimit = view.findViewById(R.id.etLowLimit)
        etHighLimit = view.findViewById(R.id.etHighLimit)

        btnReadProperty = view.findViewById(R.id.btnReadProperty)
        btnReadProperty.isEnabled = false
        btnReadProperty.isClickable = false
        btnReadProperty.setTextColor(
            ContextCompat.getColor(
                UtilityApplication.context,
                R.color.tuner_group
            )
        )

        btnReadProperty.setOnClickListener {
            generateRequest()
        }

        etPort = view.findViewById(R.id.etPort)
        etPort.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                if (etPort.text.toString().trim { it <= ' ' }.isNotEmpty()) {
                    if (etPort.text.toString() == BacnetConfigConstants.EMPTY_STRING || !CCUUiUtil.isValidNumber(
                            etPort.text.toString().toInt(), 4069, 65535, 1
                        )
                    ) {
                        etPort.error = getString(R.string.txt_error_port)
                        disableReadButton()
                    } else {
                        etPort.error = null
                        if (validateEntries()) {
                            enableReadButton()
                        } else {
                            disableReadButton()
                        }
                    }
                } else {
                    etPort.error = null
                }
            }
        })

        etDestinationIp = view.findViewById(R.id.et_destination_ip)
        etDestinationIp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                if (etDestinationIp.text.toString().trim { it <= ' ' }.isNotEmpty()) {
                    if (!CCUUiUtil.isValidIPAddress(etDestinationIp.text.toString())) {
                        etDestinationIp.error = getString(R.string.error_ip_address)
                        disableReadButton()
                    } else {
                        etDestinationIp.error = null
                        if (validateEntries()) {
                            enableReadButton()
                        } else {
                            disableReadButton()
                        }
                    }
                } else {
                    etDestinationIp.error = null
                }
            }
        })

        if (validateEntries()) {
            enableReadButton()
        } else {
            disableReadButton()
        }
    }

    private fun generateRequest() {
        if (selectedObjectType == OBJ_ANALOG_INPUT) {
            val request = Gson().toJson(
                BacnetWhoIsRequest(
                    WhoIsRequest(
                        etLowLimit.text.toString().toInt(),
                        etHighLimit.text.toString().toInt()
                    ),
                    Destination(etDestinationIp.text.toString(), etPort.text.toString().toInt()),
                )
            )
            Log.d(TAG, "this is the unicast request-->$request")
        } else {
            val request = Gson().toJson(
                BacnetWhoIsRequest(
                    WhoIsRequest(
                        etLowLimit.text.toString().toInt(),
                        etHighLimit.text.toString().toInt()
                    ),
                    null,
                    BroadCast(selectedObjectType),
                )
            )
            Log.d(TAG, "this is the broadcast request-->$request")
        }

    }

    private fun disableReadButton() {
        btnReadProperty.isEnabled = false
        btnReadProperty.isClickable = false
        btnReadProperty.setTextColor(
            ContextCompat.getColor(
                UtilityApplication.context,
                R.color.tuner_group
            )
        )
    }

    private fun enableReadButton() {
        btnReadProperty.isEnabled = true
        btnReadProperty.isClickable = true
        btnReadProperty.setTextColor(
            ContextCompat.getColor(
                UtilityApplication.context,
                R.color.ctaOrange
            )
        )
    }

    private fun validateEntries(): Boolean {
        if (etDestinationIp.text.toString() == BacnetConfigConstants.EMPTY_STRING || !CCUUiUtil.isValidIPAddress(
                etDestinationIp.text.toString().trim { it <= ' ' })
        ) return false

        if (etPort.text.toString() == BacnetConfigConstants.EMPTY_STRING || !CCUUiUtil.isValidNumber(
                etPort.text.toString().toInt(), 4069, 65535, 1
            )
        ) return false
        return true
    }

    private fun configureObjectTypeSpinner(view: View) {

        val spin = view.findViewById(R.id.sp_object_types) as Spinner
        spin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                Log.d(TAG, "selected item is ${objectTypeArray[position]}")
                selectedObjectType = objectTypeArray[position]
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                Log.d(TAG, "onNothingSelected")
            }

        }
        val aa: ArrayAdapter<*> = ArrayAdapter<Any?>(
            requireContext(), R.layout.spinner_item_type_1, objectTypeArray
        )
        aa.setDropDownViewResource(R.layout.spinner_item_type_2)
        spin.adapter = aa
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "--onDestroyView--")
    }
}