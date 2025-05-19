package a75f.io.renatus.ENGG.bacnet.services

import a75f.io.logic.util.bacnet.BacnetConfigConstants
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.system.BacnetReadRequestMultiple
import a75f.io.logic.bo.building.system.DestinationMultiRead
import a75f.io.logic.bo.building.system.ObjectIdentifierBacNet
import a75f.io.logic.bo.building.system.PropertyReference
import a75f.io.logic.bo.building.system.ReadRequestMultiple
import a75f.io.logic.bo.building.system.RpmRequest
import a75f.io.logic.bo.building.system.client.BaseResponse
import a75f.io.logic.bo.building.system.client.CcuService
import a75f.io.logic.bo.building.system.client.ItemsViewModel
import a75f.io.logic.bo.building.system.client.MultiReadResponse
import a75f.io.logic.bo.building.system.client.ServiceManager
import a75f.io.renatus.R
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
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException


class FragmentReadPropertyMultiple : Fragment() {

    private val TAG = "BacnetServicesFragment"
    private lateinit var btnReadProperty: Button
    private lateinit var btnAddObject: Button
    private lateinit var containerObject: ViewGroup
    private lateinit var rvResponseMultiRead : RecyclerView

    private val objectTypeArray = BacNetConstants.ObjectType.values()
    private val propertyTypeArray = BacNetConstants.PropertyType.values()

    private var objectCounter = 0

    private lateinit var etDestinationIp: EditText
    private lateinit var etPort: EditText
    private lateinit var service : CcuService
    private var ipAddress = "192.168.1.1"
    private var port = 47808
    private var deviceId = "1000"

    private val data = ArrayList<ItemsViewModel>()
    private lateinit var adapter : CustomAdapterMultipleRead
    private lateinit var etMacAddress: EditText
    private lateinit var etDnet: EditText

    override fun onCreateView(
        inflator: LayoutInflater,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): View? {
        return inflator.inflate(R.layout.lyt_frag_read_property_multiple, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        etMacAddress = view.findViewById(R.id.et_destination_mac)
        etDnet = view.findViewById(R.id.etDnet)

        containerObject = view.findViewById(R.id.container_property)

        btnAddObject = view.findViewById(R.id.btnAddObject)
        btnAddObject.setOnClickListener {
            containerObject.addView(addNewObject())
        }

        etDestinationIp = view.findViewById(R.id.et_destination_ip)
        etDestinationIp.setText(ipAddress)
        etPort = view.findViewById(R.id.etPort)
        etPort.setText(port.toString())

        btnReadProperty = view.findViewById(R.id.btnReadProperty)
        btnReadProperty.setOnClickListener {
            CcuLog.d(TAG, "total objects -> ${containerObject.childCount}")
           /* if(!validateData()){
                return@setOnClickListener
            }
            if(!validateNetworkData()){
                return@setOnClickListener
            }*/


            val destination =
                DestinationMultiRead(etDestinationIp.text.toString(), etPort.text.toString(), deviceId, etDnet.text.toString(), etMacAddress.text.toString())
            val readAccessSpecification = mutableListOf<ReadRequestMultiple>()


            val totalObjectCount = containerObject.childCount - 1
            for (i in 0..totalObjectCount) {
                val chileView = containerObject.getChildAt(i)
                val objectIdentifier = getDetailsFromObjectLayout(chileView, i)
                val propertyReference = getDetailsOfProperties(chileView, i)
                readAccessSpecification.add(
                    ReadRequestMultiple(
                        objectIdentifier,
                        propertyReference
                    )
                )
            }

            val rpmRequest = RpmRequest(readAccessSpecification)
            val readRequestMultiple =
                Gson().toJson(BacnetReadRequestMultiple(destination, rpmRequest)).toString()

            sendRequest(BacnetReadRequestMultiple(destination, rpmRequest))

            CcuLog.d(TAG, "readRequestMultiple-->$readRequestMultiple")
        }

        rvResponseMultiRead = view.findViewById(R.id.bac_resp_rv)
        rvResponseMultiRead.layoutManager = LinearLayoutManager(requireContext())

        adapter = CustomAdapterMultipleRead(data)
        rvResponseMultiRead.adapter = adapter
    }

    private fun validateData(): Boolean {
        var isDataFilled = true
        val totalObjectCount = containerObject.childCount -1
        for (i in 0..totalObjectCount) {
            val childView = containerObject.getChildAt(i)
            val etObjectId = childView.findViewById<EditText>(R.id.etObjectId)
            if (etObjectId.text.toString().isEmpty()) {
                etObjectId.error = getString(R.string.txt_error_object_id)
                etObjectId.findFocus()
                isDataFilled = false
                break
            }
        }
        return isDataFilled
    }

    private fun validateNetworkData() : Boolean{
        if (etMacAddress.text.toString() == BacnetConfigConstants.EMPTY_STRING || !CCUUiUtil.isValidMacAddress(
                etMacAddress.text.toString().trim { it <= ' ' })
        ) {
            etMacAddress.error = "Please input valid mac address"
            return false
        }
        if (etDnet.text.toString() == BacnetConfigConstants.EMPTY_STRING || !CCUUiUtil.isValidNumber(
                etDnet.text.toString().toInt(), 0, Int.MAX_VALUE, 1
            )
        ) {
            etDnet.error = "Please input valid d net"
            return false
        }
        if (etDestinationIp.text.toString() == BacnetConfigConstants.EMPTY_STRING || !CCUUiUtil.isValidIPAddress(
                etDestinationIp.text.toString().trim { it <= ' ' })
        ) {
            etDestinationIp.error = "Please input valid ip address"
            return false
        }

        if (etPort.text.toString() == BacnetConfigConstants.EMPTY_STRING || !CCUUiUtil.isValidNumber(
                etPort.text.toString().toInt(), 4069, 65535, 1
            )
        ) {
            etPort.error = "Please input valid port"
            return false
        }

        return true
    }

    private fun getDetailsFromObjectLayout(childView: View, i: Int): ObjectIdentifierBacNet {
        CcuLog.d(TAG, "--------------------checking object-->$i<------------------------------- ->")
        val spin = childView.findViewById(R.id.sp_object_types) as Spinner
        val objectId = childView.findViewById<EditText>(R.id.etObjectId)
        CcuLog.d(
            TAG,
            "selected object type->${spin.selectedItem} <----object id>--->${objectId.text.toString()}"
        )
        return ObjectIdentifierBacNet(
            BacNetConstants.ObjectType.valueOf(spin.selectedItem.toString()).value,
            objectId.text.toString()
        )
    }

    private fun getDetailsOfProperties(chileView: View, i: Int): MutableList<PropertyReference> {
        val list = mutableListOf<PropertyReference>()
        CcuLog.d(TAG, "----checking properties-->$i<------- ->")
        val containerHoldingProperties =
            chileView.findViewById<LinearLayout>(R.id.containerToAddProperties)
        val totalObjectCount = containerHoldingProperties.childCount - 1
        CcuLog.d(TAG, "----no of properties-->$totalObjectCount<------- ->")
        for (i in 0..totalObjectCount) {
            val childView = containerHoldingProperties.getChildAt(i)
            val propertySpinner = childView.findViewById<Spinner>(R.id.sp_property_types)
            val etArrayIndex = childView.findViewById<EditText>(R.id.etArrayIndex)
            CcuLog.d(TAG, "selected property is->${(propertySpinner).selectedItem}")

            var arrayIndex: Int? = if (etArrayIndex.text.toString().isNotEmpty()) {
                etArrayIndex.text.toString().toInt()
            } else {
                null
            }

            list.add(
                PropertyReference(
                    BacNetConstants.PropertyType.valueOf(propertySpinner.selectedItem.toString()).value,
                    arrayIndex
                )
            )
        }
        return list
    }

    private fun addNewObject(): View? {
        var propertyCounter = 0
        val view = layoutInflater.inflate(R.layout.lyt_add_obj, null)

        val tvRemove = view.findViewById<TextView>(R.id.label_object_remove)
        objectCounter++
        tvRemove.text = "Object $objectCounter"

        configureObjectTypeSpinner(view)
        view.findViewById<View>(R.id.label_object_remove).setOnClickListener {
            containerObject.removeView(view)
        }
        view.findViewById<Button>(R.id.btnAddProperty).setOnClickListener {
            val parent = view.findViewById<LinearLayout>(R.id.containerToAddProperties)
            propertyCounter++
            val propertyView = addNewProperty(propertyCounter)
            parent.addView(propertyView)
            propertyView?.findViewById<TextView>(R.id.label_property_remove)?.setOnClickListener {
                parent.removeView(propertyView)
            }
        }
        return view
    }

    private fun addNewProperty(propertyCounter: Int): View? {
        val view = layoutInflater.inflate(R.layout.lyt_add_property, null)
        val tvRemoveProperty = view.findViewById<TextView>(R.id.label_property_remove)
        tvRemoveProperty.text = "Property $propertyCounter"

        configurePropertySpinner(view)
        return view
    }

    private fun configureObjectTypeSpinner(view: View) {

        val spin = view.findViewById(R.id.sp_object_types) as Spinner
        spin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                CcuLog.d(TAG, "selected item is ${objectTypeArray[position]}")
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


    private fun sendRequest(rpmRequest: BacnetReadRequestMultiple) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                CcuLog.d(TAG, "--------------service.multiread--------------------")
                val response = service.multiread(rpmRequest)
                CcuLog.d(TAG, "--------------service.multiread response--------------------")
                val resp = BaseResponse(response)
                if (response.isSuccessful) {
                    val result = resp.data
                    if (result != null) {
                        val readResponse = result.body()
                        CcuLog.d(TAG, "received response->${readResponse}")
                        CoroutineScope(Dispatchers.Main).launch {
                            //Log.d(TAG, "--null response--${readResponse!!.rpResponse.listOfItems.size}")
                            updateUi(readResponse)
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

    private fun updateUi(readResponse: MultiReadResponse?) {
        data.clear()
        if (readResponse != null) {
            if (readResponse.error != null) {
                //showToastMessage("error code->${readResponse.error.errorCode}--error class->${readResponse.error.errorClass}")
                val errorCode = BacNetConstants.BacnetErrorCodes.from(readResponse.error!!.errorCode.toInt())
                val errorClass = BacNetConstants.BacnetErrorClasses.from(readResponse.error!!.errorClass.toInt())
                showToastMessage("error code->${errorCode}--error class->${errorClass}")
            } else if(readResponse.errorAbort != null){
                showToastMessage("abort reason->${BacNetConstants.BacnetAbortErrors.from(
                    readResponse.errorAbort!!.abortReason.toInt())}")
            }else if(readResponse.errorBacApp != null){
                showToastMessage("abort reason->${BacNetConstants.BacnetAppErrors.from(readResponse.errorBacApp!!.abortReason.toInt())}")
            }else if(readResponse.errorReject != null){
                showToastMessage("abort reason->${BacNetConstants.BacnetRejectErrors.from(
                    readResponse.errorReject!!.abortReason.toInt())}")
            }else if(readResponse.errorASide != null){
                showToastMessage("abort reason->${readResponse.errorASide!!.abortReason}")
            }else {
                for (item in readResponse.rpResponse.listOfItems) {
                    data.add(
                        ItemsViewModel(
                            BacNetConstants.PropertyType.from(item.results[0].propertyIdentifier.toInt()).toString(),
                            item.results[0].propertyValue.value,
                            BacNetConstants.ObjectType.from(item.objectIdentifier.objectType.toInt()).toString()
                        )
                    )
                }
                adapter.notifyDataSetChanged()
            }
        }
    }
}