package a75f.io.renatus.ENGG.bacnet.services

import a75f.io.logic.util.bacnet.BacnetConfigConstants
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException


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
                service = ServiceManager.makeCcuService(ipAddress)
                val deviceObject = config.getJSONObject("device")
                deviceId = deviceObject.getString(BacnetConfigConstants.IP_DEVICE_INSTANCE_NUMBER)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

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
//            if(!validateRequestEntries()){
//                return@setOnClickListener
//            }
            generateRequest()
        }

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
                    etPort.error = getString(R.string.txt_error_port)
                    disableReadButton()
                    //etPort.error = null
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
                    //etDestinationIp.error = null
                    etDestinationIp.error = getString(R.string.error_ip_address)
                    disableReadButton()
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
        if (selectedObjectType == OBJ_ANALOG_INPUT) {
            try {
                val bacnetWhoIsRequest = BacnetWhoIsRequest(
                    WhoIsRequest(
                        etLowLimit.text.toString(),
                        etHighLimit.text.toString()
                    ),
                    BroadCast("unicast"),
                    etPort.text.toString(),
                    etDestinationIp.text.toString()
                    //DestinationWhoIs(etPort.text.toString(), ipAddress),
                )
                val request = Gson().toJson(
                    bacnetWhoIsRequest
                )
                CcuLog.d(TAG, "this is the unicast request-->$request")
                sendRequest(bacnetWhoIsRequest)
            }catch (e : NumberFormatException){
                CcuLog.d(TAG, "please provide valid input - ${e.message}")
            }
        } else {
            try {
                val broadCastValue = if (selectedObjectType == OBJ_ANALOG_OUTPUT) {
                    "local"
                } else {
                    "global"
                }
                val bacnetWhoIsRequest = BacnetWhoIsRequest(
                    WhoIsRequest(
                        etLowLimit.text.toString(),
                        etHighLimit.text.toString()
                    ),
                    BroadCast(broadCastValue),
                    etPort.text.toString(),
                    etDestinationIp.text.toString()
                )
                val request = Gson().toJson(
                    bacnetWhoIsRequest
                )
                CcuLog.d(TAG, "this is the broadcast request-->$request")
                sendRequest(bacnetWhoIsRequest)
            }catch (e : NumberFormatException){
                CcuLog.e(TAG, "please provide valid input - ${e.message}")
            }
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
            R.attr.orange_75f
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

    private fun validateRequestEntries(): Boolean {
        if (etLowLimit.text.toString() == BacnetConfigConstants.EMPTY_STRING || !CCUUiUtil.isValidNumber(
                etLowLimit.text.toString().toInt(), 0, Int.MAX_VALUE, 1
            )
        ) {
            etLowLimit.error = getString(R.string.txt_valid_input)
            return false
        }
        if (etHighLimit.text.toString() == BacnetConfigConstants.EMPTY_STRING || !CCUUiUtil.isValidNumber(
                etHighLimit.text.toString().toInt(), 0, Int.MAX_VALUE, 1
            )
        ) {
            etHighLimit.error = getString(R.string.txt_valid_input)
            return false
        }

        etLowLimit.error = null
        etHighLimit.error = null
        return true
    }

    private fun configureObjectTypeSpinner(view: View) {

        val spin = view.findViewById(R.id.sp_object_types) as Spinner
        spin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                CcuLog.d(TAG, "selected item is ${objectTypeArray[position]}")
                selectedObjectType = objectTypeArray[position]
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

    override fun onDestroyView() {
        super.onDestroyView()
        CcuLog.d(TAG, "--onDestroyView--")
    }

    private fun sendRequest(bacnetWhoIsRequest: BacnetWhoIsRequest) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.whois(bacnetWhoIsRequest)
                val resp = BaseResponse(response)
                if (response.isSuccessful) {
                    val result = resp.data
                    if (result != null) {
                        val readResponse = result.body()
                        CcuLog.d(TAG, "received response->${readResponse}")
                        CoroutineScope(Dispatchers.Main).launch {
                            //updateUi(readResponse)
                            val responseText = Gson().toJson(readResponse)
                            showToastMessage("success-->$responseText")
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