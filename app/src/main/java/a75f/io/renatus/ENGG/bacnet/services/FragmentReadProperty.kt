package a75f.io.renatus.ENGG.bacnet.services

import a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_CONFIGURATION
import a75f.io.logic.util.bacnet.BacnetConfigConstants.EMPTY_STRING
import a75f.io.logic.util.bacnet.BacnetConfigConstants.IP_ADDRESS
import a75f.io.logic.util.bacnet.BacnetConfigConstants.IP_DEVICE_INSTANCE_NUMBER
import a75f.io.logic.util.bacnet.BacnetConfigConstants.PORT
import a75f.io.logger.CcuLog
import a75f.io.renatus.ENGG.bacnet.services.client.BaseResponse
import a75f.io.renatus.ENGG.bacnet.services.client.CcuService
import a75f.io.renatus.ENGG.bacnet.services.client.ServiceManager
import a75f.io.renatus.R
import a75f.io.renatus.UtilityApplication
import a75f.io.renatus.util.CCUUiUtil
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
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
    private lateinit var service : CcuService
    private lateinit var tvPropertyType: TextView
    private lateinit var tvPropertyValue: TextView
    private var ipAddress = "192.168.1.1"
    private var port = 47808
    private var deviceId = "1000"

    private lateinit var etMacAddress: EditText
    private lateinit var etDnet: EditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CcuLog.d(TAG, "--onViewCreated--")

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val confString: String? = sharedPreferences.getString(BACNET_CONFIGURATION, null)
        if (confString != null) {
            try {
                val config = JSONObject(confString)
                val networkObject = config.getJSONObject("network")
                ipAddress = networkObject.getString(IP_ADDRESS)
                port = networkObject.getInt(PORT)
                service = ServiceManager.makeCcuService(ipAddress)
                val deviceObject = config.getJSONObject("device")
                deviceId = deviceObject.getString(IP_DEVICE_INSTANCE_NUMBER)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        etMacAddress = view.findViewById(R.id.et_destination_mac)
        etDnet = view.findViewById(R.id.etDnet)

        configureObjectTypeSpinner(view)
        configurePropertySpinner(view)

        tvPropertyType = view.findViewById(R.id.tv_property_type_text)
        tvPropertyValue = view.findViewById(R.id.tv_property_value_text)

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
        etPort.setText(port.toString())
        etObjectId = view.findViewById(R.id.etObjectId)
        etDestinationIp = view.findViewById(R.id.et_destination_ip)
        etDestinationIp.setText(ipAddress)
        enableReadButton()
//        if (validateEntries()) {
//            enableReadButton()
//        } else {
//            disableReadButton()
//        }
    }

    private fun generateRequest() {
        var arrayIndex: Int? = if (etArrayindex.text.toString().isNotEmpty()) {
            etArrayindex.text.toString().toInt()
        } else {
            null
        }

        val request = Gson().toJson(
            BacnetReadRequest(
                DestinationMultiRead(
                    etDestinationIp.text.toString(),
                    etPort.text.toString(),
                    deviceId,
                    etDnet.text.toString(), etMacAddress.text.toString()
                ),
                ReadRequest(
                    ObjectIdentifierBacNet(selectedObjectType, etObjectId.text.toString()),
                    selectedPropertyType, arrayIndex
                )
            )
        )

        val bacNetDestination = DestinationMultiRead(
            etDestinationIp.text.toString(),
            etPort.text.toString(),
            deviceId,
            etDnet.text.toString(), etMacAddress.text.toString()
        )
        val rpRequest = ReadRequest(
            ObjectIdentifierBacNet(selectedObjectType, etObjectId.text.toString()),
            selectedPropertyType, arrayIndex
        )
        sendRequest(bacNetDestination, rpRequest)
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

        if (etMacAddress.text.toString() == EMPTY_STRING || !CCUUiUtil.isValidMacAddress(
                etMacAddress.text.toString().trim { it <= ' ' })
        ) {
            etMacAddress.error = "Please input valid mac address"
            return false
        }
        if (etDnet.text.toString() == EMPTY_STRING || !CCUUiUtil.isValidNumber(
                etDnet.text.toString().toInt(), 0, Int.MAX_VALUE, 1
            )
        ) {
            etDnet.error = "Please input valid d net"
            return false
        }

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

    private fun sendRequest(bacNetDestination: DestinationMultiRead, rpRequest: ReadRequest) {
        CoroutineScope(Dispatchers.IO).launch {
            val readRequest = BacnetReadRequest(
                bacNetDestination,
                rpRequest
            )
            try {
                val response = service.read(readRequest)
                val resp = BaseResponse(response)
                if (response.isSuccessful) {
                    val result = resp.data
                    if (result != null) {
                        val readResponse = result.body()
                        CcuLog.d(TAG, "received response->${readResponse}")
                        CoroutineScope(Dispatchers.Main).launch {
                            updateUi(readResponse)
                        }
                    } else {
                        CcuLog.d(TAG, "--null response--")
                    }
                } else {
                    CcuLog.d(TAG, "--error--${resp.error}")
                }
            } catch (e: SocketTimeoutException) {
                CcuLog.e(TAG, "--SocketTimeoutException--${e.stackTrace}")
                showToastMessage("SocketTimeoutException")
            } catch (e: ConnectException) {
                CcuLog.e(TAG, "--ConnectException--${e.stackTrace}")
                showToastMessage("ConnectException")
            } catch (e: Exception) {
                CcuLog.e(TAG, "--connection time out--${e.stackTrace}")
            }
        }
    }

    private fun showToastMessage(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUi(readResponse: ReadResponse?) {
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
                readResponse.rpResponse.propertyValue.value
                tvPropertyType.text = readResponse.rpResponse.propertyIdentifier.let {
                    BacNetConstants.PropertyType.from(it.toInt())
                }
                    .toString()
                tvPropertyValue.text = readResponse.rpResponse.propertyValue.value
            }

//            if (readResponse.error == null) {
//                readResponse.rpResponse.propertyValue.value
//                tvPropertyType.text = readResponse.rpResponse.propertyIdentifier.let {
//                    BacNetConstants.PropertyType.from(it.toInt())
//                }
//                    .toString()
//                tvPropertyValue.text = readResponse.rpResponse.propertyValue.value
//            } else {
//                showToastMessage("error code->${readResponse.error.errorCode}--error class->${readResponse.error.errorClass}")
//            }
        }
    }
}