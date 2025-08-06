package a75f.io.renatus.bacnet

import a75f.io.renatus.R
import a75f.io.renatus.bacnet.models.BacnetDevice
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.compose.ComposeUtil.Companion.greyColor
import a75f.io.renatus.compose.ComposeUtil.Companion.primaryColor
import a75f.io.renatus.modbus.util.DEVICE_ID
import a75f.io.renatus.modbus.util.DEVICE_IP
import a75f.io.renatus.modbus.util.DEVICE_MAC_ADDRESS
import a75f.io.renatus.modbus.util.DEVICE_NAME
import a75f.io.renatus.modbus.util.DEVICE_NETWORK
import a75f.io.renatus.modbus.util.LOADING
import a75f.io.renatus.modbus.util.OnItemSelectBacnetDevice
import a75f.io.renatus.util.ProgressDialogUtils
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment

class BacnetDeviceSelectionFragment : DialogFragment() {

    var itemsList = mutableStateOf(emptyList<BacnetDevice>())
    lateinit var onItemSelect: OnItemSelectBacnetDevice
    lateinit var placeholder: String
    private var isMstpView: Boolean = false

    companion object {
        val ID: String = BacnetDeviceSelectionFragment::class.java.simpleName

        fun newInstance(
            items: MutableState<List<BacnetDevice>>,
            onItemSelect: OnItemSelectBacnetDevice,
            placeholder: String,
            isMstpView: Boolean = false
        ): BacnetDeviceSelectionFragment {
            val fragment = BacnetDeviceSelectionFragment()
            fragment.itemsList = items
            fragment.onItemSelect = onItemSelect
            fragment.placeholder = placeholder
            fragment.isMstpView = isMstpView
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        ProgressDialogUtils.showProgressDialog(context, LOADING)
        val rootView = ComposeView(requireContext())
        rootView.apply {
            setContent {
                ShowOptions(itemsList.value)
            }
            ProgressDialogUtils.hideProgressDialog()
            return rootView
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    fun ShowOptions(
        items: List<BacnetDevice>
    ) {
        var searchText by remember { mutableStateOf("") }
        val filteredItems = if (searchText.isEmpty()) {
            items
        } else {
            items.filter {
                it.deviceId.contains(searchText, ignoreCase = true)
            }
        }
        FlowRow(modifier = Modifier.padding(20.dp)) {
            Row {
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text(placeholder) },
                    singleLine = true,
                    colors = TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = primaryColor,
                        unfocusedIndicatorColor = greyColor,
                        containerColor = Color.White

                    ),
                    modifier = Modifier
                        .width(900.dp)
                        .background(Color.Red),

                    textStyle = TextStyle(
                        fontFamily = ComposeUtil.myFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.Black
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clickable(onClick = { dialog!!.dismiss() }),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_baseline_close_24),
                        contentDescription = "Custom Icon",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = 20.dp)
            ) {

                LazyColumn (
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    if (isMstpView) {
                        item {
                            MstpDeviceListItemHeader()
                        }
                        items(filteredItems.size) { index ->
                            MstpDeviceListItem(
                                device = filteredItems[index],
                                isEven = index % 2 == 0, index
                            )
                        }
                    } else {
                        item {
                            DeviceListItemHeader()
                        }
                        items(filteredItems.size) { index ->
                            DeviceListItem(
                                device = filteredItems[index],
                                isEven = index % 2 == 0, index
                            )
                        }
                    }
                }
            }
        }

    }

    fun getIdString(): String {
        return ID
    }

    override fun onStart() {
        super.onStart()
        if (dialog != null)
            dialog!!.window!!.setLayout(1200, 700)
    }


    @Composable
    fun DeviceListItem(device: BacnetDevice, isEven: Boolean, index: Int) {
        val backgroundColor = if (isEven) Color(0xFFF9F9F9) else Color.White
        Row(
            modifier = Modifier
                //.padding(16.dp)
                .fillMaxWidth()
                .background(backgroundColor)
                .clickable(onClick = {
                    onItemSelect.onItemSelected(index, device)
                    dismiss()
                }),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${device.deviceId}",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 22.sp,
                    color = Color.Black
                )
            )
            Text(
                text = "${device.deviceIp}/${device.devicePort}",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 22.sp,
                    color = Color.Black
                )
            )
            Text(
                text = "${device.deviceNetwork}",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 22.sp,
                    color = Color.Black
                )
            )
            Text(
                text = "${device.deviceName}",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 22.sp,
                    color = Color.Black
                ),
            )
        }
    }

    @Composable
    fun DeviceListItemHeader() {
        Row(
            modifier = Modifier
                //.padding(16.dp)
                .fillMaxWidth()
                .background(Color(0xFFEBECED)),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = DEVICE_ID,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.Black
                )
            )
            Text(
                text = DEVICE_IP,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.Black
                )
            )
            Text(
                text = DEVICE_NETWORK,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.Black
                )
            )
            Text(
                text = DEVICE_NAME,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.Black
                ),
            )
        }
    }

    @Composable
    fun MstpDeviceListItem(device: BacnetDevice, isEven: Boolean, index: Int) {
        val backgroundColor = if (isEven) Color(0xFFF9F9F9) else Color.White
        Row(
            modifier = Modifier
                //.padding(16.dp)
                .fillMaxWidth()
                .background(backgroundColor)
                .clickable(onClick = {
                    onItemSelect.onItemSelected(index, device)
                    dismiss()
                }),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${device.deviceId}",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 22.sp,
                    color = Color.Black
                )
            )
            Text(
                text = "${device.deviceMacAddress}",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 22.sp,
                    color = Color.Black
                )
            )

            Text(
                text = "${device.deviceName}",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 22.sp,
                    color = Color.Black
                ),
            )
        }
    }

    @Composable
    fun MstpDeviceListItemHeader() {
        Row(
            modifier = Modifier
                //.padding(16.dp)
                .fillMaxWidth()
                .background(Color(0xFFEBECED)),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = DEVICE_ID,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.Black
                )
            )
            Text(
                text = DEVICE_MAC_ADDRESS,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.Black
                )
            )

            Text(
                text = DEVICE_NAME,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                style = TextStyle(
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.Black
                ),
            )
        }
    }
}

