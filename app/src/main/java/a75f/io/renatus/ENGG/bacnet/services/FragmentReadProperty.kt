package a75f.io.renatus.ENGG.bacnet.services

import a75f.io.device.bacnet.BacnetConfigConstants.EMPTY_STRING
import a75f.io.renatus.R
import a75f.io.renatus.UtilityApplication
import a75f.io.renatus.util.CCUUiUtil
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.gson.Gson


class FragmentReadProperty : Fragment(R.layout.lyt_frag_read_property) {

    private val TAG = "BacnetServicesFragment"
    private val objectTypeArray = BacNetConstants.ObjectType.values()
    private val propertyTypeArray = BacNetConstants.PropertyType.values()

    private var selectedObjectType = objectTypeArray[0].value
    private var selectedPropertyType = propertyTypeArray[0].value

    private lateinit var etDestinationIp: EditText
    private lateinit var etPort: EditText
    private lateinit var etObjectId: EditText
    private lateinit var btnReadProperty: Button
    private lateinit var etArrayindex: EditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "--onViewCreated--")

        configureObjectTypeSpinner(view)
        configurePropertySpinner(view)

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

        etArrayindex = view.findViewById(R.id.etArrayIndex)

        etPort = view.findViewById(R.id.etPort)
        etPort.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                if (etPort.text.toString().trim { it <= ' ' }.isNotEmpty()) {
                    if (etPort.text.toString() == EMPTY_STRING || !CCUUiUtil.isValidNumber(
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

        etObjectId = view.findViewById(R.id.etObjectId)
        etObjectId.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                if (etObjectId.text.toString().trim { it <= ' ' }.isNotEmpty()) {
                    etObjectId.error = null
                    if (validateEntries()) {
                        enableReadButton()
                    } else {
                        etObjectId.error = getString(R.string.txt_error_object_id)
                        disableReadButton()
                    }
                } else {
                    //etObjectId.error = null
                    etObjectId.error = getString(R.string.txt_error_object_id)
                    disableReadButton()
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
        var arrayIndex : Int? = if(etArrayindex.text.toString().isNotEmpty()){
            etArrayindex.text.toString().toInt()
        }else{
            null
        }

        val request = Gson().toJson(BacnetReadRequest(
            Destination(etDestinationIp.text.toString(), etPort.text.toString().toInt()),
            ReadRequest(
                ObjectIdentifierBacNet(selectedObjectType, etObjectId.text.toString().toInt()),
                selectedPropertyType, arrayIndex
            )
        ))
        Log.d(TAG, "this is the request-->$request")
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
            R.attr.orange_75f
        )
    }

    private fun validateEntries(): Boolean {
        if (etDestinationIp.text.toString() == EMPTY_STRING || !CCUUiUtil.isValidIPAddress(
                etDestinationIp.text.toString().trim { it <= ' ' })
        ) return false

        if (etPort.text.toString() == EMPTY_STRING || !CCUUiUtil.isValidNumber(
                etPort.text.toString().toInt(), 4069, 65535, 1
            )
        ) return false
        if (etObjectId.text.toString() == EMPTY_STRING) return false
        return true
    }

    private fun configureObjectTypeSpinner(view: View) {

        val spin = view.findViewById(R.id.sp_object_types) as Spinner
        spin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                Log.d(TAG, "selected item is ${objectTypeArray[position]}")
                selectedObjectType = objectTypeArray[position].value
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

    private fun configurePropertySpinner(view: View) {

        val spin = view.findViewById(R.id.sp_property_types) as Spinner
        spin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                Log.d(TAG, "selected item is ${propertyTypeArray[position]}")
                selectedPropertyType = propertyTypeArray[position].value
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                Log.d(TAG, "onNothingSelected")
            }

        }
        val aa: ArrayAdapter<*> = ArrayAdapter<Any?>(
            requireContext(), R.layout.spinner_item_type_1, propertyTypeArray
        )
        aa.setDropDownViewResource(R.layout.spinner_item_type_2)
        spin.adapter = aa
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "--onDestroyView--")
    }
}