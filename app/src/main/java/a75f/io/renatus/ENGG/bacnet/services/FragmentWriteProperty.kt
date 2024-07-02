package a75f.io.renatus.ENGG.bacnet.services

import a75f.io.device.bacnet.BacnetConfigConstants
import a75f.io.logger.CcuLog
import a75f.io.renatus.ENGG.bacnet.services.client.BaseResponse
import a75f.io.renatus.ENGG.bacnet.services.client.CcuService
import a75f.io.renatus.ENGG.bacnet.services.client.ServiceManager
import a75f.io.renatus.R
import a75f.io.renatus.UtilityApplication
import a75f.io.renatus.util.CCUUiUtil
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException


class FragmentWriteProperty : Fragment() {

    private val TAG = "BacnetServicesFragment"


    override fun onCreateView(
        inflator: LayoutInflater,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): View? {
        return inflator.inflate(R.layout.lyt_frag_write_property, container, false)
    }

    private val objectTypeArray = BacNetConstants.ObjectType.values()
    private val propertyTypeArray = BacNetConstants.PropertyType.values()
    private val dataTypeArray = BacNetConstants.DataTypes.values()

    private lateinit var etPriority: EditText
    private lateinit var etValue: EditText
    private lateinit var etDestinationIp: EditText
    private lateinit var etPort: EditText
    private lateinit var etObjectId: EditText
    private lateinit var btnReadProperty: Button
    private lateinit var etArrayindex: EditText

    private lateinit var etMacAddress: EditText
    private lateinit var etDnet: EditText

    private var selectedObjectType = objectTypeArray[0].value
    private var selectedPropertyType = propertyTypeArray[0].value
    private var selectedDataType = BacNetConstants.DataTypes.valueOf(dataTypeArray[0].name).ordinal

    private lateinit var service : CcuService

    private var ipAddress = "192.168.1.1"
    private var port = 47808
    private var deviceId = "1000"


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CcuLog.d(TAG, "--onViewCreated--")

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val confString: String? = sharedPreferences.getString(BacnetConfigConstants.BACNET_CONFIGURATION, null)
        if (confString != null) {
            try {
                val config = JSONObject(confString)
                val networkObject = config.getJSONObject("network")
                ipAddress = networkObject.getString(BacnetConfigConstants.IP_ADDRESS)
                port = networkObject.getInt(BacnetConfigConstants.PORT)
                service = ServiceManager.CcuServiceFactory.makeCcuService(ipAddress)
                val deviceObject = config.getJSONObject("device")
                deviceId = deviceObject.getString(BacnetConfigConstants.IP_DEVICE_INSTANCE_NUMBER)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        configureObjectTypeSpinner(view)
        configurePropertySpinner(view)
        configureDataTypeSpinner(view)

        etArrayindex = view.findViewById(R.id.etArrayIndex)

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

        etMacAddress = view.findViewById(R.id.et_destination_mac)
        /*etMacAddress.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                if (etMacAddress.text.toString().trim { it <= ' ' }.isNotEmpty()) {
                    if (!CCUUiUtil.isValidMacAddress(etMacAddress.text.toString())) {
                        etMacAddress.error = getString(R.string.txt_valid_input)
                        disableReadButton()
                    } else {
                        etMacAddress.error = null
                        if (validateEntries()) {
                            enableReadButton()
                        } else {
                            disableReadButton()
                        }
                    }
                } else {
                    //etMacAddress.error = null
                    etMacAddress.error = getString(R.string.txt_valid_input)
                    disableReadButton()
                }
            }
        })*/
        etDnet = view.findViewById(R.id.etDnet)
        /*etDnet.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                if (etDnet.text.toString().trim { it <= ' ' }.isNotEmpty()) {
                    if (etDnet.text.toString() == BacnetConfigConstants.EMPTY_STRING || !CCUUiUtil.isValidNumber(
                            etDnet.text.toString().toInt(), 0, Int.MAX_VALUE, 1
                        )
                    ) {
                        etDnet.error = getString(R.string.txt_valid_number)
                        disableReadButton()
                    } else {
                        etDnet.error = null
                        if (validateEntries()) {
                            enableReadButton()
                        } else {
                            disableReadButton()
                        }
                    }
                } else {
                    etDnet.error = getString(R.string.txt_valid_number)
                    disableReadButton()
                }
            }
        })*/

        etPriority = view.findViewById(R.id.etPriority)
        /*etPriority.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                if (etPriority.text.toString().trim { it <= ' ' }.isNotEmpty()) {
                    if (etPriority.text.toString() == BacnetConfigConstants.EMPTY_STRING || !CCUUiUtil.isValidNumber(
                            etPriority.text.toString().toInt(), 0, Int.MAX_VALUE, 1
                        )
                    ) {
                        etPriority.error = getString(R.string.txt_valid_number)
                        disableReadButton()
                    } else {
                        etPriority.error = null
                        if (validateEntries()) {
                            enableReadButton()
                        } else {
                            disableReadButton()
                        }
                    }
                } else {
                    //etPriority.error = null
                    etPriority.error = getString(R.string.txt_valid_number)
                    disableReadButton()
                }
            }
        })*/


        etValue = view.findViewById(R.id.etValue)
        /*etValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                if (etValue.text.toString().trim { it <= ' ' }.isNotEmpty()) {
                    if (etValue.text.toString() == BacnetConfigConstants.EMPTY_STRING
                    ) {
                        etValue.error = getString(R.string.txt_valid_input)
                        disableReadButton()
                    } else {
                        etValue.error = null
                        if (validateEntries()) {
                            enableReadButton()
                        } else {
                            disableReadButton()
                        }
                    }
                } else {
                    //etValue.error = null
                    etValue.error = getString(R.string.txt_valid_input)
                    disableReadButton()
                }
            }
        })*/

        etPort = view.findViewById(R.id.etPort)
        etPort.setText(port.toString())
        /*etPort.addTextChangedListener(object : TextWatcher {
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
                    //etPort.error = null
                    etPort.error = getString(R.string.txt_error_port)
                    disableReadButton()
                }
            }
        })*/

        etObjectId = view.findViewById(R.id.etObjectId)
        /*etObjectId.addTextChangedListener(object : TextWatcher {
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
                    etObjectId.error = getString(R.string.txt_error_object_id)
                    disableReadButton()
                }
            }
        })*/

        etDestinationIp = view.findViewById(R.id.et_destination_ip)
        etDestinationIp.setText(ipAddress)
        /*etDestinationIp.addTextChangedListener(object : TextWatcher {
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
        })*/
        enableReadButton()
        /*if (validateEntries()) {
            enableReadButton()
        } else {
            disableReadButton()
        }*/
    }

    private fun generateRequest() {
        var arrayIndex: Int? = if (etArrayindex.text.toString().isNotEmpty()) {
            etArrayindex.text.toString().toInt()
        } else {
            null
        }
        // increasing index, bcoz index in bacnet starts from 1 not 0
        selectedDataType
        val dataTypeCompatibleWithBacNet = selectedDataType + 1
        val bacnetWriteRequest = BacnetWriteRequest(
            DestinationMultiRead(etDestinationIp.text.toString(), etPort.text.toString(), deviceId, etDnet.text.toString(), etMacAddress.text.toString()),
            WriteRequest(
                ObjectIdentifierBacNet(
                    selectedObjectType,
                    etObjectId.text.toString()
                ),
                PropertyValueBacNet(dataTypeCompatibleWithBacNet, etValue.text.toString()),
                etPriority.text.toString(),
                selectedPropertyType,
                arrayIndex
            )
        )
        val request = Gson().toJson(
            bacnetWriteRequest
        )
        sendRequest(bacnetWriteRequest)
        CcuLog.d(TAG, "this is the request-->$request")
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
        if (etMacAddress.text.toString() == BacnetConfigConstants.EMPTY_STRING || !CCUUiUtil.isValidMacAddress(
                etMacAddress.text.toString().trim { it <= ' ' })
        ) return false
        if (etDnet.text.toString() == BacnetConfigConstants.EMPTY_STRING || !CCUUiUtil.isValidNumber(
                etDnet.text.toString().toInt(), 0, Int.MAX_VALUE, 1
            )
        ) return false
        if (etDestinationIp.text.toString() == BacnetConfigConstants.EMPTY_STRING || !CCUUiUtil.isValidIPAddress(
                etDestinationIp.text.toString().trim { it <= ' ' })
        ) return false

        if (etPort.text.toString() == BacnetConfigConstants.EMPTY_STRING || !CCUUiUtil.isValidNumber(
                etPort.text.toString().toInt(), 4069, 65535, 1
            )
        ) return false
        if (etObjectId.text.toString() == BacnetConfigConstants.EMPTY_STRING) return false
        if (etPriority.text.toString() == BacnetConfigConstants.EMPTY_STRING) return false
        if (etValue.text.toString() == BacnetConfigConstants.EMPTY_STRING) return false
        return true
    }

    private fun configureObjectTypeSpinner(view: View) {

        val spin = view.findViewById(R.id.sp_object_types) as Spinner
        spin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                CcuLog.d(TAG, "selected item is ${objectTypeArray[position]}")
                selectedObjectType = objectTypeArray[position].value
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                CcuLog.d(TAG, "onNothingSelected")
            }

        }
        val aa: ArrayAdapter<*> = ArrayAdapter<Any?>(
            requireContext(), R.layout.spinner_item_type_1, objectTypeArray
        )
        aa.setDropDownViewResource(R.layout.spinner_item_type_2)
        spin.adapter = aa
    }

    private fun configureDataTypeSpinner(view: View) {

        val spin = view.findViewById(R.id.sp_data_types) as Spinner
        spin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                CcuLog.d(TAG, "selected item is ${dataTypeArray[position]}")
                selectedDataType = BacNetConstants.DataTypes.valueOf(dataTypeArray[position].name).ordinal
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                CcuLog.d(TAG, "onNothingSelected")
            }

        }
        val aa: ArrayAdapter<*> = ArrayAdapter<Any?>(
            requireContext(), R.layout.spinner_item_type_1, dataTypeArray
        )
        aa.setDropDownViewResource(R.layout.spinner_item_type_2)
        spin.adapter = aa
    }

    private fun configurePropertySpinner(view: View) {

        val spin = view.findViewById(R.id.sp_property_types) as Spinner
        spin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                CcuLog.d(TAG, "selected item is ${propertyTypeArray[position]}")
                selectedPropertyType = propertyTypeArray[position].value
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                CcuLog.d(TAG, "onNothingSelected")
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
        CcuLog.d(TAG, "--onDestroyView--")
    }

    private fun sendRequest(bacnetWriteRequest: BacnetWriteRequest) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.write(bacnetWriteRequest)
                val resp = BaseResponse(response)
                if (response.isSuccessful) {
                    val result = resp.data
                    if (result != null) {
                        val readResponse = result.body()
                        CcuLog.d(TAG, "received response->${readResponse}")
                        CoroutineScope(Dispatchers.Main).launch {
                            if (readResponse != null) {

                                if (readResponse.error != null) {
                                    //showToastMessage("error code->${readResponse.error.errorCode}--error class->${readResponse.error.errorClass}")
                                    val errorCode = BacNetConstants.BacnetErrorCodes.from(readResponse.error.errorCode.toInt())
                                    val errorClass = BacNetConstants.BacnetErrorClasses.from(readResponse.error.errorClass.toInt())
                                    showToastMessage("error code->${errorCode}--error class->${errorClass}")
                                } else if(readResponse.errorAbort != null){
                                    showToastMessage("abort reason->${BacNetConstants.BacnetAbortErrors.from(readResponse.errorAbort.abortReason.toInt())}")
                                }else if(readResponse.errorBacApp != null){
                                    showToastMessage("abort reason->${BacNetConstants.BacnetAppErrors.from(readResponse.errorBacApp.abortReason.toInt())}")
                                }else if(readResponse.errorReject != null){
                                    showToastMessage("abort reason->${BacNetConstants.BacnetRejectErrors.from(readResponse.errorReject.abortReason.toInt())}")
                                }else if(readResponse.errorASide != null){
                                    showToastMessage("abort reason->${readResponse.errorASide.abortReason}")
                                }else {
                                    showToastMessage("Success")
                                }

                                /*if(readResponse.error == null){
                                    showToastMessage("Success")
                                }else{
                                    val errorMessage = "error code->${readResponse.error.errorCode}--error class--${readResponse.error.errorClass}"
                                    showToastMessage(errorMessage)
                                }*/
                            }
                        }
                    } else {
                        CcuLog.d(TAG, "--null response--")
                    }
                } else {
                    CcuLog.d(TAG, "--error--${resp.error}")
                }
            } catch (e: SocketTimeoutException) {
                CcuLog.e(TAG, "--SocketTimeoutException--${e.message}")
                showToastMessage("SocketTimeoutException")
            } catch (e: ConnectException) {
                CcuLog.e(TAG, "--ConnectException--${e.message}")
                showToastMessage("ConnectException")
            } catch (e: Exception) {
                CcuLog.e(TAG, "--connection time out--${e.message}")
            }
        }
    }

    private fun showToastMessage(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }
}